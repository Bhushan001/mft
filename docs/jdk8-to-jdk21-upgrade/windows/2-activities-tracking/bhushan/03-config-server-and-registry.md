# Task 3 — Config Server & Service Registry (Windows)

> **Owner:** Bhushan Gadekar
> **OS:** Windows 10 / Windows 11
> **Sprint:** 1 — Week 1
> **Status:** Pending

---

## Objective

Upgrade config-server and service-registry. Code and config changes are identical to Mac; only commands differ.

---

## All Code / Config Changes

Identical to Mac. See [Mac Task 3](../../../mac/2-activities-tracking/bhushan/03-config-server-and-registry.md) for:
- `application.yml` changes
- Eureka Spring Boot 3 known issue fix
- Dockerfile updates

---

## Windows Commands

```powershell
# Build config-server
mvn -pl config-server clean package -DskipTests

# Run config-server
mvn -pl config-server spring-boot:run

# Health check (Windows 10+ has curl.exe)
curl.exe http://localhost:8888/actuator/health
# OR PowerShell:
Invoke-RestMethod -Uri "http://localhost:8888/actuator/health"

# Build service-registry
mvn -pl service-registry clean package -DskipTests

# Run service-registry
mvn -pl service-registry spring-boot:run

# Visit Eureka dashboard
Start-Process "http://localhost:8761"
```

---

## Docker on Windows

```powershell
# Ensure Docker Desktop is running
docker info

# Build images
docker build -t config-server:jdk21 -f backend/config-server/Dockerfile .
docker build -t service-registry:jdk21 -f backend/service-registry/Dockerfile .

# Run via docker-compose
docker compose up -d postgres redis
docker compose up -d service-registry config-server
```

---

## Completion Criteria

- [ ] `mvn -pl config-server clean package` succeeds
- [ ] Config server health: `{"status":"UP"}`
- [ ] `mvn -pl service-registry clean package` succeeds
- [ ] Eureka dashboard loads at `http://localhost:8761`
- [ ] Both Dockerfiles updated to JDK 21

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
