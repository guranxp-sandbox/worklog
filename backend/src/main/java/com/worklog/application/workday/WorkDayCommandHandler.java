package com.worklog.application.workday;

import com.worklog.domain.workday.WorkDay;
import com.worklog.domain.workday.WorkDayId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class WorkDayCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkDayCommandHandler.class);

    private final WorkDayRepository workDayRepository;
    private final Clock clock;

    public WorkDayCommandHandler(WorkDayRepository workDayRepository, Clock clock) {
        this.workDayRepository = workDayRepository;
        this.clock = clock;
    }

    public void handle(StartWorkCommand cmd) {
        if (workDayRepository.isRequestAlreadyProcessed(cmd.requestId())) {
            throw new DuplicateRequestException(cmd.requestId());
        }

        WorkDayId workDayId = new WorkDayId(cmd.userId(), cmd.date());

        WorkDay workDay = workDayRepository.load(workDayId)
                .orElseGet(() -> WorkDay.empty(workDayId));

        if (workDay.getVersion() != cmd.expectedVersion()) {
            throw new OptimisticLockException(workDayId, cmd.expectedVersion(), workDay.getVersion());
        }

        Instant now = clock.instant();
        workDay.startWork(cmd.timeBlockId(), cmd.timestamp(), cmd.timezone(), cmd.projectId(), now);

        workDayRepository.save(workDay, cmd.requestId());

        log.info("StartWork processed: workDay={} timeBlock={}", workDayId.toStreamId(), cmd.timeBlockId());
    }

    public void handle(StopWorkCommand cmd) {
        if (workDayRepository.isRequestAlreadyProcessed(cmd.requestId())) {
            throw new DuplicateRequestException(cmd.requestId());
        }

        WorkDayId workDayId = new WorkDayId(cmd.userId(), cmd.date());

        WorkDay workDay = workDayRepository.load(workDayId)
                .orElseThrow(() -> new com.worklog.domain.DomainException(
                        "Cannot stop work: no open time block exists for " + cmd.date()));

        if (workDay.getVersion() != cmd.expectedVersion()) {
            throw new OptimisticLockException(workDayId, cmd.expectedVersion(), workDay.getVersion());
        }

        Instant now = clock.instant();
        workDay.stopWork(cmd.timeBlockId(), cmd.timestamp(), cmd.timezone(), cmd.projectId(), cmd.note(), now);

        workDayRepository.save(workDay, cmd.requestId());

        log.info("StopWork processed: workDay={} timeBlock={}", workDayId.toStreamId(), cmd.timeBlockId());
    }
}
