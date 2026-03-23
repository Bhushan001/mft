# JDK 8 → JDK 21 Migration: Master Index (Windows)

> **OS:** Windows 10 / Windows 11
> **Shell:** PowerShell 7+ (recommended) or Command Prompt
> **Project Lead:** Bhushan Gadekar
> **Team:** Bhushan, Sakshi, Pankaj
> **Target Version:** JDK 21 (LTS) + Spring Boot 3.3.x + Spring Cloud 2023.x

---

## Document Structure

| Section | Purpose |
|---|---|
| [1. Generic Guidelines](./1-generic-guidelines/index.md) | Step-by-step technical upgrade instructions |
| [2. Activities Tracking](./2-activities-tracking/index.md) | Task assignments per team member |
| [3. Common Issues and Solutions](./3-common-issues-and-solutions/index.md) | Errors encountered and fixes |

---

## Windows vs Mac: Key Differences

| Task | Mac/Linux | Windows |
|---|---|---|
| Install JDK | `brew install --cask temurin@21` | `winget install EclipseAdoptium.Temurin.21.JDK` |
| Set JAVA_HOME | `export JAVA_HOME=...` in `.zshrc` | System Properties → Environment Variables |
| Install Maven | `brew install maven` | Download zip, extract, add to PATH |
| Shell | Terminal (zsh/bash) | PowerShell 7+ or Windows Terminal |
| Find & Replace | `sed -i ''` | PowerShell `(Get-Content) -replace` |
| Search in files | `grep -rn` | `Select-String` |
| Find files | `find . -name "*.java"` | `Get-ChildItem -Recurse -Filter *.java` |
| Environment var | `export VAR=value` | `$env:VAR = "value"` (session) / `setx VAR value` (permanent) |
| Home dir | `~` | `$HOME` or `C:\Users\<username>` |
| Maven settings | `~/.m2/` | `C:\Users\<username>\.m2\` |
| Path separator | `:` | `;` |
| curl | `curl` | `curl.exe` (Windows 10+) or `Invoke-RestMethod` |

> All Java code, XML, YAML, and Maven commands (`mvn`) are **identical** on Windows and Mac.

---

## Migration Scope (same as Mac)

| Component | From | To |
|---|---|---|
| JDK | 8 | 21 (LTS) |
| Spring Boot | 2.7.18 | 3.3.x |
| Spring Cloud | 2021.0.9 | 2023.0.x |
| Spring Security | 5.x | 6.x |
| Jakarta EE | javax.* | jakarta.* |
| JJWT | 0.11.5 | 0.12.x |

---

## Key Milestones (same as Mac)

| # | Milestone | Owner |
|---|---|---|
| 1 | Environment setup (JDK 21 + Maven) | Bhushan |
| 2 | Parent POM & commons | Bhushan |
| 3 | API Gateway | Bhushan |
| 4 | Auth Service | Sakshi |
| 5 | User + Tenant Service | Sakshi |
| 6 | Mapper + Engine Service | Pankaj |
| 7 | ETL + Orchestrator + Audit | Pankaj |
| 8 | Integration sign-off | All |
