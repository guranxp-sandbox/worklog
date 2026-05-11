package com.worklog.application.workday;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public record StopWorkCommand(
        UUID userId,
        LocalDate date,
        UUID timeBlockId,
        UUID requestId,
        Instant timestamp,
        ZoneId timezone,
        UUID projectId,
        String note,
        int expectedVersion
) {}
