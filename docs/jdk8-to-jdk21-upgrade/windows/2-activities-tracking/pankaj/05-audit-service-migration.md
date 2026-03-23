# Task 5 — Audit Service Migration (Windows)

> **Owner:** Pankaj | **OS:** Windows | **Sprint:** 4
> **Start here — simplest service, no Redis, no Feign, no Batch**

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\audit-service\src

Get-ChildItem -Path . -Filter *.java -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $updated = $content `
        -replace 'import javax\.persistence\.', 'import jakarta.persistence.' `
        -replace 'import javax\.validation\.', 'import jakarta.validation.' `
        -replace 'import javax\.servlet\.', 'import jakarta.servlet.' `
        -replace 'import javax\.annotation\.', 'import jakarta.annotation.'
    if ($content -ne $updated) {
        Set-Content -Path $_.FullName -Value $updated -NoNewline
    }
}

Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.(persistence|validation|servlet|annotation)\."
# Should be empty
```

## Java Code Changes

Identical to Mac. See [Mac Task 5](../../../mac/2-activities-tracking/pankaj/05-audit-service-migration.md) for:
- Security config (`SecurityFilterChain`)
- `AuditEvent` entity (immutable, no `BaseEntity`)
- Pagination with Spring Data 3.x

---

## Build and Smoke Test

```powershell
mvn -pl audit-service clean install
mvn -pl audit-service test

# Record an audit event
Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8088/api/v1/audit/internal/record" `
    -ContentType "application/json" `
    -Body (@{
        action       = "LOGIN"
        resourceType = "USER"
        resourceId   = "user-001"
        performedBy  = "user-001"
        tenantId     = "tenant-123"
        ipAddress    = "127.0.0.1"
    } | ConvertTo-Json)
# Expected: 204 No Content

# Query audit events
Invoke-RestMethod `
    -Uri "http://localhost:8088/api/v1/audit/events?action=LOGIN&page=0&size=10" `
    -Headers @{ "X-Tenant-Id" = "tenant-123"; "X-User-Role" = "TENANT_ADMIN" }
# Expected: 200 with paginated results
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `mvn -pl audit-service clean install` succeeds
- [ ] Audit recording works (204)
- [ ] Audit querying works with pagination
- [ ] All tests pass

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
