# 01 - javax to jakarta Migration Issues (Windows)

> **Shell:** PowerShell 7+

---

## Issue 1: `package javax.persistence does not exist`

**Error:**
```
[ERROR] .../UserProfile.java:[3,23] error: package javax.persistence does not exist
```

**Fix — PowerShell bulk replace:**
```powershell
Set-Location C:\path\to\chrono\backend

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

**Fix — IntelliJ IDEA (safest):**
- `Ctrl+Shift+R` → Replace in Files → enable Regex
- Run each pair from the table below

| Find (Regex) | Replace |
|---|---|
| `import javax\.persistence\.` | `import jakarta.persistence.` |
| `import javax\.validation\.` | `import jakarta.validation.` |
| `import javax\.servlet\.` | `import jakarta.servlet.` |
| `import javax\.annotation\.` | `import jakarta.annotation.` |

---

## Issue 2: Verify Zero Remaining javax Imports (PowerShell)

```powershell
# These should return NO results
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.(persistence|validation|servlet|annotation)\."

# These are OK to remain (JDK classes — do NOT replace):
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.sql\."       # DataSource — JDK, keep as-is

Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.crypto\."    # JDK crypto — keep as-is
```

---

## Issue 3: `javax.sql.DataSource` wrongly replaced

**Symptom:** `import jakarta.sql.DataSource` causes compile error — `jakarta.sql` does not exist.

**Fix:** Revert in PowerShell:
```powershell
Get-ChildItem -Path . -Filter *.java -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $updated = $content -replace 'import jakarta\.sql\.', 'import javax.sql.'
    if ($content -ne $updated) {
        Set-Content -Path $_.FullName -Value $updated -NoNewline
        Write-Host "Reverted: $($_.FullName)"
    }
}
```

---

## Issue 4: Missed `javax` Import in Test Files

**Symptom:** Main code compiles but tests fail — `javax.servlet` not found.

**Root Cause:** Replacement only ran on `src\main`. Test files in `src\test` were skipped.

**Fix:** Run the replacement on the full `src\` directory (not just `src\main`):
```powershell
# Run from the SERVICE directory, not src\main
Set-Location C:\path\to\chrono\backend\auth-service

Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object {
    # ... same replacement block
}
```

---

## Issue 5: File Encoding Issues After PowerShell Replace

**Symptom:** After running PowerShell replacement, some files have BOM added or encoding changed, causing compile errors.

**Root Cause:** `Set-Content` on Windows can default to UTF-16 or add BOM.

**Fix:** Explicitly set UTF-8 without BOM:
```powershell
Get-ChildItem -Path . -Filter *.java -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $updated = $content `
        -replace 'import javax\.persistence\.', 'import jakarta.persistence.' `
        -replace 'import javax\.validation\.', 'import jakarta.validation.' `
        -replace 'import javax\.servlet\.', 'import jakarta.servlet.' `
        -replace 'import javax\.annotation\.', 'import jakarta.annotation.'
    if ($content -ne $updated) {
        # Write UTF-8 without BOM
        [System.IO.File]::WriteAllText($_.FullName, $updated, [System.Text.UTF8Encoding]::new($false))
        Write-Host "Updated: $($_.FullName)"
    }
}
```

> **This is a Windows-only issue.** Use this version of the script to be safe.

---

## Issue 6: Line Endings Changed After Replace (CRLF → LF)

**Symptom:** Git shows every line of every `.java` file changed after the replacement.

**Root Cause:** PowerShell `Set-Content` may normalize line endings.

**Fix:** Check Git line ending config:
```powershell
git config core.autocrlf
# Should be: input (for cross-platform repos) or true (Windows-only)

# If all files show as modified, reset them
git diff --stat  # see how many files changed
git checkout -- .  # WARNING: discards all changes — only run before replacement
```

**Better approach:** Configure Git before running replacements:
```powershell
git config core.autocrlf input
```

Then run the replacement script — only files with actual `javax` changes will show as modified.
