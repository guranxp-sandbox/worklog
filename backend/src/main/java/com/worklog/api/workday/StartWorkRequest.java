package com.worklog.api.workday;

import com.worklog.application.workday.StartWorkCommand;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public record StartWorkRequest(
        UUID userId,
        UUID timeBlockId,
        UUID requestId,
        Instant timestamp,
        ZoneId timezone,
        UUID projectId,
        int expectedVersion
) {
    public StartWorkCommand toCommand(final LocalDate date, final Instant resolvedTimestamp) {
        return new StartWorkCommand(userId, date, timeBlockId, requestId, resolvedTimestamp, timezone, projectId, expectedVersion);
    }
}
