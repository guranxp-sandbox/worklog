# Worklog – Project Context

## Purpose

A personal time-tracking app. Log when I start and stop working, allocate time to projects, and generate weekly reports for time reporting.

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Backend | Spring Boot (Java) → Kotlin-ready |
| Database | PostgreSQL (Docker locally), H2 for integration tests |
| Frontend | HTML + CSS + JS (REST API) |
| Containerisation | Docker + Docker Compose |
| Deploy (now) | Mac Mini + Cloudflare Tunnel |
| Deploy (later) | GCP Cloud Run + Neon PostgreSQL (free tier) |
| Version control | GitHub (guranxp.sandbox@gmail.com) |
| Tooling | Manual + Claude Code + Claude Desktop + Dispatch |

---

## Architecture & Patterns

- **DDD** — ubiquitous language, aggregates, value objects, domain events
- **CQRS + Event Sourcing** — commands, events, projections, custom event store (simple)
- **Design by Contract (DBC)** — guards/preconditions in domain code
- **Dynamic Consistency Boundaries (DCB)** — migrate from aggregates later (AxonIQ-inspired, custom implementation)
- **Event Modelling** — written in Markdown before each scenario is coded
- Build one scenario at a time

---

## Functionality

### Core

- Timestamps — clock in / clock out
- Breaks — clock out / clock in for a break (not counted as work time)
- A day can have **multiple time blocks** (start + end + optional project + free-text note)
- Project is **optional** on a time block
- Time blocks can be **edited after the fact** (e.g. forgot to clock out)
- Manual time entry

### Projects

- ID + description
- Predefined in the app
- Assigned at clock-in or after the fact

### Clock-in Triggers

- Button in the UI
- WiFi connection (phase 2)

### Reports

- Arbitrary period (day, week, month, custom)
- Time per project per day
- Format: hours and minutes
- Reminder every Friday at 16:00 to submit time report

### Integrations (phase 2)

- Export to Google Sheets
- Outlook calendar (suggest project based on meetings)
- Kleer (customer time-reporting system — if API exists)
- Internal time-reporting system at work

---

## Quality

- **Unit tests** — domain logic, aggregates, commands/events
- **Integration tests** — API + database (H2)
- **Logging** — structured, SLF4J + Logback
- **Metrics** — Micrometer + Spring Actuator
- **Documentation** — README + Markdown files for architecture and decisions
- Continuous push to GitHub

---

## GitHub

- Account: guranxp.sandbox@gmail.com
- New repo: `worklog` (old demo repo renamed to `worklog-demo`)
- Cloned to Mac Mini, work from there

---

## Next Steps

1. ✅ Rename old repo to `worklog-demo` on GitHub
2. ✅ Create new `worklog` repo
3. ✅ Clone to Mac Mini
4. ✅ Event modelling — scenario 1: clock in / clock out for a day
5. 🔲 Define aggregates
6. 🔲 Start coding scenario 1
