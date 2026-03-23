# 05 - Code Changes (Windows)

> **Note:** All Java code changes are **identical** to Mac. Java source code is OS-independent.

Please refer to the [Mac version](../../mac/1-generic-guidelines/05-code-changes.md) for all code examples.

---

## Windows-Specific: Search for Removed APIs

Use PowerShell instead of `grep` to scan for problematic patterns:

```powershell
# Find sun.* imports (will break on JDK 17+)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import sun\."

# Find SecurityManager usage
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "SecurityManager|setSecurityManager"

# Find Nashorn / ScriptEngine usage
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "ScriptEngine|Nashorn|ScriptEngineManager"

# Find getBytes() without charset (should be explicit)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "\.getBytes\(\)"
```

---

## Summary: Breaking API Removals

| Removed API | Fix |
|---|---|
| `sun.misc.BASE64Encoder/Decoder` | Use `java.util.Base64` |
| `SecurityManager` | Remove all usage |
| Nashorn `ScriptEngine` | Use GraalVM or SpEL |
| `String.getBytes()` without charset | Use `getBytes(StandardCharsets.UTF_8)` |

---

## Optional: New Java 21 Features

All code examples (Records, Pattern Matching, Text Blocks, Virtual Threads, Switch Expressions) are identical — see [Mac version](../../mac/1-generic-guidelines/05-code-changes.md).

**Enable Virtual Threads on Windows (same config):**
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
