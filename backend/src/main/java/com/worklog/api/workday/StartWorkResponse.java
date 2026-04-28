package com.worklog.api.workday;

import java.time.Instant;
import java.util.UUID;

public record StartWorkResponse(UUID timeBlockId, Instant startedAt) {}
