# JDK 8 → JDK 21 Migration: Master Index

> **Confluence Space:** Chrono Platform / JDK Migration
> **Project Lead:** Bhushan Gadekar
> **Team:** Bhushan, Sakshi, Pankaj
> **Target Version:** JDK 21 (LTS) + Spring Boot 3.3.x + Spring Cloud 2023.x
> **Status:** In Progress

---

## Purpose

This space documents the end-to-end migration of the Chrono microservices platform from **JDK 8 + Spring Boot 2.7.x** to **JDK 21 + Spring Boot 3.3.x**. It serves as the reference guide for applying the same migration to similar projects.

---

## Document Structure

| Section | Purpose |
|---|---|
| [1. Generic Guidelines](./1-generic-guidelines/index.md) | Step-by-step technical upgrade instructions applicable to any Spring Boot project |
| [2. Activities Tracking](./2-activities-tracking/index.md) | Task assignments and progress tracking per team member |
| [3. Common Issues and Solutions](./3-common-issues-and-solutions/index.md) | Real issues encountered during migration and their resolutions |

---

## Migration Scope

| Component | From | To |
|---|---|---|
| JDK | 8 | 21 (LTS) |
| Spring Boot | 2.7.18 | 3.3.x |
| Spring Cloud | 2021.0.9 | 2023.0.x |
| Spring Security | 5.x | 6.x |
| Hibernate / Spring Data JPA | 5.x | 6.x |
| Jakarta EE | javax.* | jakarta.* |
| Liquibase | 4.x | 4.27.x |
| Resilience4j | 1.7.x | 2.x |
| JJWT | 0.11.5 | 0.12.x |
| Lombok | 1.18.30 | 1.18.32 |
| MapStruct | 1.5.5 | 1.6.x |

---

## Key Milestones

| # | Milestone | Owner | Status |
|---|---|---|---|
| 1 | Environment setup (JDK 21 + Maven toolchain) | Bhushan | Pending |
| 2 | Parent POM & commons migration | Bhushan | Pending |
| 3 | API Gateway migration | Bhushan | Pending |
| 4 | Auth Service migration | Sakshi | Pending |
| 5 | User Service migration | Sakshi | Pending |
| 6 | Tenant Service migration | Sakshi | Pending |
| 7 | Mapper Service migration | Pankaj | Pending |
| 8 | Engine Service migration | Pankaj | Pending |
| 9 | ETL Service migration | Pankaj | Pending |
| 10 | Orchestrator Service migration | Pankaj | Pending |
| 11 | Audit Service migration | Pankaj | Pending |
| 12 | Integration testing & sign-off | All | Pending |

---

## Quick Reference: Breaking Changes Summary

1. **`javax.*` → `jakarta.*`** — All imports across every service must be updated
2. **Spring Security 6** — `WebSecurityConfigurerAdapter` removed; `SecurityFilterChain` bean required
3. **Spring Boot 3 actuator paths** — `/actuator/health` format changes
4. **Hibernate 6** — `@Type` annotations and HQL syntax changes
5. **Spring Cloud 2023** — Config import syntax and bootstrap changes
6. **Removed APIs** — `SecurityManager`, `RMISecurityManager`, Nashorn JS engine removed in JDK 15+

---

## How to Use This Space

- **New to migration?** Start with [Generic Guidelines → Overview & Planning](./1-generic-guidelines/01-overview-and-planning.md)
- **Assigned a service?** Go directly to your [Activities Tracking page](./2-activities-tracking/index.md)
- **Hit an error?** Check [Common Issues and Solutions](./3-common-issues-and-solutions/index.md) first
