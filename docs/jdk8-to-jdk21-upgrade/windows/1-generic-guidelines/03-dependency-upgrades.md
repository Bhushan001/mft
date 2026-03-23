# 03 - Dependency Upgrades (Windows)

> **Note:** Dependency upgrades are **100% identical** to Mac. POM/XML changes are OS-independent.

Please refer to the [Mac version](../../mac/1-generic-guidelines/03-dependency-upgrades.md) for the complete version matrix and all POM snippets.

---

## Quick Reference: Version Matrix

| Dependency | From | To | Breaking? |
|---|---|---|---|
| Spring Boot | 2.7.18 | 3.3.4 | YES |
| Spring Cloud | 2021.0.9 | 2023.0.3 | YES |
| Spring Security | 5.7.x | 6.3.x | YES |
| Hibernate | 5.6.x | 6.5.x | YES |
| Spring Batch | 4.3.x | 5.1.x | YES |
| JJWT | 0.11.5 | 0.12.6 | YES |
| Resilience4j | 1.7.1 | 2.2.0 | Minor |
| Lombok | 1.18.30 | 1.18.34 | No |
| MapStruct | 1.5.5.Final | 1.6.2.Final | No |
| Liquibase | 4.x | 4.29.x | Minor |

---

## Windows Tip: Check for Dependency Conflicts

```powershell
# Show full dependency tree for a module
mvn -pl auth-service dependency:tree -Dverbose

# Check for specific library versions
mvn -pl auth-service dependency:tree -Dincludes=org.springframework.security

# List available updates
mvn versions:display-dependency-updates
```

All `mvn` commands work identically on Windows.
