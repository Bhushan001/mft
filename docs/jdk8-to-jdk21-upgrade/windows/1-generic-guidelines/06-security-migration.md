# 06 - Security Migration (Windows)

> **Note:** All Spring Security 6.x code changes are **identical** to Mac. Java code is OS-independent.

Please refer to the [Mac version](../../mac/1-generic-guidelines/06-security-migration.md) for all code examples.

---

## Summary of Changes Required

| Change | What to do |
|---|---|
| `WebSecurityConfigurerAdapter` removed | Replace with `@Bean SecurityFilterChain` |
| `antMatchers` removed | Use `requestMatchers` |
| `authorizeRequests` deprecated | Use `authorizeHttpRequests` |
| `cors().and().csrf().disable()` | Use lambda style |
| JWT filter imports | `javax.servlet.*` → `jakarta.servlet.*` |
| WebFlux gateway security | `csrf(ServerHttpSecurity.CsrfSpec::disable)` |

---

## Windows Tip: Find Security Config Classes

```powershell
# Find all classes extending WebSecurityConfigurerAdapter (need migration)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "WebSecurityConfigurerAdapter"

# Find antMatchers usage
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "antMatchers"

# Find authorizeRequests usage
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "\.authorizeRequests\(\)"
```

---

## Migration Checklist

- [ ] Zero `WebSecurityConfigurerAdapter` references
- [ ] Zero `antMatchers` references
- [ ] Zero `authorizeRequests()` references (replaced by `authorizeHttpRequests`)
- [ ] JWT filter: `javax.servlet` → `jakarta.servlet`
- [ ] Login returns 200 with valid credentials
- [ ] Invalid token returns 401
