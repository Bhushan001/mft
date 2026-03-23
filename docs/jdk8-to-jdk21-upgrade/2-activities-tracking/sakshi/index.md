# Sakshi — Activity Tracker

> **Role:** Developer — Auth & User Domain
> **Assigned Services:** auth-service, user-service, tenant-service
> **Sprint:** 2 + 3

---

## Sub-Pages

| Page | Description |
|---|---|
| [Task 1 — Auth Service Migration](./01-auth-service-migration.md) | Spring Security 6, JJWT 0.12.x, BCrypt, Redis blacklist |
| [Task 2 — User Service Migration](./02-user-service-migration.md) | javax→jakarta, Feign client to auth-service, Redis cache |
| [Task 3 — Tenant Service Migration](./03-tenant-service-migration.md) | javax→jakarta, tenant entity, plan management |

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
| Auth Service | Pending | | Most complex: Security 6 + JJWT 0.12 |
| User Service | Pending | | |
| Tenant Service | Pending | | Simplest of the three |

---

## Key Reference Pages

- [Generic Guidelines — Security Migration](../../1-generic-guidelines/06-security-migration.md)
- [Generic Guidelines — Dependency Upgrades](../../1-generic-guidelines/03-dependency-upgrades.md) (JJWT 0.12 section)
- [Common Issues — Spring Security 6](../../3-common-issues-and-solutions/03-spring-security-6x.md)
