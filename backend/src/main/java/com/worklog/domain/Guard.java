package com.worklog.domain;

public final class Guard {

    private Guard() {}

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new DomainException(message);
        }
    }
}
