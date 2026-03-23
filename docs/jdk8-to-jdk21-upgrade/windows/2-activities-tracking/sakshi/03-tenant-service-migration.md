# Task 3 — Tenant Service Migration (Windows)

> **Owner:** Sakshi
> **OS:** Windows 10 / Windows 11
> **Sprint:** 3 — Week 3
> **Status:** Pending

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\tenant-service\src

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

# Verify
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.(persistence|validation|servlet|annotation)\."
```

## Java Code Changes

Identical to Mac. See [Mac Task 3](../../../mac/2-activities-tracking/sakshi/03-tenant-service-migration.md).

---

## Build and Smoke Test

```powershell
mvn -pl tenant-service clean install
mvn -pl tenant-service test

# Smoke test
Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8083/api/v1/tenants" `
    -ContentType "application/json" `
    -Headers @{ "X-User-Id" = "admin"; "X-User-Role" = "SUPER_ADMIN" } `
    -Body (@{ name = "Acme Corp"; plan = "ENTERPRISE" } | ConvertTo-Json)
# Expected: 201 Created
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `mvn -pl tenant-service clean install` succeeds
- [ ] Tenant CRUD API works
- [ ] All tests pass

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
