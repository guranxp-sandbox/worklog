package com.worklog.domain.workday.events;

import com.worklog.domain.DomainEvent;
import com.worklog.domain.workday.WorkDayId;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public record TimeBlockStarted(
        UUID userId,
        WorkDayId workDayId,
        UUID timeBlockId,
        Instant startedAt,
        ZoneId timezone,
        UUID projectId
) implements DomainEvent {}
