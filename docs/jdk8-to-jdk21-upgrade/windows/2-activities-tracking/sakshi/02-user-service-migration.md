# Task 2 — User Service Migration (Windows)

> **Owner:** Sakshi
> **OS:** Windows 10 / Windows 11
> **Sprint:** 3 — Week 3
> **Status:** Pending

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\user-service\src

Get-ChildItem -Path . -Filter *.java -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $updated = $content `
        -replace 'import javax\.persistence\.', 'import jakarta.persistence.' `
        -replace 'import javax\.validation\.', 'import jakarta.validation.' `
        -replace 'import javax\.servlet\.', 'import jakarta.servlet.' `
        -replace 'import javax\.annotation\.', 'import jakarta.annotation.'
    if ($content -ne $updated) {
        Set-Content -Path $_.FullName -Value $updated -NoNewline
        Write-Host "Updated: $($_.FullName)"
    }
}
```

## Steps 2–5: Java Code Changes

Identical to Mac. See [Mac Task 2](../../../mac/2-activities-tracking/sakshi/02-user-service-migration.md) for:
- `SecurityFilterChain` config
- Feign client (no change)
- Redis `spring.data.redis.*`
- `UserProfile` entity jakarta imports

---

## Build and Smoke Test

```powershell
mvn -pl user-service clean install
mvn -pl user-service test

# Smoke test
$headers = @{
    "X-User-Id"   = "admin"
    "X-Tenant-Id" = "tenant-123"
    "X-User-Role" = "SUPER_ADMIN"
}
Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8082/api/v1/users" `
    -ContentType "application/json" `
    -Headers $headers `
    -Body (@{
        email     = "newuser@test.com"
        firstName = "Test"
        lastName  = "User"
        tenantId  = "tenant-123"
        role      = "TENANT_USER"
    } | ConvertTo-Json)
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `mvn -pl user-service clean install` succeeds
- [ ] Feign call to auth-service works
- [ ] All unit tests pass

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
