package com.worklog.domain.workday;

import java.time.Instant;
import java.util.UUID;

public class TimeBlock {

    private final UUID id;
    private final Instant startedAt;
    private Instant endedAt;
    private UUID projectId;

    public TimeBlock(UUID id, Instant startedAt, Instant endedAt, UUID projectId) {
        this.id = id;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.projectId = projectId;
    }

    public boolean isOpen() {
        return endedAt == null;
    }

    public void close(Instant endedAt, UUID projectId) {
        this.endedAt = endedAt;
        if (projectId != null) {
            this.projectId = projectId;
        }
    }

    public UUID getId() { return id; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public UUID getProjectId() { return projectId; }
}
