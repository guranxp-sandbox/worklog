package com.worklog.application.workday;

import com.worklog.domain.workday.WorkDayId;

public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException(WorkDayId id, int expected, int actual) {
        super("Version conflict for " + id.toStreamId() + ": expected " + expected + ", actual " + actual);
    }
}
