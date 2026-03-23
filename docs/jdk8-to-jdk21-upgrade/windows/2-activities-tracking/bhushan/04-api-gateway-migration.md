# Task 4 — API Gateway Migration (Windows)

> **Owner:** Bhushan Gadekar
> **OS:** Windows 10 / Windows 11
> **Sprint:** 2 — Week 2
> **Status:** Pending
> **Module:** `backend\api-gateway`

---

## Objective

Migrate the api-gateway. All Java code changes (WebFlux security, JJWT 0.12, Redis reactive) are identical to Mac.

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\api-gateway\src

Get-ChildItem -Path . -Filter *.java -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $updated = $content `
        -replace 'import javax\.servlet\.', 'import jakarta.servlet.' `
        -replace 'import javax\.annotation\.', 'import jakarta.annotation.'
    if ($content -ne $updated) {
        Set-Content -Path $_.FullName -Value $updated -NoNewline
        Write-Host "Updated: $($_.FullName)"
    }
}

# Verify
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\."
```

---

## Step 2–7: All Code Changes

Identical to Mac. See [Mac Task 4](../../../mac/2-activities-tracking/bhushan/04-api-gateway-migration.md) for:
- Spring Security 6 WebFlux config
- JJWT 0.12.x parser migration (`parser()`, `verifyWith()`, `parseSignedClaims()`, `getPayload()`)
- Redis property `spring.data.redis.*`
- Spring Cloud Gateway route config
- Properties migrator

---

## Step 8: End-to-End JWT Flow Test (Windows)

```powershell
# Option A: curl.exe (Windows 10+)
curl.exe -X POST http://localhost:8080/api/v1/auth/login `
    -H "Content-Type: application/json" `
    -d '{\"email\":\"admin@chrono.com\",\"password\":\"Admin@123\"}'

# Option B: PowerShell Invoke-RestMethod (cleaner)
$body = @{
    email    = "admin@chrono.com"
    password = "Admin@123"
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8080/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body $body

$token = $response.data.accessToken
Write-Host "Token: $token"

# Test protected endpoint
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/users" `
    -Headers @{ Authorization = "Bearer $token" }

# Test invalid token (expect 401)
try {
    Invoke-RestMethod `
        -Uri "http://localhost:8080/api/v1/users" `
        -Headers @{ Authorization = "Bearer invalid.token.here" }
} catch {
    Write-Host "Status: $($_.Exception.Response.StatusCode)"  # Expected: Unauthorized (401)
}
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `mvn -pl api-gateway clean package` succeeds
- [ ] Gateway starts, `/actuator/health` = UP
- [ ] Login via PowerShell returns JWT
- [ ] Valid JWT routes to downstream service
- [ ] Invalid JWT returns 401

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
