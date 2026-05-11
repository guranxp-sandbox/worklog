package com.worklog.application.workday;

import java.time.Instant;
import java.util.UUID;

public record TimeBlockView(
        UUID id,
        Instant startedAt,
        Instant endedAt,
        UUID projectId,
        Integer minutes
) {}
