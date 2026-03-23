# Task 1 — Mapper Service Migration (Windows)

> **Owner:** Pankaj | **OS:** Windows | **Sprint:** 3

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\mapper-service\src

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

Identical to Mac. See [Mac Task 1](../../../mac/2-activities-tracking/pankaj/01-mapper-service-migration.md) for:
- `SecurityFilterChain` config
- Redis `spring.data.redis.*`
- Resilience4j 2.x dependency change

---

## Build and Smoke Test

```powershell
mvn -pl mapper-service clean install
mvn -pl mapper-service test

Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8084/api/v1/mapping-rules" `
    -ContentType "application/json" `
    -Headers @{ "X-Tenant-Id" = "tenant-123"; "X-User-Id" = "user-001"; "X-User-Role" = "TENANT_ADMIN" } `
    -Body (@{ name = "Field Mapper"; sourceField = "input.name"; targetField = "output.fullName" } | ConvertTo-Json)
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports | [ ] Build succeeds | [ ] Tests pass

## Notes / Observations

| Date | Observation |
|---|---|
| | |
