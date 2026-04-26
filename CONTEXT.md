# Worklog – Projektkontext

## Syfte
En personlig tidsloggningsapp. Logga när jag börjar och slutar jobba, fördela tid på projekt, och generera veckorapporter för tidredovisning.

---

## Tech Stack

| Del | Val |
|-----|-----|
| Backend | Spring Boot (Java) → Kotlin-redo |
| Databas | PostgreSQL (Docker lokalt), H2 för integrationstester |
| Frontend | HTML + CSS + JS (REST API) |
| Containerisering | Docker + Docker Compose |
| Deploy (nu) | Mac Mini + Cloudflare Tunnel |
| Deploy (sen) | GCP Cloud Run + Neon PostgreSQL (gratis) |
| Versionshantering | GitHub (guranxp.sandbox@gmail.com) |
| Kodverktyg | Manuellt + Claude Code + Claude Desktop + Dispatch |

---

## Arkitektur & Mönster

- **DDD** – ubiquitous language, aggregat, value objects, domänhändelser
- **CQRS + Event Sourcing** – commands, events, projections, event store (eget, simpelt)
- **Design by Contract (DBC)** – guards/preconditions i domänkoden
- **Dynamic Consistency Boundaries (DCB)** – migration från aggregat senare (AxonIQ-inspirerat, eget impl)
- **Event Modelling** – görs textuellt i Markdown innan varje scenario kodas
- Bygg ett scenario i taget

---

## Funktionalitet

### Kärnfunktion
- Tidsstämplar – stämpla in/ut
- Raster – stämpla ut/in för paus (räknas ej som arbetstid)
- En dag kan ha **flera tidsblock** (start + slut + valfritt projekt + fritext-anteckning)
- Projekt är **valfritt** på ett tidsblock
- Tidsblock kan **redigeras i efterhand** (t.ex. glömt stämpla ut)
- Manuell inmatning av tid

### Projekt
- Id + beskrivning
- Fördefinierade i appen
- Sätts vid stämpling eller i efterhand

### Triggers för instämpling
- Knapp i UI
- WiFi-koppling (fas 2)

### Rapporter
- Valfri period (dag, vecka, månad, custom)
- Tid per projekt per dag
- Format: timmar och minuter
- Påminnelse fredagar kl 16 att tidredovisa

### Integrationer (fas 2)
- Export till Google Sheets
- Outlook-kalender (föreslå projekt baserat på möten)
- Kleer (tidredovisningssystem hos kund – om API finns)
- Internt tidredovisningssystem på jobbet

---

## Kvalitet

- **Unit tester** – domänlogik, aggregat, commands/events
- **Integrationstester** – API + databas (H2)
- **Loggning** – strukturerad, SLF4J + Logback
- **Metrics** – Micrometer + Spring Actuator
- **Dokumentation** – README + Markdown-filer för arkitektur och beslut
- Kontinuerlig push till GitHub

---

## GitHub

- Konto: guranxp.sandbox@gmail.com
- Nytt repo: `worklog` (gamla demo-repot byts namn till `worklog-demo`)
- Klona till Mac Mini, jobba därifrån

---

## Nästa steg

1. ✅ Byt namn på gamla repot till `worklog-demo` på GitHub
2. ✅ Skapa nytt `worklog` repo
3. ✅ Klona till Mac Mini
4. 🔲 Event modelling – scenario 1: stämpla in/ut en dag
5. 🔲 Definiera aggregat
6. 🔲 Börja koda scenario 1
