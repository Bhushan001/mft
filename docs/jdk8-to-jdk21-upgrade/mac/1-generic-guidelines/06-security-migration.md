# 06 - Security Migration (Spring Security 6.x)

> **Confluence Page:** Generic Guidelines / Security Migration
> **Owner:** Bhushan (gateway), Sakshi (auth, user)

---

## Overview

Spring Security 6 (shipped with Spring Boot 3.x) has **breaking changes** that require code rewrites in all services with security configuration. The core concept is the same, but the API is significantly different.

---

## Breaking Change #1: `WebSecurityConfigurerAdapter` Removed

This is the most impactful change. Every service that extends `WebSecurityConfigurerAdapter` must be rewritten.

```java
// BEFORE — Spring Security 5 (Spring Boot 2.7)
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
```

```java
// AFTER — Spring Security 6 (Spring Boot 3.x)
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Breaking Change #2: `antMatchers` → `requestMatchers`

```java
// BEFORE
.authorizeRequests()
    .antMatchers("/api/v1/auth/**").permitAll()
    .antMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
    .anyRequest().authenticated()

// AFTER
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
    .anyRequest().authenticated())
```

> `antMatchers()`, `mvcMatchers()`, and `regexMatchers()` are all removed. Use `requestMatchers()`.

---

## Breaking Change #3: `authorizeRequests` → `authorizeHttpRequests`

```java
// BEFORE
.authorizeRequests().antMatchers(...)

// AFTER
.authorizeHttpRequests(auth -> auth.requestMatchers(...))
```

---

## Breaking Change #4: `cors()` and `csrf()` Fluent API

```java
// BEFORE
http.cors().and().csrf().disable()

// AFTER
http
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(AbstractHttpConfigurer::disable)
```

---

## Spring Security 6: API Gateway (WebFlux)

The api-gateway uses **Spring WebFlux** (reactive). The reactive security API also changed:

```java
// BEFORE — Spring Security 5 WebFlux
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable()
            .authorizeExchange()
                .pathMatchers("/api/v1/auth/**").permitAll()
                .anyExchange().authenticated()
            .and()
            .build();
    }
}

// AFTER — Spring Security 6 WebFlux
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/v1/auth/**").permitAll()
                .anyExchange().authenticated())
            .build();
    }
}
```

---

## JWT Filter Migration

Custom JWT filters need package import updates:

```java
// BEFORE
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

// AFTER
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
```

---

## Role/Authority Changes

Spring Security 6 tightened role prefix handling:

```java
// BEFORE — roles with or without ROLE_ prefix were handled inconsistently
.hasRole("TENANT_ADMIN")         // Spring auto-prepends ROLE_
.hasAuthority("ROLE_TENANT_ADMIN") // Explicit ROLE_ prefix

// AFTER — same behavior, but be explicit and consistent
// Use hasRole() for roles stored without ROLE_ prefix
// Use hasAuthority() for full authority strings
.hasRole("TENANT_ADMIN")
// OR
.hasAuthority("ROLE_TENANT_ADMIN")
```

---

## OAuth2 / JWT Resource Server (If Applicable)

If any service validates JWTs using Spring's built-in OAuth2 resource server (not custom filter):

```java
// AFTER
http
    .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt.decoder(jwtDecoder())));
```

---

## Password Encoder

No change — `BCryptPasswordEncoder` is still the recommended encoder:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

---

## Security Test Changes

```java
// BEFORE
@WebMvcTest
@WithMockUser(roles = "TENANT_ADMIN")
class UserControllerTest {
    // ...
}

// AFTER — same annotations work, but import changes
import org.springframework.security.test.context.support.WithMockUser;
// No change needed for test annotations
```

```java
// BEFORE — MockMvc security setup
mockMvc = MockMvcBuilders
    .webAppContextSetup(context)
    .apply(springSecurity())
    .build();

// AFTER — same, ensure spring-security-test is on classpath
```

---

## Migration Checklist: Security

- [ ] Remove all `WebSecurityConfigurerAdapter` extensions
- [ ] Replace with `@Bean SecurityFilterChain` pattern
- [ ] Replace `antMatchers` → `requestMatchers`
- [ ] Replace `authorizeRequests` → `authorizeHttpRequests`
- [ ] Replace `cors().and().csrf().disable()` with lambda style
- [ ] Update JWT filter: `javax.servlet.*` → `jakarta.servlet.*`
- [ ] Update WebFlux security config for api-gateway
- [ ] Verify login endpoint returns 200 with valid credentials
- [ ] Verify invalid token returns 401
- [ ] Verify role-based access works correctly
