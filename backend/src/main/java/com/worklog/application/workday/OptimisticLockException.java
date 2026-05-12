package com.worklog.application.workday;

import com.worklog.domain.workday.WorkDayId;

public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException(final WorkDayId id, final int expected, final int actual) {
        super("Version conflict for " + id.toStreamId() + ": expected " + expected + ", actual " + actual);
    }
}
