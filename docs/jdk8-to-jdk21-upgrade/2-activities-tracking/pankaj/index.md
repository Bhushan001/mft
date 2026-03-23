# Pankaj — Activity Tracker

> **Role:** Developer — Processing Domain
> **Assigned Services:** mapper-service, engine-service, etl-service, orchestrator-service, audit-service
> **Sprint:** 3 + 4

---

## Sub-Pages

| Page | Description |
|---|---|
| [Task 1 — Mapper Service Migration](./01-mapper-service-migration.md) | javax→jakarta, Feign client, rule lifecycle |
| [Task 2 — Engine Service Migration](./02-engine-service-migration.md) | javax→jakarta, Redis idempotency cache, transformation logic |
| [Task 3 — ETL Service Migration](./03-etl-service-migration.md) | **Spring Batch 5** — most complex: Job/Step builder API rewrite |
| [Task 4 — Orchestrator Service Migration](./04-orchestrator-service-migration.md) | javax→jakarta, Feign clients, async saga pattern |
| [Task 5 — Audit Service Migration](./05-audit-service-migration.md) | javax→jakarta, immutable entity, append-only audit trail |

---

## Prerequisites

Before starting, ensure:
- [ ] Bhushan has completed Task 1 (Parent POM) and Task 2 (Commons)
- [ ] `mvn -pl commons clean install` succeeds
- [ ] JDK 21 and Maven 3.9 installed locally
- [ ] `jdk21-migration` branch checked out and up to date

---

## Status Overview

| Task | Status | Completion Date | Notes |
|---|---|---|---|
| Mapper Service | Pending | | |
| Engine Service | Pending | | Redis cache update |
| ETL Service | Pending | | Spring Batch 5 — complex, allocate extra time |
| Orchestrator Service | Pending | | Async Saga pattern |
| Audit Service | Pending | | Simplest — immutable entity only |

---

## Priority Order

Recommended migration order:
1. **Audit Service** first (simplest — no Redis, no Feign, no Batch)
2. **Mapper Service** (moderate — Feign + Redis)
3. **Engine Service** (moderate — Redis idempotency)
4. **Orchestrator Service** (moderate — async + Feign)
5. **ETL Service** last (hardest — Spring Batch 5 rewrite)

---

## Key Reference Pages

- [Generic Guidelines — Spring Boot 3.x Migration](../../1-generic-guidelines/04-spring-boot-3x-migration.md)
- [Generic Guidelines — Dependency Upgrades](../../1-generic-guidelines/03-dependency-upgrades.md) (Spring Batch 5 section)
- [Common Issues — Hibernate 6.x](../../3-common-issues-and-solutions/04-hibernate-6x.md)
