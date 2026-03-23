# 02 - Maven & Build Changes (Windows)

> **OS:** Windows 10 / Windows 11
> **Shell:** PowerShell 7+

---

## Overview

POM changes are **identical** to Mac — XML has no OS differences. Only the setup commands and file paths differ on Windows.

---

## 1. Parent POM Changes

Identical to Mac. See [Mac version](../../mac/1-generic-guidelines/02-maven-and-build-changes.md) for full POM snippets.

**Summary of changes:**
- Spring Boot: `2.7.18` → `3.3.4`
- Spring Cloud: `2021.0.9` → `2023.0.3`
- Java version: `1.8` → `21` (use `<maven.compiler.release>21</maven.compiler.release>`)
- Maven Compiler Plugin: `3.13.0` with Lombok before MapStruct in `annotationProcessorPaths`
- Maven Surefire Plugin: `3.3.1` with `--add-opens` JVM args

---

## 2. Maven Compiler Plugin

Identical XML — no Windows-specific changes:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>21</release>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## 3. Maven Surefire Plugin

Identical XML:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <argLine>
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.util=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
        </argLine>
    </configuration>
</plugin>
```

---

## 4. Docker Build Update (Windows)

Ensure Docker Desktop for Windows is installed and running.

Update all Dockerfiles (same content as Mac):

```dockerfile
# BEFORE
FROM maven:3.8.8-eclipse-temurin-8 AS build
FROM eclipse-temurin:8-jre-alpine

# AFTER
FROM maven:3.9.9-eclipse-temurin-21 AS build
FROM eclipse-temurin:21-jre-alpine
```

Docker Desktop uses Linux containers by default on Windows — Dockerfiles are identical to Mac.

---

## 5. Toolchain Configuration (Windows)

File location on Windows: `C:\Users\<username>\.m2\toolchains.xml`

```powershell
# Find JDK 21 install location
Get-ChildItem "C:\Program Files\Eclipse Adoptium\" -Directory
# e.g., jdk-21.0.5.11-hotspot
```

**`C:\Users\<username>\.m2\toolchains.xml`:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>21</version>
            <vendor>temurin</vendor>
        </provides>
        <configuration>
            <jdkHome>C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```

> **Note:** Use forward slashes or escaped backslashes in the path — Maven handles both:
> `C:/Program Files/Eclipse Adoptium/jdk-21.0.5.11-hotspot`

---

## 6. Spring Boot Properties Migrator

Add temporarily to detect deprecated properties (identical to Mac):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>
```

Remove after resolving all warnings.

---

## 7. Validation: Compilation Check (PowerShell)

```powershell
# Step 1: Compile commons first
mvn -pl commons clean compile -e

# Step 2: Compile all modules
mvn clean compile -e 2>&1 | Select-String "ERROR|WARNING|BUILD"

# Step 3: Full build
mvn clean install

# Expected: [INFO] BUILD SUCCESS
```

---

## 8. Maven Settings File (Windows)

If you need a custom `settings.xml` (e.g., proxy, private repo):

Location: `C:\Users\<username>\.m2\settings.xml`

```powershell
# Open settings.xml in notepad
notepad "$env:USERPROFILE\.m2\settings.xml"

# Or check if it exists
Test-Path "$env:USERPROFILE\.m2\settings.xml"
```

---

## Common Windows Build Issues

| Error | Cause | Fix |
|---|---|---|
| `'mvn' is not recognized` | Maven not in PATH | Add `%MAVEN_HOME%\bin` to System PATH, open new PowerShell |
| `'java' is not recognized` | JDK not in PATH | Add `%JAVA_HOME%\bin` to System PATH, open new PowerShell |
| `error: release version 21 not supported` | Old compiler plugin | Upgrade to `maven-compiler-plugin:3.13.0` |
| Path too long build failure | Windows MAX_PATH | Enable long paths (see Overview page) |
| `mvn` uses JDK 8 instead of 21 | JAVA_HOME still pointing to JDK 8 | Update JAVA_HOME system variable, reopen PowerShell |

---

## Verify Maven Uses JDK 21

```powershell
mvn -version
# Must show:
# Apache Maven 3.9.x
# Java version: 21.x.x, vendor: Eclipse Adoptium, ...
# Java home: C:\Program Files\Eclipse Adoptium\jdk-21...
```

If it shows Java 8, your `JAVA_HOME` is still pointing to the old JDK. Update it in System Properties and open a **new** PowerShell window.
