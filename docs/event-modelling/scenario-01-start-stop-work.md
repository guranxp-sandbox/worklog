# Scenario 01 – Start Work / Stop Work (single day)

## Ubiquitous Language

| Term | Definition |
|------|------------|
| WorkDay | A calendar day with zero or more time blocks |
| TimeBlock | A contiguous time interval with a start, optional end, and optional project |
| Open time block | A time block without an end time — currently in progress |
| Start Work (StartWork) | Start a new time block |
| Stop Work (StopWork) | End the current open time block |
| Project | Optional categorization of a time block (predefined in the app) |

---

## Commands

### StartWork

| Field | Type | Notes |
|-------|------|-------|
| userId | UUID | Identifies the user |
| workDayId | userId + LocalDate | Composite aggregate identity |
| timeBlockId | UUID | Identifies the time block being started |
| requestId | UUID | Client-generated idempotency key |
| timestamp | Instant (UTC) | When work started |
| timezone | ZoneId | User's local timezone at the time of the command |
| projectId | optional | Assign project at start-work time |

**Preconditions:**
1. `timestamp ≤ now`
2. `timestamp` belongs to the `workDayId` calendar day (evaluated in `timezone`)
3. No open time block exists for `workDayId`

### StopWork

| Field | Type | Notes |
|-------|------|-------|
| userId | UUID | Identifies the user |
| workDayId | userId + LocalDate | Composite aggregate identity |
| timeBlockId | UUID | Must match the open time block |
| requestId | UUID | Client-generated idempotency key |
| timestamp | Instant (UTC) | When work stopped |
| timezone | ZoneId | User's local timezone at the time of the command |
| projectId | optional | Override or assign project at stop-work time |
| note | optional String | Free-text annotation for the time block |

**Preconditions:**
1. `timestamp ≤ now`
2. Exactly one open time block exists for `workDayId`
3. `timeBlockId` matches the open time block
4. `timestamp > startedAt` (equal timestamps are also rejected — block must be at least 1 second long)

---

## Events

### TimeBlockStarted

| Field | Type |
|-------|------|
| userId | UUID |
| workDayId | userId + LocalDate |
| timeBlockId | UUID |
| startedAt | Instant (UTC) |
| timezone | ZoneId |
| projectId | optional |

### TimeBlockEnded

| Field | Type |
|-------|------|
| userId | UUID |
| workDayId | userId + LocalDate |
| timeBlockId | UUID |
| endedAt | Instant (UTC) |
| timezone | ZoneId |
| projectId | optional |
| note | optional String |

---

## Read Models

### CurrentDayView *(live UI)*

| Field | Type |
|-------|------|
| date | LocalDate (user's local timezone) |
| status | `WORKING` \| `NOT_WORKING` |
| activeTimeBlock | TimeBlock (nullable) |
| completedTimeBlocks | List\<TimeBlock\> |
| totalWorkedMinutes | Int |

Updated by: `TimeBlockStarted`, `TimeBlockEnded`

### DaySummary *(history / reports)*

| Field | Type |
|-------|------|
| date | LocalDate (user's local timezone) |
| totalWorkedMinutes | Int |
| timeBlocks | List of `{start, end, project, note, minutes}` |
| incomplete | Boolean — true if day has no open block but was never properly closed |

Updated by: `TimeBlockEnded`

---

## Invariants (Design by Contract)

1. Cannot start work if an open time block already exists — StartWork precondition
2. Cannot stop work if no open time block exists — StopWork precondition
3. End time must be strictly after start time — equal timestamps are also rejected — StopWork precondition
4. Timestamps must not be in the future — both commands
5. StartWork timestamp must belong to the same calendar day as `workDayId`, evaluated in the command's `timezone` — StartWork precondition
6. `timeBlockId` in StopWork must match the open time block — StopWork precondition

---

## Aggregate

**WorkDay** (identified by `userId + LocalDate`)

- Holds list of `TimeBlock` entities
- Tracks whether an open time block exists
- Carries a `sequenceNumber` (version) for optimistic locking — incremented on every state change
- Validates all preconditions

> **DCB note:** Consistency boundary is `WorkDay` for this scenario. Migration to Dynamic Consistency Boundaries later if boundaries prove wrong.

---

## Decisions

1. **Forgotten stop-work** — Allow but flag the day as incomplete. No blocking of subsequent days. The `DaySummary` read model exposes an `incomplete` flag for UI and report warnings.

2. **Timezone** — Commands include the user's `timezone` (ZoneId) as metadata. Events store `Instant` (UTC). Projections convert to local time using the timezone from the event. Must work correctly across timezone changes and DST transitions.

3. **Idempotency** — Use `requestId` (UUID, client-generated) as the idempotency key in the event store. `timeBlockId` identifies the domain entity; `requestId` guards against duplicate command submissions.

4. **Minimum block length** — A time block must be strictly longer than 0 minutes. End time must be after start time; equal timestamps are also rejected. Already covered by invariant 3 — clarified here for explicitness.

5. **API design** — `POST /days/{date}/start-work` and `POST /days/{date}/stop-work`. Most RESTful for this domain; the day is the primary resource.

6. **Optimistic locking** — `WorkDay` carries a `sequenceNumber` (version). Commands include the expected version; the handler rejects the command if the actual version differs. Guards against double-clicks and concurrent requests.

7. **Multi-user** — `workDayId` is a composite of `userId + LocalDate` from the start. Aggregate identity, events, and read models all include `userId`. No migration needed when multi-user support is added.
