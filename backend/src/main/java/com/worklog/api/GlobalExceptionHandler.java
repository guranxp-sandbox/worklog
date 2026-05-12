package com.worklog.api;

import com.worklog.application.workday.OptimisticLockException;
import com.worklog.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.valid4j.errors.RequireViolation;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(final DomainException ex) {
        log.debug("Domain rule violated: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Domain rule violated");
        return problem;
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ProblemDetail handleOptimisticLock(final OptimisticLockException ex) {
        log.debug("Version conflict: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Version conflict");
        return problem;
    }

    @ExceptionHandler(RequireViolation.class)
    public ProblemDetail handleRequireViolation(final RequireViolation ex) {
        log.error("Precondition violated — this is a bug", ex);
        final ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception ex) {
        log.error("Unexpected error", ex);
        final ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        return problem;
    }
}
