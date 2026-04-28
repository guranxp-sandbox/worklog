package com.worklog.application.workday;

import java.util.UUID;

public class DuplicateRequestException extends RuntimeException {

    public DuplicateRequestException(UUID requestId) {
        super("Request " + requestId + " was already processed");
    }
}
