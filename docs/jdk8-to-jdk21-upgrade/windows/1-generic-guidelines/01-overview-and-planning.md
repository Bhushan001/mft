# 01 - Overview & Planning (Windows)

> **Owner:** Bhushan Gadekar
> **OS:** Windows 10 / Windows 11
> **Shell:** PowerShell 7+ (recommended)

---

## Why Upgrade to JDK 21?

Same rationale as Mac — JDK 8 EOL, Spring Boot 2.7 EOL, performance gains, Virtual Threads, modern language features. See [Mac version](../../mac/1-generic-guidelines/01-overview-and-planning.md) for full details.

---

## Pre-Migration Checklist

### Environment Setup (Windows)

#### Step 1: Install PowerShell 7+

Windows ships with PowerShell 5.x. Upgrade to 7+ for better scripting:

```powershell
winget install Microsoft.PowerShell
```

Or download from: https://github.com/PowerShell/PowerShell/releases

Verify:
```powershell
$PSVersionTable.PSVersion
# Major should be 7 or higher
```

---

#### Step 2: Install JDK 21

**Option A — winget (Recommended):**
```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

**Option B — Chocolatey:**
```powershell
# Install Chocolatey first (if not installed)
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Install JDK 21
choco install temurin21 -y
```

**Option C — Manual:**
1. Download from: https://adoptium.net/temurin/releases/?version=21
2. Select: **Windows x64**, **JDK**, `.msi` installer
3. Run installer — check "Set JAVA_HOME" and "Add to PATH" during setup

---

#### Step 3: Set JAVA_HOME (Windows)

**Via PowerShell (permanent — requires admin):**
```powershell
# Run PowerShell as Administrator
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot", "Machine")
[System.Environment]::SetEnvironmentVariable("Path", $env:Path + ";%JAVA_HOME%\bin", "Machine")
```

**Via GUI:**
1. Win + R → `sysdm.cpl` → Advanced → Environment Variables
2. Under **System variables** → New:
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot`
3. Edit `Path` → New → `%JAVA_HOME%\bin`
4. Click OK → Open a **new** PowerShell window

**Verify (new PowerShell window):**
```powershell
java -version
# Expected: openjdk version "21.x.x" ...

$env:JAVA_HOME
# Expected: C:\Program Files\Eclipse Adoptium\jdk-21...
```

---

#### Step 4: Install Maven 3.9.x

**Option A — winget:**
```powershell
winget install Apache.Maven
```

**Option B — Manual:**
1. Download: https://maven.apache.org/download.cgi → `apache-maven-3.9.9-bin.zip`
2. Extract to `C:\Program Files\Apache\Maven\apache-maven-3.9.9`
3. Add to PATH via System Properties:
   - Variable: `MAVEN_HOME` = `C:\Program Files\Apache\Maven\apache-maven-3.9.9`
   - Add to `Path`: `%MAVEN_HOME%\bin`

**Verify:**
```powershell
mvn -version
# Expected: Apache Maven 3.9.x ... Java version: 21
```

---

#### Step 5: Create Migration Branch

```powershell
# Tag current JDK 8 baseline
git tag jdk8-baseline
git push origin jdk8-baseline

# Create migration branch
git checkout -b jdk21-migration
git push -u origin jdk21-migration
```

---

#### Step 6: Dependency Audit (PowerShell)

```powershell
# Navigate to backend directory
Set-Location C:\path\to\chrono\backend

# Count javax imports across all .java files
(Get-ChildItem -Recurse -Filter *.java | Select-String "import javax\.").Count

# List all javax imports to a file
Get-ChildItem -Recurse -Filter *.java |
    Select-String "import javax\." |
    Out-File ..\docs\jdk8-to-jdk21-upgrade\javax-imports-baseline.txt

# Find any sun.* imports (will break on JDK 17+)
Get-ChildItem -Recurse -Filter *.java |
    Select-String "import sun\."
```

---

## Migration Strategy

Same module-by-module strategy as Mac. Migrate in this order:

```
commons → config-server → service-registry → api-gateway
       → auth-service → user-service → tenant-service
       → mapper-service → engine-service
       → etl-service → orchestrator-service → audit-service
```

---

## Risk Register (Windows-Specific Additional Risks)

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| JAVA_HOME pointing to JDK 8 in session | High | High | Always open new PowerShell after setting env vars; verify with `java -version` |
| Maven using system JDK instead of JDK 21 | Medium | High | Verify `mvn -version` shows Java 21 |
| Long path issues (`MAX_PATH`) | Low | Medium | Enable long paths: `Set-ItemProperty "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name LongPathsEnabled -Value 1` |
| Line endings (CRLF vs LF) in scripts | Low | Low | Configure Git: `git config core.autocrlf input` |
| Docker Desktop required | Low | Medium | Install Docker Desktop for Windows before running docker-compose |

---

## Enable Long Paths (Recommended)

Windows has a 260-character path limit by default. Maven builds with deep directory structures can hit this:

```powershell
# Run as Administrator
Set-ItemProperty "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" `
    -Name LongPathsEnabled -Value 1

# Also configure Git
git config --global core.longpaths true
```

---

## Useful Commands Reference (Windows vs Mac)

| Task | Mac/Linux | Windows PowerShell |
|---|---|---|
| Set env var | `export JAVA_HOME=/path` | `$env:JAVA_HOME = "C:\path"` |
| Verify Java | `java -version` | `java -version` (same) |
| Find .java files | `find . -name "*.java"` | `Get-ChildItem -Recurse -Filter *.java` |
| Search in files | `grep -rn "pattern" --include="*.java"` | `Get-ChildItem -Recurse -Filter *.java \| Select-String "pattern"` |
| Replace in files | `sed -i '' 's/old/new/g' file` | `(Get-Content file) -replace 'old','new' \| Set-Content file` |
| Count lines | `wc -l` | `Measure-Object -Line` |
| Maven compile | `mvn clean compile` | `mvn clean compile` (same) |
| Run tests | `mvn test` | `mvn test` (same) |
