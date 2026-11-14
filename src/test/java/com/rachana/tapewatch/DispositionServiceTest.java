package com.rachana.tapewatch;

import com.rachana.tapewatch.domain.*;
import com.rachana.tapewatch.store.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DispositionServiceTest {
    @Test void validTransitionWritesOneAuditEntry(){
        AlertRepository alerts=mock(AlertRepository.class);AlertAuditRepository audit=mock(AlertAuditRepository.class);
        AlertEntity alert=new AlertEntity("WT-1","OWN-token","XYZ","HIGH",Instant.now(),"ev1");when(alerts.findById(7L)).thenReturn(Optional.of(alert));
        new DispositionService(alerts,audit).transition(7,"ESCALATED","Escalating for analyst review","analyst-1");
        assertEquals("ESCALATED",alert.getStatus());verify(audit,times(1)).save(any(AlertAuditEntity.class));
    }
    @Test void invalidTransitionConflictsAndWritesNoAudit(){
        AlertRepository alerts=mock(AlertRepository.class);AlertAuditRepository audit=mock(AlertAuditRepository.class);
        AlertEntity alert=new AlertEntity("WT-1","OWN-token","XYZ","HIGH",Instant.now(),"ev1");alert.setStatus("CLOSED_FALSE_POSITIVE");when(alerts.findById(7L)).thenReturn(Optional.of(alert));
        assertThrows(IllegalStateException.class,()->new DispositionService(alerts,audit).transition(7,"ESCALATED","Reopen alert please","analyst-1"));
        verify(audit,never()).save(any());
    }
}
