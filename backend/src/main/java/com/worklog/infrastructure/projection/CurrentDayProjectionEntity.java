package com.worklog.infrastructure.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "current_day_projection")
public class CurrentDayProjectionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id; // userId:date

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "active_time_block_id")
    private UUID activeTimeBlockId;

    @Column(name = "active_time_block_started_at")
    private Instant activeTimeBlockStartedAt;

    @Column(name = "active_time_block_project_id")
    private UUID activeTimeBlockProjectId;

    @Column(name = "completed_time_blocks_json", columnDefinition = "TEXT")
    private String completedTimeBlocksJson;

    @Column(name = "total_worked_minutes", nullable = false)
    private int totalWorkedMinutes;

    protected CurrentDayProjectionEntity() {}

    public CurrentDayProjectionEntity(String id, UUID userId, LocalDate date, String status, int version,
                                       UUID activeTimeBlockId, Instant activeTimeBlockStartedAt,
                                       UUID activeTimeBlockProjectId, String completedTimeBlocksJson,
                                       int totalWorkedMinutes) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.status = status;
        this.version = version;
        this.activeTimeBlockId = activeTimeBlockId;
        this.activeTimeBlockStartedAt = activeTimeBlockStartedAt;
        this.activeTimeBlockProjectId = activeTimeBlockProjectId;
        this.completedTimeBlocksJson = completedTimeBlocksJson;
        this.totalWorkedMinutes = totalWorkedMinutes;
    }

    public String getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public String getStatus() { return status; }
    public int getVersion() { return version; }
    public UUID getActiveTimeBlockId() { return activeTimeBlockId; }
    public Instant getActiveTimeBlockStartedAt() { return activeTimeBlockStartedAt; }
    public UUID getActiveTimeBlockProjectId() { return activeTimeBlockProjectId; }
    public String getCompletedTimeBlocksJson() { return completedTimeBlocksJson; }
    public int getTotalWorkedMinutes() { return totalWorkedMinutes; }
}
