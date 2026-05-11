package com.worklog.infrastructure.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrentDayProjectionJpaRepository extends JpaRepository<CurrentDayProjectionEntity, String> {
    Optional<CurrentDayProjectionEntity> findById(String id);
}
