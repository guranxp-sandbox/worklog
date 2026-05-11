package com.worklog.domain.workday;

import com.worklog.domain.DomainException;
import com.worklog.domain.workday.events.TimeBlockEnded;
import com.worklog.domain.workday.events.TimeBlockStarted;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class WorkDayStopWorkTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 4, 28);
    private static final ZoneId TZ = ZoneId.of("Europe/Stockholm");
    private static final WorkDayId WORK_DAY_ID = new WorkDayId(USER_ID, DATE);

    private static final Instant START_AT = Instant.parse("2026-04-28T06:00:00Z");
    private static final Instant STOP_AT = Instant.parse("2026-04-28T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-04-28T11:00:00Z");

    private WorkDay workDayWithOpenBlock(UUID timeBlockId) {
        WorkDay wd = WorkDay.empty(WORK_DAY_ID);
        wd.startWork(timeBlockId, START_AT, TZ, null, NOW);
        return wd;
    }

    @Test
    void happyPath_emitsTimeBlockEnded() {
        UUID timeBlockId = UUID.randomUUID();
        WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        workDay.stopWork(timeBlockId, STOP_AT, TZ, null, null, NOW);

        long ended = workDay.getUncommittedEvents().stream()
                .filter(e -> e instanceof TimeBlockEnded)
                .count();
        assertThat(ended).isEqualTo(1);

        TimeBlockEnded event = workDay.getUncommittedEvents().stream()
                .filter(e -> e instanceof TimeBlockEnded)
                .map(e -> (TimeBlockEnded) e)
                .findFirst().orElseThrow();

        assertThat(event.timeBlockId()).isEqualTo(timeBlockId);
        assertThat(event.endedAt()).isEqualTo(STOP_AT);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.workDayId()).isEqualTo(WORK_DAY_ID);
        assertThat(event.timezone()).isEqualTo(TZ);
        assertThat(event.projectId()).isNull();
        assertThat(event.note()).isNull();
    }

    @Test
    void happyPath_withProjectAndNote() {
        UUID timeBlockId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        workDay.stopWork(timeBlockId, STOP_AT, TZ, projectId, "focused session", NOW);

        TimeBlockEnded event = workDay.getUncommittedEvents().stream()
                .filter(e -> e instanceof TimeBlockEnded)
                .map(e -> (TimeBlockEnded) e)
                .findFirst().orElseThrow();

        assertThat(event.projectId()).isEqualTo(projectId);
        assertThat(event.note()).isEqualTo("focused session");
    }

    @Test
    void happyPath_closesOpenBlock() {
        UUID timeBlockId = UUID.randomUUID();
        WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        workDay.stopWork(timeBlockId, STOP_AT, TZ, null, null, NOW);

        assertThat(workDay.hasOpenTimeBlock()).isFalse();
    }

    @Test
    void rejects_futureTimestamp() {
        UUID timeBlockId = UUID.randomUUID();
        WorkDay workDay = workDayWithOpenBlock(timeBlockId);
        Instant future = NOW.plusSeconds(60);

        assertThatThrownBy(() -> workDay.stopWork(timeBlockId, future, TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("future");
    }

    @Test
    void rejects_whenNoOpenBlock() {
        WorkDay workDay = WorkDay.empty(WORK_DAY_ID);

        assertThatThrownBy(() -> workDay.stopWork(UUID.randomUUID(), STOP_AT, TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no open time block");
    }

    @Test
    void rejects_whenTimeBlockIdDoesNotMatch() {
        UUID timeBlockId = UUID.randomUUID();
        WorkDay workDay = workDayWithOpenBlock(timeBlockId);
        UUID wrongId = UUID.randomUUID();

        assertThatThrownBy(() -> workDay.stopWork(wrongId, STOP_AT, TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejects_endTimeEqualToStartTime() {
        UUID timeBlockId = UUID.randomUUID();
        WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        assertThatThrownBy(() -> workDay.stopWork(timeBlockId, START_AT, TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("strictly after");
    }

    @Test
    void rejects_endTimeBeforeStartTime() {
        UUID timeBlockId = UUID.randomUUID();
        WorkDay workDay = workDayWithOpenBlock(timeBlockId);
        Instant before = START_AT.minusSeconds(1);

        assertThatThrownBy(() -> workDay.stopWork(timeBlockId, before, TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("strictly after");
    }

    @Test
    void reconstitute_closedBlock_hasNoOpenBlock() {
        UUID timeBlockId = UUID.randomUUID();
        WorkDay original = WorkDay.empty(WORK_DAY_ID);
        original.startWork(timeBlockId, START_AT, TZ, null, NOW);
        original.stopWork(timeBlockId, STOP_AT, TZ, null, null, NOW);

        WorkDay reconstituted = WorkDay.reconstitute(WORK_DAY_ID, original.getUncommittedEvents());

        assertThat(reconstituted.hasOpenTimeBlock()).isFalse();
        assertThat(reconstituted.getVersion()).isEqualTo(2);
    }
}
