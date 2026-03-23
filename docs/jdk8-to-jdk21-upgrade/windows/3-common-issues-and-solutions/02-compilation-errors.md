# 02 - Compilation Errors (Windows)

---

## Issue 1: `'java' is not recognized as an internal or external command`

**Error:**
```
'java' is not recognized as an internal or external command
```

**Root Cause:** JDK not added to system `PATH`, or `JAVA_HOME` not set.

**Fix:**
```powershell
# Run as Administrator
[System.Environment]::SetEnvironmentVariable(
    "JAVA_HOME",
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot",
    "Machine"
)

$currentPath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
if ($currentPath -notlike "*JAVA_HOME*") {
    [System.Environment]::SetEnvironmentVariable("Path", "$currentPath;%JAVA_HOME%\bin", "Machine")
}
```

**Then close and reopen PowerShell:**
```powershell
java -version
# Expected: openjdk version "21.x.x"
```

---

## Issue 2: `'mvn' is not recognized`

**Root Cause:** Maven `bin` directory not in system `PATH`.

**Fix:**
```powershell
# Find Maven install location
Get-ChildItem "C:\Program Files\Apache\" -Directory

# Add to PATH (run as Administrator)
$mavenHome = "C:\Program Files\Apache\Maven\apache-maven-3.9.9"
[System.Environment]::SetEnvironmentVariable("MAVEN_HOME", $mavenHome, "Machine")
$path = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
[System.Environment]::SetEnvironmentVariable("Path", "$path;$mavenHome\bin", "Machine")
```

---

## Issue 3: `mvn` Uses JDK 8 Instead of JDK 21

**Symptom:** `mvn -version` shows `Java version: 1.8` after updating `JAVA_HOME`.

**Root Cause:** Environment variable change requires a **new** shell session. Old session still has JDK 8 in memory.

**Fix:**
1. Close the current PowerShell window completely
2. Open a new PowerShell window
3. Verify: `mvn -version` → must show `Java version: 21`

If still wrong, check for a `JAVA_HOME` override in the **user** environment variables (not system):
```powershell
# Check user-level JAVA_HOME
[System.Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
# If this shows JDK 8 path, update it:
[System.Environment]::SetEnvironmentVariable(
    "JAVA_HOME",
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot",
    "User"
)
```

---

## Issue 4: `error: release version 21 not supported`

**Root Cause:** Old `maven-compiler-plugin` (3.8.x or earlier).

**Fix:** Same as Mac — upgrade to `maven-compiler-plugin:3.13.0` in parent POM. See [Mac Compilation Errors](../../mac/3-common-issues-and-solutions/02-compilation-errors.md#issue-1).

---

## Issue 5: Path Too Long — Build Failure

**Error:**
```
The filename or extension is too long
CreateProcess error=206
```

**Root Cause:** Windows default `MAX_PATH` is 260 characters. Maven builds with deep dependency paths can exceed this.

**Fix:**
```powershell
# Enable long paths (run as Administrator)
Set-ItemProperty `
    "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" `
    -Name LongPathsEnabled `
    -Value 1

# Enable in Git
git config --global core.longpaths true

# Reboot or restart shell to take effect
```

---

## Issue 6: `Cannot find symbol @Builder` / Lombok Not Processing

**Root Cause:** Same as Mac — Lombok not in `annotationProcessorPaths`.

**Fix:** Same POM change as Mac. See [Mac Compilation Errors](../../mac/3-common-issues-and-solutions/02-compilation-errors.md#issue-2).

**Windows diagnostic:**
```powershell
# Check if Lombok jar is in local repo
Test-Path "$env:USERPROFILE\.m2\repository\org\projectlombok\lombok\1.18.34\lombok-1.18.34.jar"
# If false, run: mvn dependency:resolve
```

---

## Issue 7: JJWT `parseClaimsJws` / `parserBuilder` Not Found

Same as Mac. See [Mac Compilation Errors](../../mac/3-common-issues-and-solutions/02-compilation-errors.md#issue-3).

**Windows: Find JJWT usages quickly:**
```powershell
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "parserBuilder|parseClaimsJws|getBody\(\)|setSubject|setExpiration|setIssuedAt|SignatureAlgorithm"
```

---

## Issue 8: `HttpStatus` vs `HttpStatusCode` in `GlobalExceptionHandler`

Same as Mac. See [Mac Compilation Errors](../../mac/3-common-issues-and-solutions/02-compilation-errors.md#issue-5).

---

## Issue 9: Docker Not Found / Docker Desktop Not Running

**Error during integration tests:**
```
Could not find a valid Docker environment
```

**Root Cause:** Docker Desktop is not running or not installed.

**Fix:**
```powershell
# Check Docker status
docker info

# If Docker not running, start Docker Desktop
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Wait for Docker to start
Start-Sleep -Seconds 30
docker info
```

---

## Issue 10: `InaccessibleObjectException` in Tests

Same fix as Mac — add `--add-opens` to Surefire plugin. See [Mac Compilation Errors](../../mac/3-common-issues-and-solutions/02-compilation-errors.md#issue-4).
