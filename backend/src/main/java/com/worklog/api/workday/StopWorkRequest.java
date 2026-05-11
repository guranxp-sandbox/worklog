package com.worklog.api.workday;

import com.worklog.application.workday.StopWorkCommand;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public record StopWorkRequest(
        UUID userId,
        UUID timeBlockId,
        UUID requestId,
        Instant timestamp,
        ZoneId timezone,
        UUID projectId,
        String note,
        int expectedVersion
) {
    public StopWorkCommand toCommand(final LocalDate date, final Instant resolvedTimestamp) {
        return new StopWorkCommand(userId, date, timeBlockId, requestId, resolvedTimestamp, timezone, projectId, note, expectedVersion);
    }
}
