# 03 - Spring Security 6.x Issues

---

## Issue 1: `WebSecurityConfigurerAdapter` not found

**Error:**
```
[ERROR] .../SecurityConfig.java: cannot find symbol
  class WebSecurityConfigurerAdapter
```

**Root Cause:** `WebSecurityConfigurerAdapter` was deprecated in Spring Security 5.7 and **removed** in Spring Security 6.

**Fix:** Replace with `SecurityFilterChain` bean:
```java
// BEFORE
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()...
    }
}

// AFTER
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
```

---

## Issue 2: `antMatchers` not found

**Error:**
```
[ERROR] cannot find symbol: method antMatchers(String)
```

**Root Cause:** `antMatchers()`, `mvcMatchers()`, and `regexMatchers()` all removed in Spring Security 6.

**Fix:** Replace with `requestMatchers()`:
```java
// BEFORE
.authorizeRequests().antMatchers("/api/v1/auth/**").permitAll()

// AFTER
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll())
```

---

## Issue 3: `authorizeRequests` deprecated / not working

**Error:**
```
WARN: The method `authorizeRequests` is deprecated. Use `authorizeHttpRequests` instead.
```

**Fix:**
```java
// BEFORE
.authorizeRequests().antMatchers(...).permitAll()

// AFTER
.authorizeHttpRequests(auth -> auth.requestMatchers(...).permitAll())
```

---

## Issue 4: `AuthenticationManagerBean` override not working

**Error:**
```
[ERROR] method does not override or implement a method from a supertype
```

**Root Cause:** Can't override `authenticationManagerBean()` without `WebSecurityConfigurerAdapter`.

**Fix:**
```java
// BEFORE (inside WebSecurityConfigurerAdapter)
@Bean
@Override
public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
}

// AFTER
@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```

---

## Issue 5: `cors().and()` method not found

**Error:**
```
[ERROR] cannot find symbol: method and()
```

**Root Cause:** Spring Security 6 removed the chained `.and()` style. All configuration must use lambdas.

**Fix:**
```java
// BEFORE
http.cors().and().csrf().disable()

// AFTER
http
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(AbstractHttpConfigurer::disable)
```

---

## Issue 6: 403 Forbidden on all requests after migration

**Symptom:** Every request returns 403 even after the security config is updated.

**Common Causes:**

1. **CSRF not disabled:** Ensure `csrf(AbstractHttpConfigurer::disable)` is present for REST APIs.

2. **Filter order issue:** Custom JWT filter added but not positioned correctly:
   ```java
   http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
   ```

3. **`requestMatchers` pattern wrong:** Spring Security 6 with Spring MVC uses `MvcRequestMatcher`. Verify path pattern matches:
   ```java
   // If using Spring MVC, paths must match exactly (no trailing slash issues)
   .requestMatchers("/api/v1/auth/login").permitAll()
   // OR use pattern:
   .requestMatchers("/api/v1/auth/**").permitAll()
   ```

4. **Missing `SecurityFilterChain` bean:** Ensure `http.build()` is called and the result is returned as a `@Bean`.

---

## Issue 7: WebFlux Gateway — `csrf().disable()` method signature changed

**Error:**
```
[ERROR] The method disable() is undefined for type CsrfSpec
```

**Fix for WebFlux:**
```java
// BEFORE
http.csrf().disable()

// AFTER (WebFlux)
http.csrf(ServerHttpSecurity.CsrfSpec::disable)
```

---

## Issue 8: `HttpSecurity.apply()` removed

**Error:**
```
[ERROR] cannot find symbol: method apply(...)
```

**Root Cause:** `HttpSecurity.apply(AbstractHttpConfigurer)` for DSL customizers was removed.

**Fix:** Use `with()` instead:
```java
// BEFORE
http.apply(new MyCustomDsl());

// AFTER
http.with(new MyCustomDsl(), Customizer.withDefaults());
```

---

## Issue 9: 401 returned instead of 403 for missing role

**Symptom:** After migration, access-denied returns 401 instead of 403.

**Root Cause:** Spring Security 6 changed default exception handling. Without an `AuthenticationEntryPoint`, unauthenticated requests return 401 and forbidden requests return 403. If JWT filter doesn't set authentication context, the request appears unauthenticated (401) rather than forbidden (403).

**Fix:** Ensure JWT filter sets `SecurityContextHolder` when a valid token is present:
```java
SecurityContextHolder.getContext().setAuthentication(
    new UsernamePasswordAuthenticationToken(userId, null, authorities)
);
```
