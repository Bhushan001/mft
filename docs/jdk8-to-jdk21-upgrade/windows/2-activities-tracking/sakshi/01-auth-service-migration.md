# Task 1 — Auth Service Migration (Windows)

> **Owner:** Sakshi
> **OS:** Windows 10 / Windows 11
> **Sprint:** 2 — Week 2
> **Status:** Pending
> **Module:** `backend\auth-service`

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\auth-service\src

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

# Verify
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.(persistence|validation|servlet|annotation)\."
# Should return nothing
```

---

## Steps 2–6: Java Code Changes

All code changes are **identical to Mac**. See [Mac Task 1](../../../mac/2-activities-tracking/sakshi/01-auth-service-migration.md) for:
- Spring Security 6 config (`SecurityFilterChain` bean)
- JJWT 0.12.x migration (`parser()`, `verifyWith()`, `parseSignedClaims()`, `getPayload()`)
- Redis property `spring.data.redis.*`
- JWT filter `jakarta.servlet` import

---

## Step 7: Build and Test

```powershell
# Build
mvn -pl auth-service clean install

# Run tests
mvn -pl auth-service test

# Expected tests to pass:
# - loginShouldSucceed_whenCredentialsAreValid
# - loginShouldThrowBusinessException_whenAccountIsLocked
# - loginShouldIncrementFailedAttempts_whenPasswordIsWrong
```

---

## Step 8: Smoke Test (PowerShell)

```powershell
# Start Redis + Postgres first
docker compose up -d postgres redis

# Run auth-service
# In a separate PowerShell window:
mvn -pl auth-service spring-boot:run

# Test login
$body = '{"email":"admin@chrono.com","password":"Admin@123"}'
$response = Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8081/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body $body
Write-Host "Access Token: $($response.data.accessToken.Substring(0,30))..."

# Test wrong password
try {
    Invoke-RestMethod `
        -Method POST `
        -Uri "http://localhost:8081/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body '{"email":"admin@chrono.com","password":"wrong"}'
} catch {
    Write-Host "Status: $($_.Exception.Response.StatusCode)"  # Expected: 401
}
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports in auth-service
- [ ] `mvn -pl auth-service clean install` succeeds
- [ ] `WebSecurityConfigurerAdapter` removed
- [ ] JJWT 0.12.x API used
- [ ] Redis property `spring.data.redis.*`
- [ ] All unit tests pass
- [ ] Login smoke test returns JWT

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
