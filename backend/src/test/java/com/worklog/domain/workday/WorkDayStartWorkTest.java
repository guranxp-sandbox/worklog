package com.worklog.domain.workday;

import com.worklog.domain.DomainException;
import com.worklog.domain.workday.events.TimeBlockStarted;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class WorkDayStartWorkTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 4, 28);
    private static final ZoneId TZ = ZoneId.of("Europe/Stockholm");
    private static final WorkDayId WORK_DAY_ID = new WorkDayId(USER_ID, DATE);

    // 2026-04-28T08:00:00 Stockholm = 2026-04-28T06:00:00Z (CEST = UTC+2)
    private static final Instant START_AT = Instant.parse("2026-04-28T06:00:00Z");
    private static final Instant NOW = Instant.parse("2026-04-28T07:00:00Z");

    @Test
    void happyPath_emitsTimeBlockStarted() {
        WorkDay workDay = WorkDay.empty(WORK_DAY_ID);
        UUID timeBlockId = UUID.randomUUID();

        workDay.startWork(timeBlockId, START_AT, TZ, null, NOW);

        assertThat(workDay.getUncommittedEvents()).hasSize(1);
        TimeBlockStarted event = (TimeBlockStarted) workDay.getUncommittedEvents().get(0);
        assertThat(event.timeBlockId()).isEqualTo(timeBlockId);
        assertThat(event.startedAt()).isEqualTo(START_AT);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.workDayId()).isEqualTo(WORK_DAY_ID);
        assertThat(event.timezone()).isEqualTo(TZ);
        assertThat(event.projectId()).isNull();
    }

    @Test
    void happyPath_withProject() {
        WorkDay workDay = WorkDay.empty(WORK_DAY_ID);
        UUID projectId = UUID.randomUUID();

        workDay.startWork(UUID.randomUUID(), START_AT, TZ, projectId, NOW);

        TimeBlockStarted event = (TimeBlockStarted) workDay.getUncommittedEvents().get(0);
        assertThat(event.projectId()).isEqualTo(projectId);
    }

    @Test
    void rejects_futureTimestamp() {
        WorkDay workDay = WorkDay.empty(WORK_DAY_ID);
        Instant future = NOW.plusSeconds(60);

        assertThatThrownBy(() -> workDay.startWork(UUID.randomUUID(), future, TZ, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("future");
    }

    @Test
    void rejects_timestampOnWrongDate() {
        WorkDay workDay = WorkDay.empty(WORK_DAY_ID);
        // 2026-04-27T22:01:00Z = 2026-04-28T00:01:00 CEST  → still April 28 in Stockholm, OK
        // 2026-04-27T21:59:00Z = 2026-04-27T23:59:00 CEST  → April 27 in Stockholm, WRONG date
        Instant wrongDate = Instant.parse("2026-04-27T21:59:00Z");
        Instant nowYesterday = Instant.parse("2026-04-27T22:30:00Z");

        assertThatThrownBy(() -> workDay.startWork(UUID.randomUUID(), wrongDate, TZ, null, nowYesterday))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("2026-04-28");
    }

    @Test
    void rejects_whenOpenTimeBlockExists() {
        WorkDay workDay = WorkDay.empty(WORK_DAY_ID);
        workDay.startWork(UUID.randomUUID(), START_AT, TZ, null, NOW);

        // laterStart is before NOW so the future-timestamp guard does not fire first
        Instant laterStart = Instant.parse("2026-04-28T06:30:00Z");
        assertThatThrownBy(() -> workDay.startWork(UUID.randomUUID(), laterStart, TZ, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("open time block");
    }

    @Test
    void version_isZeroForEmptyWorkDay() {
        WorkDay workDay = WorkDay.empty(WORK_DAY_ID);
        assertThat(workDay.getVersion()).isZero();
    }

    @Test
    void version_reflectsCommittedEvents_afterReconstitution() {
        WorkDay original = WorkDay.empty(WORK_DAY_ID);
        original.startWork(UUID.randomUUID(), START_AT, TZ, null, NOW);
        var events = original.getUncommittedEvents();

        WorkDay reconstituted = WorkDay.reconstitute(WORK_DAY_ID, events);

        assertThat(reconstituted.getVersion()).isEqualTo(1);
        assertThat(reconstituted.hasOpenTimeBlock()).isTrue();
    }

    @Test
    void timestamp_exactlyEqualToNow_isAccepted() {
        WorkDay workDay = WorkDay.empty(WORK_DAY_ID);

        assertThatCode(() -> workDay.startWork(UUID.randomUUID(), NOW, TZ, null, NOW))
                .doesNotThrowAnyException();
    }
}
