package com.worklog.application.workday;

import com.worklog.domain.DomainException;
import com.worklog.domain.workday.WorkDay;
import com.worklog.domain.workday.WorkDayId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class WorkDayCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkDayCommandHandler.class);

    private final WorkDayRepository workDayRepository;
    private final Clock clock;

    public WorkDayCommandHandler(final WorkDayRepository workDayRepository, final Clock clock) {
        this.workDayRepository = workDayRepository;
        this.clock = clock;
    }

    public void handle(final StartWorkCommand cmd) {
        checkIdempotency(cmd.requestId());
        final WorkDayId workDayId = new WorkDayId(cmd.userId(), cmd.date());
        final WorkDay workDay = workDayRepository.load(workDayId).orElseGet(() -> WorkDay.empty(workDayId));
        checkVersion(workDayId, workDay, cmd.expectedVersion());

        workDay.startWork(cmd.timeBlockId(), cmd.timestamp(), cmd.timezone(), cmd.projectId(), clock.instant());
        workDayRepository.save(workDay, cmd.requestId());

        log.info("StartWork processed: workDay={} timeBlock={}", workDayId.toStreamId(), cmd.timeBlockId());
    }

    public void handle(final StopWorkCommand cmd) {
        checkIdempotency(cmd.requestId());
        final WorkDayId workDayId = new WorkDayId(cmd.userId(), cmd.date());
        final WorkDay workDay = workDayRepository.load(workDayId)
                .orElseThrow(() -> new DomainException("Cannot stop work: no open time block exists for " + cmd.date()));
        checkVersion(workDayId, workDay, cmd.expectedVersion());

        workDay.stopWork(cmd.timeBlockId(), cmd.timestamp(), cmd.timezone(), cmd.projectId(), cmd.note(), clock.instant());
        workDayRepository.save(workDay, cmd.requestId());

        log.info("StopWork processed: workDay={} timeBlock={}", workDayId.toStreamId(), cmd.timeBlockId());
    }

    private void checkIdempotency(final UUID requestId) {
        if (workDayRepository.isRequestAlreadyProcessed(requestId)) {
            throw new DuplicateRequestException(requestId);
        }
    }

    private void checkVersion(final WorkDayId workDayId, final WorkDay workDay, final int expectedVersion) {
        if (workDay.getVersion() != expectedVersion) {
            throw new OptimisticLockException(workDayId, expectedVersion, workDay.getVersion());
        }
    }
}
