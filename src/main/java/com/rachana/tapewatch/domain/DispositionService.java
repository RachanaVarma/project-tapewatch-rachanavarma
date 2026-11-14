package com.rachana.tapewatch.domain;
import com.rachana.tapewatch.store.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service public class DispositionService {
    private final AlertRepository alerts;private final AlertAuditRepository audit;
    public DispositionService(AlertRepository alerts,AlertAuditRepository audit){this.alerts=alerts;this.audit=audit;}
    @Transactional public AlertEntity transition(long id,String target,String note,String user){
        AlertEntity a=alerts.findById(id).orElseThrow(()->new NoSuchElementException("Alert not found"));String from=a.getStatus();
        boolean allowed=(from.equals("OPEN")&&(target.equals("ESCALATED")||target.equals("CLOSED_FALSE_POSITIVE")))||(from.equals("ESCALATED")&&target.equals("CLOSED_FALSE_POSITIVE"));
        if(!allowed)throw new IllegalStateException("Invalid alert status transition");
        if(target.equals("CLOSED_FALSE_POSITIVE")&&(note==null||note.trim().length()<10))throw new IllegalArgumentException("note must contain at least 10 characters when closing");
        if(user==null||user.isBlank())throw new IllegalArgumentException("X-User header is required");
        a.setStatus(target);alerts.save(a);audit.save(new AlertAuditEntity(id,user,from,target,note));return a;
    }
    public List<AlertAuditEntity> history(long id){return audit.findAllByAlertIdOrderByTimestampAsc(id);}
}
