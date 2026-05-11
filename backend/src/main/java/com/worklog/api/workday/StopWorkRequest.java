package com.worklog.api.workday;

import java.time.Instant;
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
) {}
