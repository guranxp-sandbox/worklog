package com.worklog.domain.workday;

import com.worklog.domain.DomainException;
import com.worklog.domain.workday.events.TimeBlockEnded;
import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

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
    private static final Instant STOP_AT  = Instant.parse("2026-04-28T10:00:00Z");
    private static final Instant NOW      = Instant.parse("2026-04-28T11:00:00Z");

    private WorkDay workDayWithOpenBlock(final UUID timeBlockId) {
        final WorkDay wd = WorkDay.empty(WORK_DAY_ID);
        wd.startWork(timeBlockId, START_AT, TZ, null, NOW);
        return wd;
    }

    @Test
    void happyPath_emitsTimeBlockEnded() {
        final UUID timeBlockId = UUID.randomUUID();
        final WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        workDay.stopWork(timeBlockId, STOP_AT, TZ, null, null, NOW);

        final TimeBlockEnded event = workDay.getUncommittedEvents().stream()
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
        final UUID timeBlockId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        workDay.stopWork(timeBlockId, STOP_AT, TZ, projectId, "focused session", NOW);

        final TimeBlockEnded event = workDay.getUncommittedEvents().stream()
                .filter(e -> e instanceof TimeBlockEnded)
                .map(e -> (TimeBlockEnded) e)
                .findFirst().orElseThrow();

        assertThat(event.projectId()).isEqualTo(projectId);
        assertThat(event.note()).isEqualTo("focused session");
    }

    @Test
    void happyPath_closesOpenBlock() {
        final UUID timeBlockId = UUID.randomUUID();
        final WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        workDay.stopWork(timeBlockId, STOP_AT, TZ, null, null, NOW);

        assertThat(workDay.hasOpenTimeBlock()).isFalse();
    }

    @Test
    void requireViolation_futureTimestamp() {
        final UUID timeBlockId = UUID.randomUUID();
        final WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        assertThatThrownBy(() -> workDay.stopWork(timeBlockId, NOW.plusSeconds(60), TZ, null, null, NOW))
                .isInstanceOf(RequireViolation.class);
    }

    @Test
    void domainException_whenNoOpenBlock() {
        assertThatThrownBy(() -> WorkDay.empty(WORK_DAY_ID).stopWork(UUID.randomUUID(), STOP_AT, TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no open time block");
    }

    @Test
    void domainException_whenTimeBlockIdDoesNotMatch() {
        final UUID timeBlockId = UUID.randomUUID();
        final WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        assertThatThrownBy(() -> workDay.stopWork(UUID.randomUUID(), STOP_AT, TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void domainException_endTimeEqualToStartTime() {
        final UUID timeBlockId = UUID.randomUUID();
        final WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        assertThatThrownBy(() -> workDay.stopWork(timeBlockId, START_AT, TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("strictly after");
    }

    @Test
    void domainException_endTimeBeforeStartTime() {
        final UUID timeBlockId = UUID.randomUUID();
        final WorkDay workDay = workDayWithOpenBlock(timeBlockId);

        assertThatThrownBy(() -> workDay.stopWork(timeBlockId, START_AT.minusSeconds(1), TZ, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("strictly after");
    }

    @Test
    void reconstitute_closedBlock_hasNoOpenBlock() {
        final UUID timeBlockId = UUID.randomUUID();
        final WorkDay original = WorkDay.empty(WORK_DAY_ID);
        original.startWork(timeBlockId, START_AT, TZ, null, NOW);
        original.stopWork(timeBlockId, STOP_AT, TZ, null, null, NOW);

        final WorkDay reconstituted = WorkDay.reconstitute(WORK_DAY_ID, original.getUncommittedEvents());

        assertThat(reconstituted.hasOpenTimeBlock()).isFalse();
        assertThat(reconstituted.getVersion()).isEqualTo(2);
    }
}
