package com.worklog.infrastructure.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventStoreJpaRepository extends JpaRepository<EventRecord, UUID> {

    List<EventRecord> findByStreamIdOrderBySequenceNumber(String streamId);

    boolean existsByRequestId(UUID requestId);
}
