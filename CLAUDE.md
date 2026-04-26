# Worklog

Personal time-tracking app. Log clock-in/out, break stamps, project allocation, and generate weekly reports.

## Tech Stack

- **Backend**: Spring Boot (Java, Kotlin-ready)
- **Database**: PostgreSQL via Docker (prod), H2 (integration tests)
- **Frontend**: HTML + CSS + JS (REST API consumer)
- **Infra**: Docker Compose locally, Mac Mini + Cloudflare Tunnel (now), GCP Cloud Run + Neon PostgreSQL (later)

## Architecture

- **DDD** — ubiquitous language, aggregates, value objects, domain events
- **CQRS + Event Sourcing** — commands, events, projections, custom event store
- **Design by Contract (DBC)** — guards and preconditions in domain code; fail fast on invariant violations
- **Dynamic Consistency Boundaries (DCB)** — AxonIQ-inspired, custom implementation; replaces rigid aggregate boundaries over time

## Test Strategy

- **Unit tests** — domain logic, aggregates, commands/events; no infrastructure
- **Integration tests** — full API + DB stack using H2; cover the happy path and key failure modes
- Logging: SLF4J + Logback (structured). Metrics: Micrometer + Spring Actuator.

## Workflow

1. **Event model first** — write the scenario in Markdown (commands → events → projections) before touching code
2. **One scenario at a time** — complete event model → domain code → tests → push before starting the next
3. Commit and push to GitHub after each scenario is green

## Language

All code and documentation must be written in English.
