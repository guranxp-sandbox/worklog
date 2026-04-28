package com.worklog.infrastructure.eventstore;

import com.worklog.application.workday.DuplicateRequestException;
import com.worklog.application.workday.WorkDayRepository;
import com.worklog.domain.DomainEvent;
import com.worklog.domain.workday.WorkDay;
import com.worklog.domain.workday.WorkDayId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaWorkDayRepository implements WorkDayRepository {

    private final EventStoreJpaRepository jpaRepository;
    private final EventSerializer serializer;

    public JpaWorkDayRepository(EventStoreJpaRepository jpaRepository, EventSerializer serializer) {
        this.jpaRepository = jpaRepository;
        this.serializer = serializer;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRequestAlreadyProcessed(UUID requestId) {
        return jpaRepository.existsByRequestId(requestId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkDay> load(WorkDayId id) {
        List<EventRecord> records = jpaRepository.findByStreamIdOrderBySequenceNumber(id.toStreamId());
        if (records.isEmpty()) {
            return Optional.empty();
        }
        List<DomainEvent> history = records.stream()
                .map(r -> serializer.deserialize(r.getType(), r.getPayload()))
                .toList();
        return Optional.of(WorkDay.reconstitute(id, history));
    }

    @Override
    @Transactional
    public void save(WorkDay workDay, UUID requestId) {
        if (jpaRepository.existsByRequestId(requestId)) {
            throw new DuplicateRequestException(requestId);
        }

        List<DomainEvent> uncommitted = workDay.getUncommittedEvents();
        int nextSeq = workDay.getVersion() + 1;

        for (DomainEvent event : uncommitted) {
            EventRecord record = new EventRecord(
                    UUID.randomUUID(),
                    workDay.getId().toStreamId(),
                    nextSeq++,
                    serializer.typeOf(event),
                    serializer.serialize(event),
                    requestId,
                    Instant.now()
            );
            try {
                jpaRepository.save(record);
                jpaRepository.flush();
            } catch (DataIntegrityViolationException e) {
                throw new com.worklog.application.workday.OptimisticLockException(
                        workDay.getId(), workDay.getVersion(), nextSeq - 2);
            }
        }

        workDay.markEventsAsCommitted();
    }
}
