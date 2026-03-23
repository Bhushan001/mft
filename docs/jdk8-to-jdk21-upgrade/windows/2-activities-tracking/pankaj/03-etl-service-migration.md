# Task 3 — ETL Service Migration (Windows)

> **Owner:** Pankaj | **OS:** Windows | **Sprint:** 4
> **Complexity:** HIGH — Spring Batch 5 major breaking changes

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\etl-service\src

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

---

## Steps 2–8: Spring Batch 5 Rewrite

All Java code changes are **identical to Mac**. See [Mac Task 3](../../../mac/2-activities-tracking/pankaj/03-etl-service-migration.md) for:

- `JobBuilderFactory` → `new JobBuilder(name, jobRepository)`
- `StepBuilderFactory` → `new StepBuilder(name, jobRepository)`
- `chunk(100)` → `chunk(100, transactionManager)`
- `@EnableBatchProcessing` removal
- `SimpleJobLauncher` → `TaskExecutorJobLauncher`
- Spring Batch 5 schema changes
- `JobParameters` builder (`addDate` → `addLocalDate`)

---

## Build and Smoke Test

```powershell
mvn -pl etl-service clean install
mvn -pl etl-service test

# Submit job
$response = Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8087/api/v1/etl/jobs/submit" `
    -ContentType "application/json" `
    -Headers @{ "X-Tenant-Id" = "tenant-123"; "X-User-Id" = "user-001" } `
    -Body (@{ sourceRef = "batch-001"; batchDate = "2024-01-01" } | ConvertTo-Json)

$jobId = $response.data.jobId
Write-Host "Job ID: $jobId"

# Poll status
do {
    Start-Sleep -Seconds 2
    $status = Invoke-RestMethod `
        -Uri "http://localhost:8087/api/v1/etl/jobs/$jobId" `
        -Headers @{ "X-Tenant-Id" = "tenant-123" }
    Write-Host "Status: $($status.data.status)"
} while ($status.data.status -eq "RUNNING")
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `JobBuilderFactory` / `StepBuilderFactory` removed
- [ ] `chunk(size, transactionManager)` used
- [ ] `mvn -pl etl-service clean install` succeeds
- [ ] Job submits and completes successfully

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
