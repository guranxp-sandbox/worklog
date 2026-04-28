package com.worklog.application.workday;

import com.worklog.domain.workday.WorkDay;
import com.worklog.domain.workday.WorkDayId;

import java.util.Optional;
import java.util.UUID;

public interface WorkDayRepository {

    Optional<WorkDay> load(WorkDayId id);

    boolean isRequestAlreadyProcessed(UUID requestId);

    /**
     * Persists uncommitted events from workDay.
     * Throws DuplicateRequestException if requestId was already processed.
     * Throws OptimisticLockException if the stream version conflicts.
     */
    void save(WorkDay workDay, UUID requestId);
}
