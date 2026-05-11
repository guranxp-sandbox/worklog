package com.worklog.domain.workday;

import com.worklog.domain.DomainEvent;
import com.worklog.domain.DomainException;
import com.worklog.domain.workday.events.TimeBlockEnded;
import com.worklog.domain.workday.events.TimeBlockStarted;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.valid4j.Assertive.require;

/**
 * Aggregate root for a single calendar day's work session.
 *
 * version = number of committed events in the event store.
 * After reconstitution from N stored events, version == N.
 * Uncommitted events (from commands in the current session) do not increment version
 * until they are persisted and the aggregate is reloaded.
 */
public class WorkDay {

    private final WorkDayId id;
    private final List<TimeBlock> timeBlocks = new ArrayList<>();
    private int version = 0;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    private WorkDay(final WorkDayId id) {
        this.id = id;
    }

    // ── Factory methods ──────────────────────────────────────────────────────

    public static WorkDay empty(final WorkDayId id) {
        return new WorkDay(id);
    }

    public static WorkDay reconstitute(final WorkDayId id, final List<DomainEvent> history) {
        final WorkDay wd = new WorkDay(id);
        for (final DomainEvent event : history) {
            wd.applyHistory(event);
        }
        return wd;
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    public void startWork(final UUID timeBlockId, final Instant timestamp, final ZoneId timezone,
                          final UUID projectId, final Instant now) {
        require(timeBlockId, notNullValue());
        require(timestamp, notNullValue());
        require(timezone, notNullValue());
        require(now, notNullValue());
        require(timestamp, is(not(greaterThan(now))));
        require(timestamp.atZone(timezone).toLocalDate(), is(id.date()));

        if (hasOpenTimeBlock()) {
            throw new DomainException("Cannot start work: an open time block already exists for " + id.date());
        }

        final TimeBlockStarted event = new TimeBlockStarted(id.userId(), id, timeBlockId, timestamp, timezone, projectId);
        applyNew(event);
        uncommittedEvents.add(event);
    }

    public void stopWork(final UUID timeBlockId, final Instant timestamp, final ZoneId timezone,
                         final UUID projectId, final String note, final Instant now) {
        require(timeBlockId, notNullValue());
        require(timestamp, notNullValue());
        require(timezone, notNullValue());
        require(now, notNullValue());
        require(timestamp, is(not(greaterThan(now))));

        final TimeBlock openBlock = getOpenTimeBlock()
                .orElseThrow(() -> new DomainException("Cannot stop work: no open time block exists for " + id.date()));

        if (!openBlock.getId().equals(timeBlockId)) {
            throw new DomainException("timeBlockId does not match the open time block");
        }
        if (!timestamp.isAfter(openBlock.getStartedAt())) {
            throw new DomainException("End time must be strictly after start time");
        }

        final TimeBlockEnded event = new TimeBlockEnded(id.userId(), id, timeBlockId, timestamp, timezone, projectId, note);
        applyNew(event);
        uncommittedEvents.add(event);
    }

    // ── State queries ────────────────────────────────────────────────────────

    public boolean hasOpenTimeBlock() {
        return timeBlocks.stream().anyMatch(TimeBlock::isOpen);
    }

    public Optional<TimeBlock> getOpenTimeBlock() {
        return timeBlocks.stream().filter(TimeBlock::isOpen).findFirst();
    }

    // ── Event application ────────────────────────────────────────────────────

    private void applyNew(final DomainEvent event) {
        applyState(event);
    }

    private void applyHistory(final DomainEvent event) {
        applyState(event);
        version++;
    }

    private void applyState(final DomainEvent event) {
        if (event instanceof TimeBlockStarted e) {
            timeBlocks.add(new TimeBlock(e.timeBlockId(), e.startedAt(), null, e.projectId()));
        } else if (event instanceof TimeBlockEnded e) {
            timeBlocks.stream()
                    .filter(tb -> tb.getId().equals(e.timeBlockId()))
                    .findFirst()
                    .ifPresent(tb -> tb.close(e.endedAt(), e.projectId()));
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public WorkDayId getId() { return id; }

    public int getVersion() { return version; }

    public List<TimeBlock> getTimeBlocks() { return Collections.unmodifiableList(timeBlocks); }

    public List<DomainEvent> getUncommittedEvents() { return Collections.unmodifiableList(uncommittedEvents); }

    public void markEventsAsCommitted() { uncommittedEvents.clear(); }
}
