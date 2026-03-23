# 07 - Runtime and Startup Errors (Windows)

> Most issues are **identical to Mac**. See [Mac version](../../mac/3-common-issues-and-solutions/07-runtime-and-startup-errors.md) for full details.
> Windows-specific issues are listed below.

---

## Issue 1: Circular Dependency / `BeanCreationException`

Same as Mac. Fix: break cycle or set `spring.main.allow-circular-references: true` temporarily.

---

## Issue 2: Redis Connection Refused

Same fix as Mac — `spring.redis.*` → `spring.data.redis.*`.

**Windows: Verify Redis is running in Docker:**
```powershell
docker ps --filter "name=redis"
# Status should be: Up (healthy)

# Test Redis connectivity
docker exec -it chrono-redis-1 redis-cli ping
# Expected: PONG
```

---

## Issue 3: Liquibase Fails — `object already exists`

Same fix as Mac.

**Windows PowerShell equivalent:**
```powershell
# Run Liquibase changelogSync
mvn -pl auth-service liquibase:changelogSync `
    "-Dliquibase.url=jdbc:postgresql://localhost:5432/chrono_db?currentSchema=chrono_auth" `
    "-Dliquibase.username=postgres" `
    "-Dliquibase.password=postgres"
```

---

## Issue 4: `NoSuchMethodError` — JAR Version Conflict

Same fix as Mac. **Windows: Check dependency tree:**
```powershell
mvn -pl auth-service dependency:tree -Dincludes=io.jsonwebtoken 2>&1 |
    Select-String "jsonwebtoken"
```

---

## Issue 5: `JAVA_HOME` Pointing to JDK 8 at Runtime

**Windows-Specific:** Even after setting `JAVA_HOME` in System Properties, the running service may pick up the old JDK if launched from an old shell session or IDE with cached settings.

**Fix:**
```powershell
# Verify in current session
$env:JAVA_HOME
java -version

# If wrong in IntelliJ: File → Project Structure → SDK → set to JDK 21
# If wrong in VS Code: set "java.home" in settings.json
```

---

## Issue 6: Docker Desktop Not Starting Services

**Windows-Specific:** Docker Desktop on Windows requires Hyper-V or WSL2 backend.

**Fix:**
```powershell
# Check Docker backend
docker info | Select-String "OS/Arch|Server Version"

# If WSL2 errors:
wsl --update
wsl --set-default-version 2

# Restart Docker Desktop from system tray
```

---

## Issue 7: Port Already in Use

**Error:**
```
Port 8080 is already in use
```

**Windows: Find and kill the process using the port:**
```powershell
# Find what's using port 8080
netstat -ano | Select-String ":8080"

# Get the PID from the last column (e.g., 12345)
# Kill it:
Stop-Process -Id 12345 -Force

# OR find by process name
Get-Process | Where-Object { $_.Id -eq 12345 } | Select-Object Name, Id
```

---

## Issue 8: `HikariPool` Connection Exhaustion

Same fix as Mac — increase pool size:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

**Windows: Monitor pool via Actuator:**
```powershell
Invoke-RestMethod "http://localhost:8081/actuator/metrics/hikaricp.connections.active"
```

---

## Issue 9: `ConfigDataLocationNotFoundException` (Windows Docker)

**Windows-Specific:** When running services locally (not in Docker), `config-server` is at `localhost:8888`. When inside Docker, it's `config-server:8888`. Mixing these causes failure.

**Fix:** Use profiles to differentiate:
```yaml
# application.yml (local dev — outside Docker)
spring:
  config:
    import: "optional:configserver:http://localhost:8888"

# application-docker.yml (inside Docker)
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
```

Activate Docker profile in `docker-compose.yml`:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=dev,docker
```
