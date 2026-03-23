# Task 2 — Engine Service Migration (Windows)

> **Owner:** Pankaj | **OS:** Windows | **Sprint:** 3

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\engine-service\src

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
```

## Java Code Changes

Identical to Mac. See [Mac Task 2](../../../mac/2-activities-tracking/pankaj/02-engine-service-migration.md) for:
- Redis `spring.data.redis.*`
- Idempotency logic (no code change, only property)
- Security config

---

## Build and Smoke Test

```powershell
mvn -pl engine-service clean install
mvn -pl engine-service test

# First call
Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8085/api/v1/engine/process" `
    -ContentType "application/json" `
    -Headers @{ "X-Tenant-Id" = "tenant-123"; "X-User-Role" = "TENANT_USER" } `
    -Body (@{ idempotencyKey = "key-001"; data = @{ input = "test" } } | ConvertTo-Json)

# Second call with same key — should return cached response
Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8085/api/v1/engine/process" `
    -ContentType "application/json" `
    -Headers @{ "X-Tenant-Id" = "tenant-123"; "X-User-Role" = "TENANT_USER" } `
    -Body (@{ idempotencyKey = "key-001"; data = @{ input = "test" } } | ConvertTo-Json)
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports | [ ] Build succeeds | [ ] Idempotency works | [ ] Tests pass

## Notes / Observations

| Date | Observation |
|---|---|
| | |
