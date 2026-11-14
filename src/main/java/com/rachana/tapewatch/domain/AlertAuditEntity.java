package com.rachana.tapewatch.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="alert_audit")
public class AlertAuditEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long alertId; private String actingUser; private String fromStatus; private String toStatus;
    @Column(length=2000) private String note; @Column(name="created_at") private Instant timestamp;
    protected AlertAuditEntity(){}
    public AlertAuditEntity(Long alertId,String actingUser,String fromStatus,String toStatus,String note){this.alertId=alertId;this.actingUser=actingUser;this.fromStatus=fromStatus;this.toStatus=toStatus;this.note=note==null?"":note;this.timestamp=Instant.now();}
    public Long getId(){return id;} public Long getAlertId(){return alertId;} public String getActingUser(){return actingUser;}
    public String getFromStatus(){return fromStatus;} public String getToStatus(){return toStatus;} public String getNote(){return note;} public Instant getTimestamp(){return timestamp;}
}
