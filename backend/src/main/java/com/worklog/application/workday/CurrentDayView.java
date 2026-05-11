package com.worklog.application.workday;

import java.time.LocalDate;
import java.util.List;

public record CurrentDayView(
        LocalDate date,
        String status,
        int version,
        TimeBlockView activeTimeBlock,
        List<TimeBlockView> completedTimeBlocks,
        int totalWorkedMinutes
) {
    public static final String WORKING = "WORKING";
    public static final String NOT_WORKING = "NOT_WORKING";
}
