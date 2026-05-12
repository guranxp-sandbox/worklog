package com.worklog.application.workday;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class WorkDayQueryService {

    private final CurrentDayProjectionRepository projectionRepository;

    public WorkDayQueryService(final CurrentDayProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public CurrentDayView getCurrentDayView(final UUID userId, final LocalDate date) {
        return projectionRepository.find(userId, date)
                .orElseGet(() -> new CurrentDayView(date, CurrentDayView.NOT_WORKING, 0, null, List.of(), 0));
    }
}
