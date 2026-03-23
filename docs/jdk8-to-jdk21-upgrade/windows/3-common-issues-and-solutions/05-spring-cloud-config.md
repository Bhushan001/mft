# 05 - Spring Cloud & Config Issues (Windows)

> Most issues and fixes are **identical to Mac**. See [Mac version](../../mac/3-common-issues-and-solutions/05-spring-cloud-config.md) for full details.

Windows-specific notes are listed below.

---

## Issue 1: `bootstrap.yml` Not Loading

Same fix as Mac — migrate to `spring.config.import` or add `spring-cloud-starter-bootstrap`.

---

## Issue 2: `spring.redis.*` Properties Ignored

Same fix — rename to `spring.data.redis.*`.

**Windows: Find all occurrences across YML files:**
```powershell
Get-ChildItem -Path . -Filter "*.yml" -Recurse |
    Select-String "spring\.redis\."

# Bulk replace in all yml files
Get-ChildItem -Path . -Filter "*.yml" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $updated = $content -replace 'spring:\s*\n(\s+)redis:', "spring:`n`$1data:`n`$1  redis:"
    # Note: YML indentation changes are tricky — prefer manual edit for YML
    # Use IntelliJ Find & Replace for yml files
}
```

> **Recommendation:** For `application.yml` edits, use IntelliJ IDEA **Find & Replace** (`Ctrl+Shift+R`) rather than PowerShell, to preserve YAML indentation safely.

---

## Issue 3: Spring Cloud Sleuth Classes Not Found

Same fix as Mac — replace with Micrometer Tracing. See [Mac version](../../mac/3-common-issues-and-solutions/05-spring-cloud-config.md#issue-3).

**Windows: Verify Sleuth is fully removed:**
```powershell
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "org\.springframework\.cloud\.sleuth"
# Should return nothing after migration
```

---

## Issue 4: Feign `404` After Upgrade

Same diagnosis as Mac. **Windows: Check Eureka registration:**
```powershell
# See all registered services in Eureka
Invoke-RestMethod "http://localhost:8761/eureka/apps" |
    Select-Xml "//application/name" |
    ForEach-Object { $_.Node.InnerText }
```

---

## Issue 5: Config Server Not Reachable in Docker

**Windows-Specific:** Docker Desktop uses a Linux VM internally. Services inside Docker containers cannot reach `localhost` of the Windows host. Use Docker service names:

```yaml
# application.yml inside Docker container
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
  # NOT: http://localhost:8888
```

---

## Issue 6: Eureka Dashboard Errors

Same fix as Mac. Add to `service-registry/application.yml`:
```yaml
spring:
  freemarker:
    prefer-file-system-access: false
```
