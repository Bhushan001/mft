# Task 5 — Integration Sign-off (Windows)

> **Owner:** Bhushan Gadekar
> **OS:** Windows 10 / Windows 11
> **Sprint:** 5 — Week 5
> **Status:** Pending

---

## Objective

Full end-to-end validation via Docker Compose on Windows. All smoke tests use PowerShell.

---

## Step 1: Full Docker Compose Startup

```powershell
# Ensure Docker Desktop is running
docker info

# Navigate to project root
Set-Location C:\path\to\chrono

# Build all images
docker compose build --no-cache

# Start infrastructure
docker compose up -d postgres redis zipkin

# Wait and check health
Start-Sleep -Seconds 15
docker compose ps

# Start platform services
docker compose up -d service-registry config-server
Start-Sleep -Seconds 20

# Start all application services
docker compose up -d

# Verify all healthy
docker compose ps
```

---

## Step 2: Smoke Tests (PowerShell)

```powershell
$BASE = "http://localhost:8080/api/v1"

# Helper function
function Invoke-Api($method, $path, $body = $null, $headers = @{}) {
    $params = @{
        Method      = $method
        Uri         = "$BASE$path"
        ContentType = "application/json"
        Headers     = $headers
    }
    if ($body) { $params.Body = ($body | ConvertTo-Json) }
    Invoke-RestMethod @params
}

# 1. Register tenant
$tenant = Invoke-Api "POST" "/tenants" @{ name = "Test Tenant"; plan = "BASIC" }
$tenantId = $tenant.data.id
Write-Host "Tenant: $tenantId"

# 2. Register user
Invoke-Api "POST" "/users" @{
    email    = "test@tenant.com"
    password = "Test@123"
    tenantId = $tenantId
    role     = "TENANT_ADMIN"
}

# 3. Login
$login = Invoke-Api "POST" "/auth/login" @{
    email    = "test@tenant.com"
    password = "Test@123"
}
$token = $login.data.accessToken
Write-Host "Token acquired: $($token.Substring(0,20))..."

# 4. Get user profile
$authHeader = @{ Authorization = "Bearer $token" }
Invoke-Api "GET" "/users/me" -headers $authHeader

# 5. Submit ETL job
Invoke-Api "POST" "/etl/jobs/submit" @{
    sourceRef = "batch-001"
    batchDate = "2024-01-01"
} -headers $authHeader

# 6. Check audit trail
Invoke-Api "GET" "/audit/events?action=LOGIN" -headers $authHeader
```

---

## Step 3: Performance Baseline

```powershell
# Install k6 (load testing tool for Windows)
winget install k6 --source winget
# OR: choco install k6 -y

# Simple load test script (save as load-test.js)
@"
import http from 'k6/http';
import { sleep } from 'k6';

export let options = { vus: 10, duration: '10s' };

export default function() {
    http.post('http://localhost:8080/api/v1/auth/login',
        JSON.stringify({ email: 'test@tenant.com', password: 'Test@123' }),
        { headers: { 'Content-Type': 'application/json' } });
    sleep(1);
}
"@ | Out-File load-test.js

k6 run load-test.js
```

| Endpoint | JDK 8 (req/s) | JDK 21 (req/s) | Improvement |
|---|---|---|---|
| POST /auth/login | *(fill in)* | *(fill in)* | |
| GET /users/me | *(fill in)* | *(fill in)* | |

---

## Step 4: Final Checklist

Same as Mac — see [Mac Task 5](../../../mac/2-activities-tracking/bhushan/05-integration-signoff.md) for the complete checklist.

---

## Step 5: Tag and Push

```powershell
git tag jdk21-migration-complete
git push origin jdk21-migration-complete
```

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
