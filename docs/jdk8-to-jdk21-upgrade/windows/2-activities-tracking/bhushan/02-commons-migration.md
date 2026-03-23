# Task 2 — Commons Migration (Windows)

> **Owner:** Bhushan Gadekar
> **OS:** Windows 10 / Windows 11
> **Sprint:** 1 — Week 1
> **Status:** Pending
> **Module:** `backend\commons`

---

## Objective

Migrate `commons` — the shared library. All code changes are identical to Mac; only shell commands differ.

---

## Step 1: Replace `javax.*` → `jakarta.*` (PowerShell)

```powershell
Set-Location C:\path\to\chrono\backend\commons\src

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

# Verify zero remaining
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.(persistence|validation|servlet|annotation)\."
# Should return nothing
```

---

## Step 2–6: Code Changes

All Java code changes are **identical to Mac**. See [Mac Task 2](../../../mac/2-activities-tracking/bhushan/02-commons-migration.md) for:
- `BaseEntity` auditing verification
- `GlobalExceptionHandler` — `HttpStatus` → `HttpStatusCode` fix
- `AuditorAwareImpl` — jakarta.servlet import

---

## Step 7: Build and Verify

```powershell
# Build commons
mvn -pl commons clean install -DskipTests

# Run tests
mvn -pl commons test

# Compile all modules to see remaining errors across services
mvn clean compile -DskipTests 2>&1 | Select-String "ERROR" | Select-Object -First 50
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports in `commons\src`
- [ ] `mvn -pl commons clean install` succeeds
- [ ] Downstream compile errors list shared with Sakshi and Pankaj

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
