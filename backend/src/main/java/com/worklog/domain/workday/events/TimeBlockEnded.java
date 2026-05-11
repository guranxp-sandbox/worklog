package com.worklog.domain.workday.events;

import com.worklog.domain.DomainEvent;
import com.worklog.domain.workday.WorkDayId;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public record TimeBlockEnded(
        UUID userId,
        WorkDayId workDayId,
        UUID timeBlockId,
        Instant endedAt,
        ZoneId timezone,
        UUID projectId,
        String note
) implements DomainEvent {}