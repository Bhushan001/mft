# Task 1 — Environment Setup & Parent POM (Windows)

> **Owner:** Bhushan Gadekar
> **OS:** Windows 10 / Windows 11
> **Shell:** PowerShell 7+
> **Sprint:** 1 — Week 1
> **Status:** Pending

---

## Objective

Set up the JDK 21 development environment on Windows, create the migration branch, and upgrade the parent POM.

---

## Step 1: Install PowerShell 7+

```powershell
winget install Microsoft.PowerShell
# OR download from: https://aka.ms/powershell-release?tag=stable
```

Open **Windows Terminal** and set PowerShell 7 as default shell.

---

## Step 2: Install JDK 21

```powershell
# Option A: winget
winget install EclipseAdoptium.Temurin.21.JDK

# Option B: Chocolatey
choco install temurin21 -y
```

**Manual download:** https://adoptium.net/temurin/releases/?version=21 → Windows x64 JDK `.msi`

---

## Step 3: Set JAVA_HOME

**Via PowerShell (run as Administrator):**
```powershell
# Set JAVA_HOME permanently
[System.Environment]::SetEnvironmentVariable(
    "JAVA_HOME",
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot",
    "Machine"
)

# Add to PATH
$currentPath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
[System.Environment]::SetEnvironmentVariable(
    "Path",
    "$currentPath;%JAVA_HOME%\bin",
    "Machine"
)
```

**Close and reopen PowerShell, then verify:**
```powershell
java -version
# Expected: openjdk version "21.x.x"

$env:JAVA_HOME
# Expected: C:\Program Files\Eclipse Adoptium\jdk-21...
```

---

## Step 4: Install Maven 3.9.x

```powershell
# Option A: winget
winget install Apache.Maven

# Option B: Chocolatey
choco install maven -y
```

**Manual:**
1. Download: https://maven.apache.org/download.cgi → `apache-maven-3.9.9-bin.zip`
2. Extract to `C:\Program Files\Apache\Maven\apache-maven-3.9.9`
3. System Properties → Environment Variables:
   - New system variable: `MAVEN_HOME` = `C:\Program Files\Apache\Maven\apache-maven-3.9.9`
   - Edit `Path` → New → `%MAVEN_HOME%\bin`

```powershell
# Verify
mvn -version
# Expected: Apache Maven 3.9.x ... Java version: 21
```

---

## Step 5: Enable Long Paths

```powershell
# Run as Administrator — prevents MAX_PATH build failures
Set-ItemProperty "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" `
    -Name LongPathsEnabled -Value 1

git config --global core.longpaths true
```

---

## Step 6: Create Migration Branch

```powershell
# Navigate to project
Set-Location C:\path\to\chrono

# Tag baseline
git tag jdk8-baseline
git push origin jdk8-baseline

# Create migration branch
git checkout -b jdk21-migration
git push -u origin jdk21-migration
```

---

## Step 7: Dependency Audit

```powershell
Set-Location C:\path\to\chrono\backend

# Count javax imports
(Get-ChildItem -Recurse -Filter *.java | Select-String "import javax\.").Count

# Save to file
Get-ChildItem -Recurse -Filter *.java |
    Select-String "import javax\." |
    Out-File ..\docs\jdk8-to-jdk21-upgrade\javax-imports-baseline.txt

# Find sun.* imports
Get-ChildItem -Recurse -Filter *.java | Select-String "import sun\."
```

---

## Step 8: Upgrade Parent POM

POM changes are **identical to Mac**. See [Mac Task 1](../../../mac/2-activities-tracking/bhushan/01-environment-setup-and-parent-pom.md) for full POM XML.

Key changes:
- Spring Boot: `2.7.18` → `3.3.4`
- Spring Cloud BOM: `2021.0.9` → `2023.0.3`
- Java: `1.8` → `21`
- Maven Compiler Plugin: `3.13.0`

**Validate:**
```powershell
mvn -pl commons clean compile -e
# Expected: BUILD SUCCESS
```

---

## Step 9: Share With Team

Push branch and notify Sakshi and Pankaj:
```powershell
git add backend/pom.xml
git commit -m "chore: upgrade parent POM to Spring Boot 3.3.4, JDK 21"
git push origin jdk21-migration
```

---

## Completion Criteria

- [ ] `java -version` shows JDK 21
- [ ] `mvn -version` shows Maven 3.9.x + Java 21
- [ ] Long paths enabled
- [ ] `jdk21-migration` branch pushed
- [ ] Javax import count saved
- [ ] `mvn -pl commons clean compile` succeeds

---

## Notes / Observations

| Date | Observation |
|---|---|
| | |
