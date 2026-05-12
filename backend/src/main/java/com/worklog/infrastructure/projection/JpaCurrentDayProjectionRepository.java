package com.worklog.infrastructure.projection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worklog.application.workday.CurrentDayProjectionRepository;
import com.worklog.application.workday.CurrentDayView;
import com.worklog.application.workday.TimeBlockView;
import com.worklog.domain.workday.WorkDayId;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCurrentDayProjectionRepository implements CurrentDayProjectionRepository {

    private final CurrentDayProjectionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public JpaCurrentDayProjectionRepository(final CurrentDayProjectionJpaRepository jpaRepository,
                                              final ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CurrentDayView> find(final UUID userId, final LocalDate date) {
        final String id = new WorkDayId(userId, date).toStreamId();
        return jpaRepository.findById(id).map(this::toView);
    }

    @Override
    public void save(final UUID userId, final CurrentDayView view) {
        final String id = new WorkDayId(userId, view.date()).toStreamId();

        final TimeBlockView active = view.activeTimeBlock();
        final CurrentDayProjectionEntity entity = new CurrentDayProjectionEntity(
                id,
                userId,
                view.date(),
                view.status(),
                view.version(),
                active != null ? active.id() : null,
                active != null ? active.startedAt() : null,
                active != null ? active.projectId() : null,
                serializeCompleted(view.completedTimeBlocks()),
                view.totalWorkedMinutes()
        );
        jpaRepository.save(entity);
    }

    private CurrentDayView toView(final CurrentDayProjectionEntity e) {
        final TimeBlockView active = e.getActiveTimeBlockId() != null
                ? new TimeBlockView(e.getActiveTimeBlockId(), e.getActiveTimeBlockStartedAt(), null,
                                    e.getActiveTimeBlockProjectId(), null)
                : null;
        final List<TimeBlockView> completed = deserializeCompleted(e.getCompletedTimeBlocksJson());
        return new CurrentDayView(e.getDate(), e.getStatus(), e.getVersion(), active, completed, e.getTotalWorkedMinutes());
    }

    private String serializeCompleted(final List<TimeBlockView> blocks) {
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (final JsonProcessingException ex) {
            throw new ProjectionSerializationException("Failed to serialize completed time blocks", ex);
        }
    }

    private List<TimeBlockView> deserializeCompleted(final String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (final JsonProcessingException ex) {
            throw new ProjectionSerializationException("Failed to deserialize completed time blocks", ex);
        }
    }

    public static class ProjectionSerializationException extends RuntimeException {
        public ProjectionSerializationException(final String message, final Throwable cause) { super(message, cause); }
    }
}
