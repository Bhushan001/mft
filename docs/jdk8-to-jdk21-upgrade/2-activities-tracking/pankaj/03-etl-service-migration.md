# Task 3 — ETL Service Migration

> **Owner:** Pankaj
> **Sprint:** 4 — Week 4
> **Status:** Pending
> **Module:** `backend/etl-service`
> **Complexity:** HIGH — Spring Batch 5 has major breaking changes

---

## Scope

ETL service handles:
- Spring Batch job submission and execution for bulk data import
- Calls engine-service via Feign for each record transformation
- Idempotency via `(tenantId, sourceRef, batchDate)` unique constraint
- Async execution via `@Async("etlExecutor")`

---

## ⚠️ Spring Batch 5 — Major Breaking Changes

Spring Boot 3 ships with **Spring Batch 5** which removed `JobBuilderFactory` and `StepBuilderFactory`. This requires a **rewrite** of all job/step configurations.

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/etl-service/src -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g;
     s/import javax\.batch\./import jakarta.batch./g' {} +

grep -rn "import javax\." backend/etl-service/src/ --include="*.java"
```

---

### Step 2: Rewrite Spring Batch Job Configuration

This is the main work. Every `@Bean` Job and Step definition must change:

```java
// BEFORE — Spring Batch 4 (Spring Boot 2.7)
@Configuration
@EnableBatchProcessing
public class EtlJobConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job etlJob() {
        return jobBuilderFactory.get("etlJob")
            .start(etlStep())
            .build();
    }

    @Bean
    public Step etlStep() {
        return stepBuilderFactory.get("etlStep")
            .<EtlRecord, ProcessedRecord>chunk(100)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .build();
    }
}

// AFTER — Spring Batch 5 (Spring Boot 3.x)
@Configuration
// NOTE: @EnableBatchProcessing is OPTIONAL in Spring Boot 3 — Spring Boot auto-configures Batch
public class EtlJobConfig {

    // Inject JobRepository and PlatformTransactionManager directly
    @Bean
    public Job etlJob(JobRepository jobRepository,
                      Step etlStep) {
        return new JobBuilder("etlJob", jobRepository)
            .start(etlStep)
            .build();
    }

    @Bean
    public Step etlStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("etlStep", jobRepository)
            .<EtlRecord, ProcessedRecord>chunk(100, transactionManager)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .build();
    }
}
```

> **Key differences:**
> - `JobBuilderFactory` → `new JobBuilder(name, jobRepository)`
> - `StepBuilderFactory` → `new StepBuilder(name, jobRepository)`
> - `chunk(10)` → `chunk(10, transactionManager)` (transaction manager now required)
> - `@EnableBatchProcessing` no longer needed (Spring Boot auto-configures it)

---

### Step 3: JobRepository Auto-Configuration

In Spring Batch 5, Spring Boot auto-creates `JobRepository` if you don't use `@EnableBatchProcessing`. If you need custom config:

```java
// Only if you need to customize JobRepository
@Configuration
public class BatchConfig implements BatchConfigurer {
    // Spring Batch 5 uses DefaultBatchConfiguration instead of BatchConfigurer
}

// AFTER — Spring Batch 5 way
@Configuration
public class BatchConfig extends DefaultBatchConfiguration {

    @Override
    protected DataSource getDataSource() {
        return dataSource;  // inject your DataSource
    }
}
```

---

### Step 4: Spring Batch 5 — Schema Changes

Spring Batch 5 changed the `BATCH_*` table schema. If you use Liquibase for Batch schema, update the SQL:

```yaml
# application.yml — Spring Batch schema initialization
spring:
  batch:
    jdbc:
      initialize-schema: always  # creates BATCH_* tables automatically
    job:
      enabled: false  # don't auto-run jobs on startup
```

Or update Liquibase changeset to use Spring Batch 5 schema:
- Old schema: `schema-postgresql.sql` from Batch 4
- New schema: Use `spring.batch.jdbc.initialize-schema=always` to let Spring manage it

---

### Step 5: JobLauncher for Async Execution

```java
// BEFORE
@Autowired
private JobLauncher jobLauncher;

// To run async:
SimpleJobLauncher launcher = (SimpleJobLauncher) jobLauncher;
launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());

// AFTER — Spring Batch 5
@Bean
public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
    TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
    launcher.setJobRepository(jobRepository);
    launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
    launcher.afterPropertiesSet();
    return launcher;
}
```

> `SimpleJobLauncher` → `TaskExecutorJobLauncher` in Spring Batch 5.

---

### Step 6: ItemReader / ItemProcessor / ItemWriter

These are largely unchanged. Verify imports:

```java
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ExecutionContext;
// No javax → jakarta change needed for these (Spring Batch classes)
```

---

### Step 7: Redis Property Update

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:redis}
```

---

### Step 8: Build and Test

```bash
mvn -pl etl-service clean install

# Run tests — Spring Batch tests need JobRepository
mvn -pl etl-service test

# Smoke test: submit a job
curl -X POST http://localhost:8087/api/v1/etl/jobs/submit \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -H "X-User-Id: user-001" \
  -d '{"sourceRef":"batch-001","batchDate":"2024-01-01"}'
# Expected: 202 Accepted with jobId

# Poll job status
curl http://localhost:8087/api/v1/etl/jobs/{jobId} \
  -H "X-Tenant-Id: tenant-123"
# Expected: {"status":"COMPLETED"} or {"status":"RUNNING"}
```

---

## Common Spring Batch 5 Issues

See [Common Issues — Spring Batch 5](../../3-common-issues-and-solutions/06-spring-batch-5.md)

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] All `JobBuilderFactory`/`StepBuilderFactory` usages removed
- [ ] Jobs use `new JobBuilder(name, jobRepository)` pattern
- [ ] Steps use `chunk(size, transactionManager)` pattern
- [ ] `@EnableBatchProcessing` removed (or replaced with `DefaultBatchConfiguration`)
- [ ] `SimpleJobLauncher` → `TaskExecutorJobLauncher`
- [ ] `mvn -pl etl-service clean install` succeeds
- [ ] Job submission returns 202, status polling works
- [ ] All unit tests pass

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
