package com.worklog.application.workday;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public record StartWorkCommand(
        UUID userId,
        LocalDate date,
        UUID timeBlockId,
        UUID requestId,
        Instant timestamp,
        ZoneId timezone,
        UUID projectId,
        int expectedVersion
) {}
