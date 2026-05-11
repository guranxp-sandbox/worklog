package com.worklog.application.workday;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CurrentDayProjectionRepository {
    Optional<CurrentDayView> find(UUID userId, LocalDate date);
    void save(UUID userId, CurrentDayView view);
}
