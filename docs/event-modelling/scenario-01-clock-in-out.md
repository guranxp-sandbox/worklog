# Scenario 01 – Clock In / Clock Out (single day)

## Ubiquitous Language

| Term | Definition |
|------|------------|
| WorkDay | A calendar day with zero or more time blocks |
| TimeBlock | A contiguous time interval with a start, optional end, and optional project |
| Open time block | A time block without an end time — currently in progress |
| Clock In (ClockIn) | Start a new time block |
| Clock Out (ClockOut) | End the current open time block |
| Project | Optional categorization of a time block (predefined in the app) |

---

## Commands

### ClockIn

| Field | Type | Notes |
|-------|------|-------|
| workDayId | LocalDate | Identifies the work day |
| timeBlockId | UUID | Client-generated for idempotency |
| timestamp | Instant (UTC) | When the clock-in occurred |
| projectId | optional | Assign project at clock-in time |

**Preconditions:**
1. `timestamp ≤ now`
2. `timestamp` belongs to the `workDayId` calendar day
3. No open time block exists for `workDayId`

### ClockOut

| Field | Type | Notes |
|-------|------|-------|
| workDayId | LocalDate | Identifies the work day |
| timeBlockId | UUID | Must match the open time block |
| timestamp | Instant (UTC) | When the clock-out occurred |
| projectId | optional | Override or assign project at clock-out time |
| note | optional String | Free-text annotation for the time block |

**Preconditions:**
1. `timestamp ≤ now`
2. Exactly one open time block exists for `workDayId`
3. `timeBlockId` matches the open time block
4. `timestamp > startedAt`

---

## Events

### TimeBlockStarted

| Field | Type |
|-------|------|
| workDayId | LocalDate |
| timeBlockId | UUID |
| startedAt | Instant (UTC) |
| projectId | optional |

### TimeBlockEnded

| Field | Type |
|-------|------|
| workDayId | LocalDate |
| timeBlockId | UUID |
| endedAt | Instant (UTC) |
| projectId | optional |
| note | optional String |

---

## Read Models

### CurrentDayView *(live UI)*

| Field | Type |
|-------|------|
| date | LocalDate |
| status | `CLOCKED_IN` \| `CLOCKED_OUT` |
| activeTimeBlock | TimeBlock (nullable) |
| completedTimeBlocks | List\<TimeBlock\> |
| totalWorkedMinutes | Int |

Updated by: `TimeBlockStarted`, `TimeBlockEnded`

### DaySummary *(history / reports)*

| Field | Type |
|-------|------|
| date | LocalDate |
| totalWorkedMinutes | Int |
| timeBlocks | List of `{start, end, project, note, minutes}` |

Updated by: `TimeBlockEnded`

---

## Invariants (Design by Contract)

1. Cannot clock in if an open time block already exists — ClockIn precondition
2. Cannot clock out if no open time block exists — ClockOut precondition
3. End time must be after start time — ClockOut precondition
4. Timestamps must not be in the future — both commands
5. ClockIn timestamp must belong to the same calendar day as `workDayId` — ClockIn precondition
6. `timeBlockId` in ClockOut must match the open time block — ClockOut precondition

---

## Aggregate

**WorkDay** (identified by `LocalDate`)

- Holds list of `TimeBlock` entities
- Tracks whether an open time block exists
- Validates all preconditions

> **DCB note:** Consistency boundary is `WorkDay` for this scenario. Migration to Dynamic Consistency Boundaries later if boundaries prove wrong.

---

## Open Questions

1. **Forgotten clock-out** — block next-day clock-in and require manual fix, auto clock-out at midnight, or allow but flag day as incomplete?
2. **Timezone** — store `Instant` (UTC) in event store, timezone as metadata in the command, convert in projection?
3. **Idempotency** — ignore duplicate `ClockIn` events based on `timeBlockId`?
4. **Minimum block length** — allow 0-minute blocks or enforce a minimum?
5. **API design** — `POST /days/{date}/clock-in` + `/clock-out`, or `POST /time-blocks` with an action parameter?
6. **Optimistic locking** — version/sequenceNumber on `WorkDay` to guard against double-clicks?
7. **Single vs multi-user** — include `userId` in `workDayId` in the event store now to ease future multi-user support?
