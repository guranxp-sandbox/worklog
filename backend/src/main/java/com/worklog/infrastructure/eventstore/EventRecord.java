package com.worklog.infrastructure.eventstore;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "domain_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_domain_events_stream_seq", columnNames = {"stream_id", "sequence_number"}),
                @UniqueConstraint(name = "uc_domain_events_request_id", columnNames = {"request_id"})
        }
)
public class EventRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "stream_id", nullable = false)
    private String streamId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected EventRecord() {}

    public EventRecord(final UUID id, final String streamId, final int sequenceNumber, final String type,
                       final String payload, final UUID requestId, final Instant occurredAt) {
        this.id = id;
        this.streamId = streamId;
        this.sequenceNumber = sequenceNumber;
        this.type = type;
        this.payload = payload;
        this.requestId = requestId;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public String getStreamId() { return streamId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public UUID getRequestId() { return requestId; }
    public Instant getOccurredAt() { return occurredAt; }
}
