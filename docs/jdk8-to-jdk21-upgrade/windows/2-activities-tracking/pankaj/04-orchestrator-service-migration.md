# Task 4 — Orchestrator Service Migration (Windows)

> **Owner:** Pankaj | **OS:** Windows | **Sprint:** 4

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\orchestrator-service\src

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

Identical to Mac. See [Mac Task 4](../../../mac/2-activities-tracking/pankaj/04-orchestrator-service-migration.md).

---

## Build and Smoke Test

```powershell
mvn -pl orchestrator-service clean install
mvn -pl orchestrator-service test

# Submit workflow
$response = Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8086/api/v1/workflows" `
    -ContentType "application/json" `
    -Headers @{ "X-Tenant-Id" = "tenant-123"; "X-User-Id" = "user-001" } `
    -Body (@{ type = "ETL_ORCHESTRATION"; correlationId = "corr-001" } | ConvertTo-Json)
# Expected: 202 Accepted

$workflowId = $response.data.workflowId
Invoke-RestMethod `
    -Uri "http://localhost:8086/api/v1/workflows/$workflowId" `
    -Headers @{ "X-Tenant-Id" = "tenant-123" }
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports | [ ] Build succeeds | [ ] 202 returned | [ ] Tests pass

## Notes / Observations

| Date | Observation |
|---|---|
| | |
