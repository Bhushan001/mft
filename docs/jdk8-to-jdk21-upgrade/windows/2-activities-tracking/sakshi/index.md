# Sakshi — Activity Tracker (Windows)

> **OS:** Windows 10 / Windows 11 | **Shell:** PowerShell 7+

| Page | Description |
|---|---|
| [Task 1 — Auth Service](./01-auth-service-migration.md) | Security 6, JJWT 0.12, Redis — with PowerShell commands |
| [Task 2 — User Service](./02-user-service-migration.md) | javax→jakarta, Feign, Redis cache |
| [Task 3 — Tenant Service](./03-tenant-service-migration.md) | javax→jakarta, minimal changes |

**Prerequisites:** Bhushan's Task 1 (Parent POM) complete. `mvn -pl commons clean install` succeeds.

| Task | Status | Notes |
|---|---|---|
| Auth Service | Pending | JJWT 0.12 + Security 6 |
| User Service | Pending | |
| Tenant Service | Pending | Simplest |
