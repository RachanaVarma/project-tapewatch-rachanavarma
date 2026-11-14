package com.rachana.tapewatch.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="market_events")
public class EventEntity {
    @Id private String eventId; @Column(length=8000) private String payload; private Instant eventTime;
    protected EventEntity() {}
    public EventEntity(String eventId,String payload,Instant eventTime){this.eventId=eventId;this.payload=payload;this.eventTime=eventTime;}
    public String getEventId(){return eventId;} public String getPayload(){return payload;} public Instant getEventTime(){return eventTime;}
}
