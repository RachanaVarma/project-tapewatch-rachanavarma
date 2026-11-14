package com.rachana.tapewatch.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rachana.tapewatch.config.Masker;
import com.rachana.tapewatch.config.RuleProperties;
import com.rachana.tapewatch.store.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class SurveillanceService {
    private final AlertRepository alerts; private final EventRepository stored; private final RollingWindowStore windows;
    private final RuleProperties rules; private final Masker masker; private final ObjectMapper json;
    private final Map<String,String> owners;
    public SurveillanceService(AlertRepository alerts, EventRepository stored, RollingWindowStore windows, RuleProperties rules, Masker masker, ObjectMapper json) {
        this.alerts=alerts;this.stored=stored;this.windows=windows;this.rules=rules;this.masker=masker;this.json=json;
        this.owners=loadOwners();
    }
    private Map<String,String> loadOwners(){
        Map<String,String> map=new HashMap<>();
        try(var in=getClass().getResourceAsStream("/accounts.csv")){
            if(in==null)return map;
            try(var reader=new java.io.BufferedReader(new java.io.InputStreamReader(in))){String line;reader.readLine();while((line=reader.readLine())!=null){String[] p=line.split(",");if(p.length==2)map.put(p[0].trim(),p[1].trim());}}
        }catch(Exception e){throw new IllegalStateException("Cannot load seeded accounts",e);} return map;
    }
    public record Counts(int accepted,int duplicates,int rejected,Map<String,List<String>> fieldErrors) {}
    @Transactional
    public Counts ingest(List<MarketEvent> events){
        int accepted=0,duplicates=0,rejected=0,index=0;Map<String,List<String>> errors=new LinkedHashMap<>();
        for(MarketEvent e:events){
            if(e!=null&&e.eventId()!=null&&!e.eventId().isBlank()&&stored.existsById(e.eventId())){duplicates++;index++;continue;}
            try{validate(e);}catch(IllegalArgumentException ex){rejected++;String key=e==null||e.eventId()==null||e.eventId().isBlank()?"index-"+index:e.eventId();errors.put(key,Arrays.stream(ex.getMessage().replace("Invalid fields: ","").split(", ")).toList());index++;continue;}
            try{stored.save(new EventEntity(e.eventId(),json.writeValueAsString(e),e.ts()));}catch(Exception ex){throw new IllegalStateException(ex);}
            accepted++; evaluate(e);
            index++;
        } return new Counts(accepted,duplicates,rejected,errors);
    }
    public void validate(MarketEvent e){
        if(e==null)throw new IllegalArgumentException("Invalid fields: event");
        List<String> bad=new ArrayList<>();
        if(e.eventId()==null||e.eventId().isBlank())bad.add("eventId");
        if(e.type()==null||!Set.of("ORDER_NEW","ORDER_CANCEL","TRADE").contains(e.type()))bad.add("type");
        if(e.instrument()==null||e.instrument().isBlank())bad.add("instrument");
        if(e.ts()==null)bad.add("ts");
        if(e.qty()==null||e.qty().signum()<=0)bad.add("qty");
        if(e.price()==null||e.price().signum()<=0)bad.add("price");
        if("TRADE".equals(e.type())){
            if(e.tradeId()==null||e.tradeId().isBlank())bad.add("tradeId");
            if(e.buyAccountId()==null||e.sellAccountId()==null)bad.add("buyAccountId/sellAccountId");
            if(e.buyOrderId()==null||e.sellOrderId()==null)bad.add("buyOrderId/sellOrderId");
            if(e.buyAccountId()!=null&&!owners.containsKey(e.buyAccountId()))bad.add("buyAccountId");
            if(e.sellAccountId()!=null&&!owners.containsKey(e.sellAccountId()))bad.add("sellAccountId");
        }
        else {
            if(e.accountId()==null||e.orderId()==null||!("BUY".equals(e.side())||"SELL".equals(e.side())))bad.add("accountId/orderId/side");
            if(e.accountId()!=null&&!owners.containsKey(e.accountId()))bad.add("accountId");
        }
        if(!bad.isEmpty())throw new IllegalArgumentException("Invalid fields: "+String.join(", ",bad));
    }
    private void evaluate(MarketEvent e){
        if("ORDER_NEW".equals(e.type())||"ORDER_CANCEL".equals(e.type())){
            String owner=owner(e.accountId());windows.add(owner,e.instrument(),e);detectLayering(owner,e.instrument(),e.ts());return;
        }
        if(!"TRADE".equals(e.type()))return;
        String buy=owner(e.buyAccountId()),sell=owner(e.sellAccountId());
        if(buy.equals(sell)) saveAlert("WT-1",buy,e.instrument(),rules.wash().wt1Severity(),e.ts(),List.of(e.eventId()));
        detectRoundTrip(e,buy,"BUY"); detectRoundTrip(e,sell,"SELL");
        windows.add(buy,e.instrument(),e); if(!sell.equals(buy))windows.add(sell,e.instrument(),e);
        detectLayering(buy,e.instrument(),e.ts());if(!sell.equals(buy))detectLayering(sell,e.instrument(),e.ts());
    }
    private void detectLayering(String owner,String instrument,Instant at){
        var cfg=rules.sp1();Instant since=at.minusSeconds(cfg.windowSeconds());List<MarketEvent> history=windows.events(owner,instrument,since).stream().filter(e->!e.ts().isAfter(at)).toList();
        for(String placedSide:List.of("BUY","SELL")){
            Map<String,MarketEvent> placements=new HashMap<>();BigDecimal placed=BigDecimal.ZERO;
            for(MarketEvent e:history)if("ORDER_NEW".equals(e.type())&&placedSide.equals(e.side())){placements.put(e.orderId(),e);placed=placed.add(e.qty());}
            if(placed.compareTo(cfg.minPlacedQty())<0)continue;
            BigDecimal cancelled=BigDecimal.ZERO;List<MarketEvent> evidence=new ArrayList<>(placements.values());
            for(MarketEvent cancel:history)if("ORDER_CANCEL".equals(cancel.type())){
                MarketEvent order=placements.get(cancel.orderId());
                if(order!=null&&!cancel.ts().isBefore(order.ts())&&!cancel.ts().isAfter(order.ts().plusSeconds(cfg.maxOrderLifetimeSeconds()))){cancelled=cancelled.add(cancel.qty());evidence.add(cancel);}
            }
            if(cancelled.divide(placed,8,RoundingMode.HALF_UP).compareTo(cfg.minCancelRatio())<0)continue;
            List<MarketEvent> executions=history.stream().filter(x->"TRADE".equals(x.type())&&(
                    (owner(x.buyAccountId()).equals(owner)&&!placedSide.equals("BUY"))||
                    (owner(x.sellAccountId()).equals(owner)&&!placedSide.equals("SELL")))).toList();
            if(executions.isEmpty())continue;
            evidence.addAll(executions);
            if(windows.claimAlert("SP-1",owner,instrument,at,cfg.windowSeconds()))
                saveAlert("SP-1",owner,instrument,cfg.severity(),at,evidence.stream().map(MarketEvent::eventId).distinct().toList());
        }
    }
    private void detectRoundTrip(MarketEvent current,String owner,String currentSide){
        Instant since=current.ts().minusSeconds(rules.wash().windowSeconds());
        for(MarketEvent prev:windows.events(owner,current.instrument(),since)){
            if(!"TRADE".equals(prev.type())||prev.eventId().equals(current.eventId())||prev.ts().isAfter(current.ts()))continue;
            boolean prevBuy=owner(prev.buyAccountId()).equals(owner),prevSell=owner(prev.sellAccountId()).equals(owner);
            String prevSide=prevBuy?"BUY":prevSell?"SELL":null;
            if(prevSide==null||prevSide.equals(currentSide))continue;
            if(!withinPct(prev.qty(),current.qty(),rules.wash().qtyTolerancePct()))continue;
            if(!withinPct(prev.price(),current.price(),rules.wash().priceTolerancePct()))continue;
            saveAlert("WT-2",owner,current.instrument(),rules.wash().wt2Severity(),current.ts(),List.of(prev.eventId(),current.eventId())); break;
        }
    }
    private String owner(String account){return owners.getOrDefault(account,account);}
    private boolean withinPct(BigDecimal a,BigDecimal b,BigDecimal tolerance){BigDecimal base=a.max(b);return a.subtract(b).abs().multiply(BigDecimal.valueOf(100)).compareTo(base.multiply(tolerance))<=0;}
    private void saveAlert(String rule,String owner,String instrument,String severity,Instant time,List<String> ids){alerts.save(new AlertEntity(rule,masker.token(owner),instrument,severity,time,String.join(",",ids)));}
    public Map<String,Object> alertView(AlertEntity a){
        Map<String,Object> out=new LinkedHashMap<>();out.put("id",a.getId());out.put("ruleId",a.getRuleId());out.put("ownerToken",a.getOwnerToken());out.put("instrument",a.getInstrument());out.put("severity",a.getSeverity());out.put("status",a.getStatus());out.put("eventTime",a.getEventTime());out.put("createdAt",a.getCreatedAt());
        List<Map<String,Object>> evidence=new ArrayList<>();for(String id:a.getEvidenceEventIds().split(",")){stored.findById(id).ifPresent(x->{try{MarketEvent e=json.readValue(x.getPayload(),MarketEvent.class);evidence.add(safeEvent(e));}catch(Exception ex){throw new IllegalStateException(ex);}});}evidence.sort(Comparator.comparing(v->(Instant)v.get("ts")));out.put("evidence",evidence);return out;
    }
    private Map<String,Object> safeEvent(MarketEvent e){Map<String,Object> m=new LinkedHashMap<>();m.put("eventId",e.eventId());m.put("type",e.type());m.put("instrument",e.instrument());m.put("qty",e.qty());m.put("price",e.price());m.put("ts",e.ts());if(e.accountId()!=null)m.put("accountToken",masker.token(owner(e.accountId())));if(e.buyAccountId()!=null)m.put("buyOwnerToken",masker.token(owner(e.buyAccountId())));if(e.sellAccountId()!=null)m.put("sellOwnerToken",masker.token(owner(e.sellAccountId())));return m;}
    public List<AlertEntity> list(String rule,String status,String instrument,int page,int size){return alerts.findAll((root,q,cb)->{List<jakarta.persistence.criteria.Predicate> p=new ArrayList<>();if(rule!=null)p.add(cb.equal(root.get("ruleId"),rule));if(status!=null)p.add(cb.equal(root.get("status"),status));if(instrument!=null)p.add(cb.equal(root.get("instrument"),instrument));q.orderBy(cb.desc(root.get("eventTime")));return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));},org.springframework.data.domain.PageRequest.of(page,size)).getContent();}
    public Optional<AlertEntity> find(long id){return alerts.findById(id);}
}
