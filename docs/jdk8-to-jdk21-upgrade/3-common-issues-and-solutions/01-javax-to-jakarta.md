# 01 - javax to jakarta Migration Issues

---

## Issue 1: `package javax.persistence does not exist`

**Error:**
```
[ERROR] .../UserProfile.java:[3,23] error: package javax.persistence does not exist
```

**Root Cause:** Spring Boot 3 uses Jakarta EE 10. All `javax.persistence.*` classes moved to `jakarta.persistence.*`.

**Fix:**
```bash
find . -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g' {} +
```

**Services affected:** All services with JPA entities.

---

## Issue 2: `package javax.validation does not exist`

**Error:**
```
[ERROR] .../LoginRequest.java: package javax.validation.constraints does not exist
```

**Fix:**
```bash
find . -name "*.java" -exec sed -i '' \
    's/import javax\.validation\./import jakarta.validation./g' {} +
```

---

## Issue 3: `package javax.servlet does not exist`

**Error:**
```
[ERROR] .../JwtAuthFilter.java: package javax.servlet does not exist
```

**Fix:**
```bash
find . -name "*.java" -exec sed -i '' \
    's/import javax\.servlet\./import jakarta.servlet./g' {} +
```

**Services affected:** api-gateway (WebFlux may not need this), auth-service, any service with custom filters.

---

## Issue 4: `javax.sql.DataSource` wrongly replaced

**Symptom:** `import jakarta.sql.DataSource` causes compile error — `jakarta.sql` does not exist.

**Root Cause:** `javax.sql.*` is part of the **JDK**, not Jakarta EE. It must stay as `javax.sql.*`.

**Fix:** Revert `javax.sql` imports:
```bash
# If you accidentally replaced javax.sql → revert
sed -i '' 's/import jakarta\.sql\./import javax.sql./g' path/to/file.java
```

**Rule:** Only replace `javax.persistence`, `javax.validation`, `javax.servlet`, `javax.annotation`, `javax.transaction`. **Never** replace `javax.sql`.

---

## Issue 5: `javax.transaction.Transactional` vs `jakarta.transaction.Transactional`

**Situation:** Both `jakarta.transaction.Transactional` and `org.springframework.transaction.annotation.Transactional` exist. Spring Boot 3 supports both, but use Spring's annotation for consistency.

**Fix:** Use Spring's `@Transactional` throughout:
```java
// Preferred
import org.springframework.transaction.annotation.Transactional;

// Also works (Jakarta EE)
import jakarta.transaction.Transactional;
```

---

## Issue 6: Missed `javax` import in a test file

**Symptom:** Main code compiles; test fails with `javax.servlet` not found.

**Root Cause:** The sed replacement only targeted `src/main`. Test files in `src/test` were missed.

**Fix:** Run replacement on full `src/` directory, not just `src/main`:
```bash
find src/ -name "*.java" -exec sed -i '' 's/import javax\.servlet\./import jakarta.servlet./g' {} +
# not just src/main
```

---

## Issue 7: Liquibase changeset using `javax` annotations

**Situation:** If any Liquibase change class (custom `Change` implementation) imports `javax.*`.

**Fix:** Same sed replacement on Liquibase custom change classes.

---

## Verification Command

After replacing all imports, verify zero remaining javax imports:

```bash
# Should print nothing (0 results)
grep -rn "import javax\.persistence\." backend/ --include="*.java"
grep -rn "import javax\.validation\." backend/ --include="*.java"
grep -rn "import javax\.servlet\." backend/ --include="*.java"

# These are OK to remain:
grep -rn "import javax\.sql\." backend/ --include="*.java"  # JDK class — OK
grep -rn "import javax\.crypto\." backend/ --include="*.java"  # JDK class — OK
grep -rn "import javax\.net\." backend/ --include="*.java"  # JDK class — OK
```
