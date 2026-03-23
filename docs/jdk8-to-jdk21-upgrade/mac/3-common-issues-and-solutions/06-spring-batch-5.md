# 06 - Spring Batch 5 Issues

> **Service:** etl-service only

---

## Issue 1: `JobBuilderFactory` not found

**Error:**
```
[ERROR] cannot find symbol: class JobBuilderFactory
```

**Root Cause:** `JobBuilderFactory` was deprecated in Spring Batch 4.3 and **removed** in Spring Batch 5.

**Fix:**
```java
// BEFORE
@Autowired
private JobBuilderFactory jobBuilderFactory;

@Bean
public Job myJob() {
    return jobBuilderFactory.get("myJob").start(myStep()).build();
}

// AFTER
@Bean
public Job myJob(JobRepository jobRepository, Step myStep) {
    return new JobBuilder("myJob", jobRepository)
        .start(myStep)
        .build();
}
```

---

## Issue 2: `StepBuilderFactory` not found

**Error:**
```
[ERROR] cannot find symbol: class StepBuilderFactory
```

**Root Cause:** Same — `StepBuilderFactory` removed in Spring Batch 5.

**Fix:**
```java
// BEFORE
@Autowired
private StepBuilderFactory stepBuilderFactory;

@Bean
public Step myStep() {
    return stepBuilderFactory.get("myStep")
        .<Input, Output>chunk(100)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
}

// AFTER
@Bean
public Step myStep(JobRepository jobRepository,
                   PlatformTransactionManager transactionManager) {
    return new StepBuilder("myStep", jobRepository)
        .<Input, Output>chunk(100, transactionManager)  // transactionManager required now
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
}
```

---

## Issue 3: `chunk(100)` compilation error — missing argument

**Error:**
```
[ERROR] The method chunk(int) is undefined for the type StepBuilder
```

**Root Cause:** Spring Batch 5 requires a `PlatformTransactionManager` argument in `chunk()`.

**Fix:**
```java
// BEFORE
.<Input, Output>chunk(100)

// AFTER
.<Input, Output>chunk(100, transactionManager)
```

---

## Issue 4: `SimpleJobLauncher` not found / deprecated

**Error:**
```
[ERROR] cannot find symbol: class SimpleJobLauncher
```

**Root Cause:** `SimpleJobLauncher` renamed to `TaskExecutorJobLauncher` in Spring Batch 5.

**Fix:**
```java
// BEFORE
SimpleJobLauncher launcher = new SimpleJobLauncher();
launcher.setJobRepository(jobRepository);
launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());

// AFTER
TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
launcher.setJobRepository(jobRepository);
launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
launcher.afterPropertiesSet();
```

---

## Issue 5: `@EnableBatchProcessing` breaks auto-configuration

**Symptom:** Application fails to start with:
```
The bean 'jobRepository', defined in class path resource [...], could not be registered.
```

**Root Cause:** In Spring Boot 3 + Spring Batch 5, `@EnableBatchProcessing` **disables** Spring Boot's Batch auto-configuration. They conflict.

**Fix — Option A (Recommended):** Remove `@EnableBatchProcessing`. Spring Boot 3 auto-configures Batch:
```java
// REMOVE this annotation
@EnableBatchProcessing  // DELETE
@Configuration
public class EtlJobConfig { }
```

**Fix — Option B:** If you need custom Batch config, extend `DefaultBatchConfiguration`:
```java
@Configuration
public class CustomBatchConfig extends DefaultBatchConfiguration {

    @Autowired
    private DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        return dataSource;
    }
}
```

---

## Issue 6: Spring Batch 5 schema changes — `BATCH_JOB_EXECUTION_PARAMS` column change

**Symptom:** Liquibase fails or job parameters can't be stored:
```
Column 'STRING_VAL' not found in schema 'chrono_etl'
```

**Root Cause:** Spring Batch 5 changed the `BATCH_JOB_EXECUTION_PARAMS` table schema. The old `STRING_VAL`, `LONG_VAL`, `DOUBLE_VAL`, `DATE_VAL` columns are replaced by a single `PARAMETER_VALUE` column.

**Fix — Option A:** Let Spring Boot recreate the schema:
```yaml
spring:
  batch:
    jdbc:
      initialize-schema: always  # recreates BATCH_* tables
```

> ⚠️ This drops and recreates Batch tables. Ensure no production job history is in the old schema.

**Fix — Option B:** Manually migrate with Liquibase changeset using Spring Batch 5 schema:
```sql
-- Drop old BATCH_JOB_EXECUTION_PARAMS and recreate with new schema
DROP TABLE IF EXISTS chrono_etl.BATCH_JOB_EXECUTION_PARAMS;

CREATE TABLE chrono_etl.BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT NOT NULL,
    PARAMETER_NAME   VARCHAR(100) NOT NULL,
    PARAMETER_TYPE   VARCHAR(100) NOT NULL,
    PARAMETER_VALUE  VARCHAR(2500),
    IDENTIFYING      CHAR(1) NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES chrono_etl.BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);
```

---

## Issue 7: `JobParameters` builder API change

**Error:**
```
[ERROR] cannot find symbol: method addString(String, String)
```

**Root Cause:** `JobParametersBuilder` API changed in Spring Batch 5.

**Fix:**
```java
// BEFORE
JobParameters params = new JobParametersBuilder()
    .addString("tenantId", tenantId)
    .addString("sourceRef", sourceRef)
    .addDate("batchDate", date)
    .toJobParameters();

// AFTER — same, but addDate changed
JobParameters params = new JobParametersBuilder()
    .addString("tenantId", tenantId)
    .addString("sourceRef", sourceRef)
    .addLocalDate("batchDate", localDate)  // addDate(Date) → addLocalDate(LocalDate)
    .toJobParameters();
```

---

## Issue 8: `ItemReader` throws `NonTransientResourceException` unexpectedly

**Symptom:** Batch job fails with `NonTransientResourceException` at step start.

**Root Cause:** In Spring Batch 5, the reader/processor/writer must be thread-safe if using multi-threaded steps. If `@StepScope` beans are shared, they may throw this.

**Fix:** Ensure readers are `@StepScope`:
```java
@Bean
@StepScope
public ItemReader<EtlRecord> itemReader(
        @Value("#{jobParameters['tenantId']}") String tenantId) {
    // return scoped reader
}
```
