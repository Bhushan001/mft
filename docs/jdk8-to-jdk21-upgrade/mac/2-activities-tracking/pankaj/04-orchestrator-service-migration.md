# Task 4 — Orchestrator Service Migration

> **Owner:** Pankaj
> **Sprint:** 4 — Week 4
> **Status:** Pending
> **Module:** `backend/orchestrator-service`
> **Complexity:** Medium

---

## Scope

Orchestrator service handles:
- Saga-pattern workflow execution (loan processing, ETL orchestration)
- Calls engine-service and etl-service via Feign
- Async execution via `@Async("orchestratorExecutor")`
- Returns 202 ACCEPTED immediately; client polls for status

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/orchestrator-service/src -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +

grep -rn "import javax\." backend/orchestrator-service/src/ --include="*.java"
```

---

### Step 2: Security Config Update

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
```

---

### Step 3: Async Configuration

`@Async` and `@EnableAsync` are unchanged. Verify the thread pool executor bean:

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("orchestratorExecutor")
    public Executor orchestratorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("orchestrator-");
        executor.initialize();
        return executor;
    }
}
```

No changes needed here — `ThreadPoolTaskExecutor` is unchanged.

---

### Step 4: Feign Clients (Engine + ETL)

```java
// EngineServiceClient — no change to interface
@FeignClient(name = "engine-service", fallback = EngineServiceClientFallback.class)
public interface EngineServiceClient {
    @PostMapping("/api/v1/engine/process")
    ApiResponse<ProcessResponseDto> process(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestHeader("X-User-Role") String role,
        @RequestBody ProcessRequestDto request);
}

// EtlServiceClient — no change to interface
@FeignClient(name = "etl-service", fallback = EtlServiceClientFallback.class)
public interface EtlServiceClient {
    @PostMapping("/api/v1/etl/jobs/submit")
    ApiResponse<EtlJobResponse> submitJob(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestBody SubmitEtlJobRequest request);
}
```

---

### Step 5: Workflow and WorkflowStep Entities

```java
import jakarta.persistence.*;

@Entity
@Table(name = "workflows", schema = "chrono_orchestrator")
public class Workflow extends BaseEntity { ... }

@Entity
@Table(name = "workflow_steps", schema = "chrono_orchestrator")
public class WorkflowStep extends BaseEntity { ... }
```

---

### Step 6: Build and Test

```bash
mvn -pl orchestrator-service clean install
mvn -pl orchestrator-service test

# Smoke test: submit workflow
curl -X POST http://localhost:8086/api/v1/workflows \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -H "X-User-Id: user-001" \
  -d '{"type":"ETL_ORCHESTRATION","correlationId":"corr-001"}'
# Expected: 202 Accepted

# Poll status
curl http://localhost:8086/api/v1/workflows/{workflowId} \
  -H "X-Tenant-Id: tenant-123"
# Expected: {"status":"IN_PROGRESS"} or {"status":"COMPLETED"}
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `mvn -pl orchestrator-service clean install` succeeds
- [ ] Security config updated
- [ ] Async executor bean still works
- [ ] Feign clients to engine-service and etl-service functional
- [ ] 202 ACCEPTED returned on workflow submission
- [ ] All unit tests pass

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
