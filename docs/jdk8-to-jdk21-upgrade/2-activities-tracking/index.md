# Activities Tracking: JDK 8 → JDK 21

> **Confluence Parent Page:** JDK Migration / Activities Tracking
> **Team:** Bhushan Gadekar (Lead), Sakshi, Pankaj

---

## Team Assignment Overview

| Team Member | Assigned Services | Phase |
|---|---|---|
| [Bhushan](./bhushan/index.md) | commons, parent-pom, config-server, service-registry, api-gateway, integration sign-off | Phase 1 + 2 + 5 |
| [Sakshi](./sakshi/index.md) | auth-service, user-service, tenant-service | Phase 2 + 3 |
| [Pankaj](./pankaj/index.md) | mapper-service, engine-service, etl-service, orchestrator-service, audit-service | Phase 3 + 4 |

---

## Progress Summary

| Service | Owner | Status | Notes |
|---|---|---|---|
| commons | Bhushan | Pending | |
| parent-pom | Bhushan | Pending | |
| config-server | Bhushan | Pending | |
| service-registry | Bhushan | Pending | |
| api-gateway | Bhushan | Pending | WebFlux security changes |
| auth-service | Sakshi | Pending | JJWT 0.12 + Security 6 |
| user-service | Sakshi | Pending | |
| tenant-service | Sakshi | Pending | |
| mapper-service | Pankaj | Pending | |
| engine-service | Pankaj | Pending | Redis cache updates |
| etl-service | Pankaj | Pending | Spring Batch 5 — complex |
| orchestrator-service | Pankaj | Pending | |
| audit-service | Pankaj | Pending | |
| Integration sign-off | Bhushan | Pending | Full docker-compose test |

---

## Sprint Plan

| Sprint | Duration | Goals |
|---|---|---|
| Sprint 1 | Week 1 | Environment setup, parent POM, commons, config/registry |
| Sprint 2 | Week 2 | api-gateway, auth-service — end-to-end login working |
| Sprint 3 | Week 3 | user-service, tenant-service, mapper-service, engine-service |
| Sprint 4 | Week 4 | etl-service, orchestrator-service, audit-service |
| Sprint 5 | Week 5 | Full integration testing, performance baseline, docs |

---

## Definition of Done (per service)

A service is considered migrated when:

1. `mvn -pl {service} clean install` passes with JDK 21
2. Service starts with `spring.profiles.active=dev`
3. `/actuator/health` returns `{"status":"UP"}`
4. All existing unit tests pass
5. End-to-end API call succeeds (via Postman or curl)
6. No `ClassNotFoundException` or `javax.*` imports remain
7. Activity tracking page updated with completion status
