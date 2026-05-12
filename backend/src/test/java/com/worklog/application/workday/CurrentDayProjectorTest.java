package com.worklog.application.workday;

import com.worklog.domain.workday.WorkDayId;
import com.worklog.domain.workday.events.TimeBlockEnded;
import com.worklog.domain.workday.events.TimeBlockStarted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentDayProjectorTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 4, 28);
    private static final WorkDayId WORK_DAY_ID = new WorkDayId(USER_ID, DATE);
    private static final ZoneId TZ = ZoneId.of("Europe/Stockholm");

    private static final Instant START_AT = Instant.parse("2026-04-28T07:00:00Z");
    private static final Instant STOP_AT  = Instant.parse("2026-04-28T09:00:00Z"); // 120 min

    private InMemoryProjectionRepository repository;
    private CurrentDayProjector projector;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectionRepository();
        projector = new CurrentDayProjector(repository);
    }

    @Test
    void timeBlockStarted_createsWorkingProjection() {
        final UUID timeBlockId = UUID.randomUUID();
        final var event = new TimeBlockStarted(USER_ID, WORK_DAY_ID, timeBlockId, START_AT, TZ, null);

        projector.project(WORK_DAY_ID, 1, List.of(event));

        final CurrentDayView view = repository.find(USER_ID, DATE).orElseThrow();
        assertThat(view.status()).isEqualTo(CurrentDayView.WORKING);
        assertThat(view.version()).isEqualTo(1);
        assertThat(view.activeTimeBlock()).isNotNull();
        assertThat(view.activeTimeBlock().id()).isEqualTo(timeBlockId);
        assertThat(view.activeTimeBlock().startedAt()).isEqualTo(START_AT);
        assertThat(view.completedTimeBlocks()).isEmpty();
        assertThat(view.totalWorkedMinutes()).isZero();
    }

    @Test
    void timeBlockEnded_closesActiveBlock() {
        final UUID timeBlockId = UUID.randomUUID();
        final var started = new TimeBlockStarted(USER_ID, WORK_DAY_ID, timeBlockId, START_AT, TZ, null);
        final var ended = new TimeBlockEnded(USER_ID, WORK_DAY_ID, timeBlockId, STOP_AT, TZ, null, null);

        projector.project(WORK_DAY_ID, 1, List.of(started));
        projector.project(WORK_DAY_ID, 2, List.of(ended));

        final CurrentDayView view = repository.find(USER_ID, DATE).orElseThrow();
        assertThat(view.status()).isEqualTo(CurrentDayView.NOT_WORKING);
        assertThat(view.version()).isEqualTo(2);
        assertThat(view.activeTimeBlock()).isNull();
        assertThat(view.completedTimeBlocks()).hasSize(1);
        assertThat(view.completedTimeBlocks().get(0).minutes()).isEqualTo(120);
        assertThat(view.totalWorkedMinutes()).isEqualTo(120);
    }

    @Test
    void timeBlockEnded_withProjectOverride_usesStopProjectId() {
        final UUID timeBlockId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final var started = new TimeBlockStarted(USER_ID, WORK_DAY_ID, timeBlockId, START_AT, TZ, null);
        final var ended = new TimeBlockEnded(USER_ID, WORK_DAY_ID, timeBlockId, STOP_AT, TZ, projectId, null);

        projector.project(WORK_DAY_ID, 1, List.of(started));
        projector.project(WORK_DAY_ID, 2, List.of(ended));

        final CurrentDayView view = repository.find(USER_ID, DATE).orElseThrow();
        assertThat(view.completedTimeBlocks().get(0).projectId()).isEqualTo(projectId);
    }

    @Test
    void multipleBlocks_accumulatesTotalMinutes() {
        final UUID block1 = UUID.randomUUID();
        final UUID block2 = UUID.randomUUID();
        final Instant start2 = Instant.parse("2026-04-28T10:00:00Z");
        final Instant stop2  = Instant.parse("2026-04-28T11:30:00Z"); // 90 min

        projector.project(WORK_DAY_ID, 1, List.of(
                new TimeBlockStarted(USER_ID, WORK_DAY_ID, block1, START_AT, TZ, null)));
        projector.project(WORK_DAY_ID, 2, List.of(
                new TimeBlockEnded(USER_ID, WORK_DAY_ID, block1, STOP_AT, TZ, null, null)));
        projector.project(WORK_DAY_ID, 3, List.of(
                new TimeBlockStarted(USER_ID, WORK_DAY_ID, block2, start2, TZ, null)));
        projector.project(WORK_DAY_ID, 4, List.of(
                new TimeBlockEnded(USER_ID, WORK_DAY_ID, block2, stop2, TZ, null, null)));

        final CurrentDayView view = repository.find(USER_ID, DATE).orElseThrow();
        assertThat(view.completedTimeBlocks()).hasSize(2);
        assertThat(view.totalWorkedMinutes()).isEqualTo(210); // 120 + 90
    }

    // ── In-memory test double ─────────────────────────────────────────────────

    static class InMemoryProjectionRepository implements CurrentDayProjectionRepository {
        private final java.util.Map<String, CurrentDayView> store = new java.util.HashMap<>();

        @Override
        public Optional<CurrentDayView> find(final UUID userId, final LocalDate date) {
            return Optional.ofNullable(store.get(userId + ":" + date));
        }

        @Override
        public void save(final UUID userId, final CurrentDayView view) {
            store.put(userId + ":" + view.date(), view);
        }
    }
}
