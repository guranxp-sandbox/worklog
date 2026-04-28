package com.worklog.domain.workday;

import java.time.LocalDate;
import java.util.UUID;

public record WorkDayId(UUID userId, LocalDate date) {

    public String toStreamId() {
        return userId + ":" + date;
    }

    public static WorkDayId fromStreamId(String streamId) {
        int sep = streamId.indexOf(':');
        UUID userId = UUID.fromString(streamId.substring(0, sep));
        LocalDate date = LocalDate.parse(streamId.substring(sep + 1));
        return new WorkDayId(userId, date);
    }
}
