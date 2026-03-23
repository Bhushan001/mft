# 05 - Code Changes (JDK 9–21 Language & API Changes)

> **Confluence Page:** Generic Guidelines / Code Changes
> **Owner:** All team members

---

## Overview

JDK 21 introduces many language improvements and removes several deprecated APIs from JDK 8. This page covers what must be changed (breaking) and what can optionally be modernized.

---

## Breaking API Removals

### 1. `sun.*` Internal APIs (Removed)

JDK 17+ enforces strong encapsulation of internal APIs via the module system.

```java
// If any code uses sun.* packages — replace immediately
// Common violations:
import sun.misc.BASE64Encoder;   // REMOVE → use java.util.Base64
import sun.misc.Unsafe;          // Flag for review — avoid in application code
import sun.reflect.Reflection;  // REMOVE → use StackWalker (JDK 9+)
```

**Fix for Base64:**
```java
// BEFORE
String encoded = new sun.misc.BASE64Encoder().encode(bytes);
byte[] decoded = new sun.misc.BASE64Decoder().decodeBuffer(encoded);

// AFTER
String encoded = Base64.getEncoder().encodeToString(bytes);
byte[] decoded = Base64.getDecoder().decode(encoded);
```

---

### 2. `SecurityManager` (Removed in JDK 17)

```java
// Remove any of these — SecurityManager is gone
System.setSecurityManager(new SecurityManager());
System.getSecurityManager();
```

---

### 3. Nashorn JavaScript Engine (Removed in JDK 15)

If any service uses `ScriptEngine` with Nashorn:

```java
// BEFORE
ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

// AFTER — use GraalVM Polyglot API or remove JS eval
// If rule evaluation is needed, use MVEL or SpEL instead
```

---

### 4. `String.getBytes()` Encoding

Not a removal, but a behavioral difference: always specify charset explicitly:

```java
// BEFORE — relies on platform default (can vary between JDK versions)
byte[] bytes = str.getBytes();

// AFTER — explicit charset
byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
```

---

## Module System (JPMS) — Reflection Restrictions

JDK 9+ enforces module boundaries. Libraries using deep reflection (Mockito, Spring, Hibernate) may fail with:

```
InaccessibleObjectException: Unable to make ... accessible
```

**Fix: Add JVM args** (in `maven-surefire-plugin` and in application startup):

```xml
<!-- pom.xml surefire plugin -->
<argLine>
    --add-opens java.base/java.lang=ALL-UNNAMED
    --add-opens java.base/java.util=ALL-UNNAMED
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED
    --add-opens java.base/java.nio=ALL-UNNAMED
</argLine>
```

```bash
# For application JVM args (Docker / startup script)
JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
```

> In practice, modern versions of Spring Boot 3.x, Hibernate 6, and Mockito 5.x handle this automatically. Only add `--add-opens` if you see this error.

---

## Optional: New Java 21 Features to Adopt

These are not required for migration but improve code quality. Adopt gradually after services are running on JDK 21.

### 1. Records (Java 16+)

Use for immutable DTOs:

```java
// BEFORE — Lombok DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorField {
    private String field;
    private String message;
    private Object rejectedValue;
}

// AFTER — Java Record
public record ErrorField(String field, String message, Object rejectedValue) {}
```

> **Note:** Records work well for response/request DTOs but **not** for JPA entities (entities need mutability and default constructors).

---

### 2. Pattern Matching `instanceof` (Java 16+)

```java
// BEFORE
if (ex instanceof BusinessException) {
    BusinessException bex = (BusinessException) ex;
    return ResponseEntity.status(bex.getStatus()).body(...);
}

// AFTER
if (ex instanceof BusinessException bex) {
    return ResponseEntity.status(bex.getStatus()).body(...);
}
```

---

### 3. Text Blocks (Java 15+)

Useful for SQL, JSON in tests:

```java
// BEFORE
String sql = "SELECT u FROM UserProfile u " +
             "WHERE u.tenantId = :tenantId " +
             "AND u.deletedAt IS NULL";

// AFTER
String sql = """
    SELECT u FROM UserProfile u
    WHERE u.tenantId = :tenantId
    AND u.deletedAt IS NULL
    """;
```

---

### 4. Switch Expressions (Java 14+)

```java
// BEFORE
String roleName;
switch (role) {
    case SUPER_ADMIN: roleName = "Super Admin"; break;
    case TENANT_ADMIN: roleName = "Tenant Admin"; break;
    default: roleName = "User";
}

// AFTER
String roleName = switch (role) {
    case SUPER_ADMIN  -> "Super Admin";
    case TENANT_ADMIN -> "Tenant Admin";
    default           -> "User";
};
```

---

### 5. Virtual Threads (Java 21 — Project Loom)

For I/O-bound services (auth, user, engine), virtual threads can dramatically increase throughput:

```yaml
# application.yml — enable virtual threads for Spring MVC (Tomcat)
spring:
  threads:
    virtual:
      enabled: true
```

> This single property enables virtual threads for the embedded Tomcat thread pool. No code changes required. Evaluate with load testing before enabling in production.

---

### 6. Sealed Classes (Java 17+)

For the exception hierarchy in `commons`, sealed classes enforce the closed hierarchy:

```java
// AFTER — optional refactor
public sealed class BaseException extends RuntimeException
    permits BusinessException, DuplicateResourceException,
            ResourceNotFoundException, TenantViolationException {
    // ...
}
```

---

## Lombok on JDK 21: Known Issues

| Issue | Cause | Fix |
|---|---|---|
| `@Builder` not generating | Lombok not in annotationProcessorPaths | Add to maven-compiler-plugin (see Build page) |
| `@SneakyThrows` with checked exceptions | Module system restrictions | Remove `@SneakyThrows` on JDK 21, use explicit try-catch |
| `@UtilityClass` warnings | Minor internal reflection | No action needed unless it errors |

---

## String / Collection API Updates

JDK 21 adds several convenience methods:

```java
// String improvements
String s = "  hello  ";
s.isBlank();           // since Java 11 — use instead of isEmpty()
s.strip();             // since Java 11 — Unicode-aware trim()
s.repeat(3);           // since Java 11

// Collection factory methods (Java 9+)
List<String> list = List.of("a", "b", "c");           // immutable
Map<String, String> map = Map.of("key", "value");     // immutable
Set<String> set = Set.of("x", "y");                   // immutable

// Stream improvements
Stream.ofNullable(maybeNull).forEach(...);             // Java 9+
list.stream().mapMulti((s, consumer) -> {...});        // Java 16+
```

---

## Migration Checklist

- [ ] Scan and remove all `sun.*` imports
- [ ] Remove `SecurityManager` usage if present
- [ ] Remove Nashorn `ScriptEngine` usage if present
- [ ] Replace `str.getBytes()` with `str.getBytes(StandardCharsets.UTF_8)`
- [ ] Run tests; if `InaccessibleObjectException` occurs, add `--add-opens`
- [ ] (Optional) Migrate immutable DTOs to Records
- [ ] (Optional) Enable virtual threads for I/O-heavy services
