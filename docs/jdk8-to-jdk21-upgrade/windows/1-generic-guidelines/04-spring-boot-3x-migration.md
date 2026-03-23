# 04 - Spring Boot 3.x Migration (Windows)

> **OS:** Windows 10 / Windows 11
> **Shell:** PowerShell 7+

---

## The Biggest Change: `javax.*` → `jakarta.*`

The package rename is the same as Mac. Only the **replacement commands** differ on Windows.

---

## Bulk Replace `javax.*` → `jakarta.*` on Windows

### Option A: PowerShell Script (Recommended)

Run from the `backend\` directory in PowerShell 7+:

```powershell
# Navigate to backend directory
Set-Location C:\path\to\chrono\backend

# Replace all javax imports in all .java files recursively
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
```

### Option B: IntelliJ IDEA Find & Replace (Easiest for Windows)

1. Open project in IntelliJ IDEA
2. Press `Ctrl+Shift+R` (Replace in Files)
3. Check **Regex** checkbox
4. Run each replacement:

| Find | Replace |
|---|---|
| `import javax\.persistence\.` | `import jakarta.persistence.` |
| `import javax\.validation\.` | `import jakarta.validation.` |
| `import javax\.servlet\.` | `import jakarta.servlet.` |
| `import javax\.annotation\.` | `import jakarta.annotation.` |

5. Scope: **Project** or **Directory: backend/src**

> **IntelliJ is the safest option** — it shows a preview before replacing.

### Option C: VS Code Find & Replace

1. `Ctrl+Shift+H` → Replace in Files
2. Enable regex (`.?*` button)
3. File to include: `**/*.java`
4. Run same replacements as above

---

## Verify Zero Remaining `javax` Imports (PowerShell)

```powershell
# Check for remaining javax imports that should be migrated
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.(persistence|validation|servlet|annotation)\." |
    Select-Object Filename, LineNumber, Line

# Should return no results

# These are OK to remain (JDK classes, not Jakarta EE):
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import javax\.sql\."
# javax.sql.DataSource is JDK — do NOT replace this
```

---

## application.yml Property Changes

Identical to Mac — no Windows-specific differences:

```yaml
# BEFORE
spring:
  redis:
    host: redis
  zipkin:
    base-url: http://zipkin:9411
  sleuth:
    sampler:
      probability: 1.0

# AFTER
spring:
  data:
    redis:
      host: redis
  config:
    import: "optional:configserver:http://config-server:8888"
management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
  tracing:
    sampling:
      probability: 1.0
```

---

## Spring Boot Properties Migrator (Windows)

Add to `pom.xml` temporarily:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>
```

Run the service and check startup output in PowerShell:

```powershell
mvn -pl auth-service spring-boot:run 2>&1 | Select-String "WARN|deprecated|renamed"
```

Fix all deprecation warnings, then remove the dependency.

---

## Micrometer Tracing (Sleuth Replacement)

Same dependencies as Mac — POM changes apply regardless of OS.
See [Mac version](../../mac/1-generic-guidelines/04-spring-boot-3x-migration.md) for full dependency XML.

---

## Hibernate 6 Changes

Identical to Mac — see [Mac version](../../mac/1-generic-guidelines/04-spring-boot-3x-migration.md#spring-data-jpa--hibernate-6).

---

## Checklist

- [ ] Run PowerShell (or IntelliJ) bulk replace — zero `javax.persistence/validation/servlet/annotation` imports
- [ ] `spring.redis.*` → `spring.data.redis.*` in all `application.yml` files
- [ ] Sleuth dependencies replaced with Micrometer Tracing
- [ ] Zipkin URL property updated
- [ ] `spring-boot-properties-migrator` added, app starts without warnings, then removed
