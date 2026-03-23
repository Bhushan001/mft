# 03 - Spring Security 6.x Issues (Windows)

> All Spring Security 6 code changes are **identical to Mac** — Java code is OS-independent.

Please refer to the [Mac version](../../mac/3-common-issues-and-solutions/03-spring-security-6x.md) for all issues and fixes.

---

## Windows Tip: Find Security Config Classes

```powershell
# Find WebSecurityConfigurerAdapter (must be removed)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "WebSecurityConfigurerAdapter"

# Find antMatchers (must be replaced with requestMatchers)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "antMatchers"

# Find authorizeRequests (must be replaced with authorizeHttpRequests)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "\.authorizeRequests\(\)"

# Find old cors().and() pattern
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "cors\(\)\.and\(\)"
```

---

## Summary of Changes

| Removed | Replace With |
|---|---|
| `extends WebSecurityConfigurerAdapter` | `@Bean SecurityFilterChain` |
| `.antMatchers(...)` | `.requestMatchers(...)` |
| `.authorizeRequests()` | `.authorizeHttpRequests(auth -> auth...)` |
| `csrf().disable()` chained | `csrf(AbstractHttpConfigurer::disable)` |
| `authenticationManagerBean()` override | `AuthenticationManager` bean from `AuthenticationConfiguration` |
