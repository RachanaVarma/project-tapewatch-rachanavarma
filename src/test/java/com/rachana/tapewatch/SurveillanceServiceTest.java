package com.rachana.tapewatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rachana.tapewatch.config.*;
import com.rachana.tapewatch.domain.*;
import com.rachana.tapewatch.store.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import com.rachana.tapewatch.api.*;

class SurveillanceServiceTest {
    private SurveillanceService service(AlertRepository alerts, EventRepository events, int qtyPct) {
        RuleProperties props=new RuleProperties(new RuleProperties.Wash(300,new BigDecimal(qtyPct),new BigDecimal("0.5"),"HIGH","MEDIUM"));
        return new SurveillanceService(alerts,events,new MemoryRollingWindowStore(),props,new Masker("test-secret"),new ObjectMapper().findAndRegisterModules());
    }
    private MarketEvent trade(String id,String buy,String sell,int qty,int second){return new MarketEvent(id,"TRADE",null,null,"XYZ",null,new BigDecimal(qty),new BigDecimal("10.00"),id,buy,sell,"B-"+id,"S-"+id,Instant.parse("2026-11-14T00:00:00Z").plusSeconds(second));}
    private MarketEvent order(String id,String type,String account,String side,int qty,int second){return new MarketEvent(id,type,"ORDER-1",account,"XYZ",side,new BigDecimal(qty),new BigDecimal("10"),null,null,null,null,null,Instant.parse("2026-11-14T00:00:00Z").plusSeconds(second));}

    @Test void detectsSelfTradeAndDoesNotExposeRawOwner(){
        AlertRepository alerts=mock(AlertRepository.class);EventRepository events=mock(EventRepository.class);
        var s=service(alerts,events,5);s.ingest(List.of(trade("self","ACC-001","ACC-002",100,0)));
        var captor=org.mockito.ArgumentCaptor.forClass(AlertEntity.class);verify(alerts).save(captor.capture());
        assertEquals("WT-1",captor.getValue().getRuleId());assertTrue(captor.getValue().getOwnerToken().startsWith("OWN-"));assertNotEquals("OWN-001",captor.getValue().getOwnerToken());
    }
    @Test void detectsRoundTripInsideWindow(){
        AlertRepository alerts=mock(AlertRepository.class);EventRepository events=mock(EventRepository.class);var s=service(alerts,events,5);
        s.ingest(List.of(trade("buy","ACC-003","ACC-004",100,0),trade("sell","ACC-005","ACC-003",104,60)));
        verify(alerts).save(argThat(a->a.getRuleId().equals("WT-2")));
    }
    @Test void ignoresRoundTripOutsideWindowAndOutsideQuantityTolerance(){
        AlertRepository a1=mock(AlertRepository.class);var s1=service(a1,mock(EventRepository.class),5);
        s1.ingest(List.of(trade("b1","ACC-003","ACC-004",100,0),trade("s1","ACC-005","ACC-003",100,301)));verify(a1,never()).save(any());
        AlertRepository a2=mock(AlertRepository.class);var s2=service(a2,mock(EventRepository.class),5);
        s2.ingest(List.of(trade("b2","ACC-003","ACC-004",100,0),trade("s2","ACC-005","ACC-003",120,30)));verify(a2,never()).save(any());
    }
    @Test void changedConfiguredToleranceChangesOutcome(){
        AlertRepository strict=mock(AlertRepository.class);var s1=service(strict,mock(EventRepository.class),5);
        s1.ingest(List.of(trade("b3","ACC-003","ACC-004",100,0),trade("s3","ACC-005","ACC-003",110,30)));verify(strict,never()).save(any());
        AlertRepository relaxed=mock(AlertRepository.class);var s2=service(relaxed,mock(EventRepository.class),15);
        s2.ingest(List.of(trade("b4","ACC-003","ACC-004",100,0),trade("s4","ACC-005","ACC-003",110,30)));verify(relaxed).save(argThat(a->a.getRuleId().equals("WT-2")));
    }
    @Test void duplicateEventIdIsNotCountedTwice(){
        AlertRepository a=mock(AlertRepository.class);EventRepository e=mock(EventRepository.class);when(e.existsById("dupe")).thenReturn(true);
        var counts=service(a,e,5).ingest(List.of(trade("dupe","ACC-003","ACC-004",10,0)));
        assertEquals(0,counts.accepted());assertEquals(1,counts.duplicates());verify(e,never()).save(any());
    }
    @Test void alertDetailMasksRawAccountAndOwnerIds(){
        AlertRepository alerts=mock(AlertRepository.class);EventRepository events=mock(EventRepository.class);
        MarketEvent evidence=trade("evid","ACC-001","ACC-003",100,0);
        try {when(events.findById("evid")).thenReturn(java.util.Optional.of(new EventEntity("evid",new ObjectMapper().findAndRegisterModules().writeValueAsString(evidence),evidence.ts())));}catch(Exception ex){throw new AssertionError(ex);}
        var s=service(alerts,events,5);AlertEntity alert=new AlertEntity("WT-1",new Masker("test-secret").token("OWN-001"),"XYZ","HIGH",evidence.ts(),"evid");
        try {String body=new ObjectMapper().findAndRegisterModules().writeValueAsString(s.alertView(alert));assertFalse(body.contains("ACC-001"));assertFalse(body.contains("ACC-003"));assertFalse(body.contains("OWN-001"));}catch(Exception ex){throw new AssertionError(ex);}
    }
    @Test void alertListAndDetailApiNeverReturnRawAccountOrOwnerIds() throws Exception {
        AlertRepository alerts=mock(AlertRepository.class);EventRepository events=mock(EventRepository.class);
        MarketEvent evidence=trade("api-evid","ACC-001","ACC-003",100,0);ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
        when(events.findById("api-evid")).thenReturn(java.util.Optional.of(new EventEntity("api-evid",mapper.writeValueAsString(evidence),evidence.ts())));
        AlertEntity alert=new AlertEntity("WT-1",new Masker("test-secret").token("OWN-001"),"XYZ","HIGH",evidence.ts(),"api-evid");
        when(alerts.findById(7L)).thenReturn(java.util.Optional.of(alert));
        when(alerts.findAll(org.mockito.ArgumentMatchers.<Specification<AlertEntity>>any(),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(alert)));
        var controller=new AlertController(service(alerts,events,5),new DispositionService(alerts,mock(AlertAuditRepository.class)));
        MockMvc mvc=standaloneSetup(controller).setControllerAdvice(new ApiErrors()).build();
        for(String url:List.of("/api/v1/alerts?page=0&size=50","/api/v1/alerts/7")){
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(not(containsString("ACC-001"))))
                    .andExpect(content().string(not(containsString("ACC-003")))).andExpect(content().string(not(containsString("OWN-001"))));
        }
    }
    @Test void detectsSpoofingSequenceInMemory(){
        AlertRepository a=mock(AlertRepository.class);var s=service(a,mock(EventRepository.class),5);
        s.ingest(List.of(order("new","ORDER_NEW","ACC-003","BUY",10000,0),order("cancel","ORDER_CANCEL","ACC-003","BUY",10000,2),trade("exec","ACC-004","ACC-003",100,3)));
        verify(a).save(argThat(x->x.getRuleId().equals("SP-1")));
    }
    @Test void doesNotFlagSpoofingBelowCancelRatioWithoutOppositeTradeOrAfterLifetime(){
        AlertRepository low=mock(AlertRepository.class);service(low,mock(EventRepository.class),5).ingest(List.of(order("n1","ORDER_NEW","ACC-003","BUY",10000,0),order("c1","ORDER_CANCEL","ACC-003","BUY",7000,2),trade("x1","ACC-004","ACC-003",100,3)));verify(low,never()).save(argThat(x->x.getRuleId().equals("SP-1")));
        AlertRepository noTrade=mock(AlertRepository.class);service(noTrade,mock(EventRepository.class),5).ingest(List.of(order("n2","ORDER_NEW","ACC-003","BUY",10000,0),order("c2","ORDER_CANCEL","ACC-003","BUY",10000,2)));verify(noTrade,never()).save(argThat(x->x.getRuleId().equals("SP-1")));
        AlertRepository late=mock(AlertRepository.class);service(late,mock(EventRepository.class),5).ingest(List.of(order("n3","ORDER_NEW","ACC-003","BUY",10000,0),order("c3","ORDER_CANCEL","ACC-003","BUY",10000,6),trade("x3","ACC-004","ACC-003",100,7)));verify(late,never()).save(argThat(x->x.getRuleId().equals("SP-1")));
    }
}
