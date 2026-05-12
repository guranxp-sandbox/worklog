package com.worklog.infrastructure.eventstore;

import com.worklog.application.workday.CurrentDayProjector;
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
    private final CurrentDayProjector projector;

    public JpaWorkDayRepository(final EventStoreJpaRepository jpaRepository, final EventSerializer serializer,
                                 final CurrentDayProjector projector) {
        this.jpaRepository = jpaRepository;
        this.serializer = serializer;
        this.projector = projector;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRequestAlreadyProcessed(final UUID requestId) {
        return jpaRepository.existsByRequestId(requestId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkDay> load(final WorkDayId id) {
        final List<EventRecord> records = jpaRepository.findByStreamIdOrderBySequenceNumber(id.toStreamId());
        if (records.isEmpty()) {
            return Optional.empty();
        }
        final List<DomainEvent> history = records.stream()
                .map(r -> serializer.deserialize(r.getType(), r.getPayload()))
                .toList();
        return Optional.of(WorkDay.reconstitute(id, history));
    }

    @Override
    @Transactional
    public void save(final WorkDay workDay, final UUID requestId) {
        if (jpaRepository.existsByRequestId(requestId)) {
            throw new DuplicateRequestException(requestId);
        }

        final List<DomainEvent> uncommitted = workDay.getUncommittedEvents();
        int nextSeq = workDay.getVersion() + 1;

        for (final DomainEvent event : uncommitted) {
            final EventRecord record = new EventRecord(
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
            } catch (final DataIntegrityViolationException e) {
                throw new com.worklog.application.workday.OptimisticLockException(
                        workDay.getId(), workDay.getVersion(), nextSeq - 2);
            }
        }

        final int newVersion = workDay.getVersion() + uncommitted.size();
        projector.project(workDay.getId(), newVersion, uncommitted);

        workDay.markEventsAsCommitted();
    }
}
