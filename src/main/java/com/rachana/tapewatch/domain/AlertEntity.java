package com.rachana.tapewatch.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="alerts")
public class AlertEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private String ruleId; private String ownerToken; private String instrument; private String severity;
    private String status; private Instant eventTime; private Instant createdAt;
    @Column(length=4000) private String evidenceEventIds;
    protected AlertEntity() {}
    public AlertEntity(String ruleId, String ownerToken, String instrument, String severity, Instant eventTime, String evidenceEventIds) {
        this.ruleId=ruleId; this.ownerToken=ownerToken; this.instrument=instrument; this.severity=severity;
        this.status="OPEN"; this.eventTime=eventTime; this.createdAt=Instant.now(); this.evidenceEventIds=evidenceEventIds;
    }
    public Long getId(){return id;} public String getRuleId(){return ruleId;} public String getOwnerToken(){return ownerToken;}
    public String getInstrument(){return instrument;} public String getSeverity(){return severity;} public String getStatus(){return status;}
    public Instant getEventTime(){return eventTime;} public Instant getCreatedAt(){return createdAt;} public String getEvidenceEventIds(){return evidenceEventIds;}
    public void setStatus(String status){this.status=status;}
}
