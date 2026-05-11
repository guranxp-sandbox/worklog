package com.worklog.domain.workday;

import com.worklog.domain.DomainEvent;
import com.worklog.domain.Guard;
import com.worklog.domain.workday.events.TimeBlockEnded;
import com.worklog.domain.workday.events.TimeBlockStarted;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

    private WorkDay(WorkDayId id) {
        this.id = id;
    }

    // ── Factory methods ──────────────────────────────────────────────────────

    public static WorkDay empty(WorkDayId id) {
        return new WorkDay(id);
    }

    public static WorkDay reconstitute(WorkDayId id, List<DomainEvent> history) {
        WorkDay wd = new WorkDay(id);
        for (DomainEvent event : history) {
            wd.applyHistory(event);
        }
        return wd;
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    public void startWork(UUID timeBlockId, Instant timestamp, ZoneId timezone, UUID projectId, Instant now) {
        Guard.require(!timestamp.isAfter(now),
                "Timestamp must not be in the future");

        LocalDate localDate = timestamp.atZone(timezone).toLocalDate();
        Guard.require(localDate.equals(id.date()),
                "Timestamp " + timestamp + " does not belong to work day " + id.date() + " in timezone " + timezone);

        Guard.require(!hasOpenTimeBlock(),
                "Cannot start work: an open time block already exists for " + id.date());

        TimeBlockStarted event = new TimeBlockStarted(id.userId(), id, timeBlockId, timestamp, timezone, projectId);
        applyNew(event);
        uncommittedEvents.add(event);
    }

    public void stopWork(UUID timeBlockId, Instant timestamp, ZoneId timezone, UUID projectId, String note, Instant now) {
        Guard.require(!timestamp.isAfter(now),
                "Timestamp must not be in the future");

        TimeBlock openBlock = timeBlocks.stream()
                .filter(TimeBlock::isOpen)
                .findFirst()
                .orElseThrow(() -> new com.worklog.domain.DomainException(
                        "Cannot stop work: no open time block exists for " + id.date()));

        Guard.require(openBlock.getId().equals(timeBlockId),
                "timeBlockId does not match the open time block");

        Guard.require(timestamp.isAfter(openBlock.getStartedAt()),
                "End time must be strictly after start time");

        TimeBlockEnded event = new TimeBlockEnded(id.userId(), id, timeBlockId, timestamp, timezone, projectId, note);
        applyNew(event);
        uncommittedEvents.add(event);
    }

    // ── State queries ────────────────────────────────────────────────────────

    public boolean hasOpenTimeBlock() {
        return timeBlocks.stream().anyMatch(TimeBlock::isOpen);
    }

    // ── Event application ────────────────────────────────────────────────────

    private void applyNew(DomainEvent event) {
        applyState(event);
        // version is NOT incremented here; it reflects committed state only
    }

    private void applyHistory(DomainEvent event) {
        applyState(event);
        version++;
    }

    private void applyState(DomainEvent event) {
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
