package com.worklog.domain.workday;

import java.time.LocalDate;
import java.util.UUID;

public record WorkDayId(UUID userId, LocalDate date) {

    public String toStreamId() {
        return userId + ":" + date;
    }

    public static WorkDayId fromStreamId(final String streamId) {
        final int sep = streamId.indexOf(':');
        final UUID userId = UUID.fromString(streamId.substring(0, sep));
        final LocalDate date = LocalDate.parse(streamId.substring(sep + 1));
        return new WorkDayId(userId, date);
    }
}
