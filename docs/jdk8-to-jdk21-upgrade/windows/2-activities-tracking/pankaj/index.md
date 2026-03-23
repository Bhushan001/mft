# Pankaj — Activity Tracker (Windows)

> **OS:** Windows 10 / Windows 11 | **Shell:** PowerShell 7+

| Page | Description |
|---|---|
| [Task 1 — Mapper Service](./01-mapper-service-migration.md) | javax→jakarta, Resilience4j 2.x |
| [Task 2 — Engine Service](./02-engine-service-migration.md) | javax→jakarta, Redis idempotency |
| [Task 3 — ETL Service](./03-etl-service-migration.md) | **Spring Batch 5** — job/step builder rewrite |
| [Task 4 — Orchestrator Service](./04-orchestrator-service-migration.md) | Async saga, Feign clients |
| [Task 5 — Audit Service](./05-audit-service-migration.md) | Simplest — immutable entity |

**Recommended order:** Audit → Mapper → Engine → Orchestrator → ETL (hardest last)

| Task | Status | Notes |
|---|---|---|
| Mapper Service | Pending | |
| Engine Service | Pending | |
| ETL Service | Pending | Spring Batch 5 — allocate extra time |
| Orchestrator Service | Pending | |
| Audit Service | Pending | Start here |
