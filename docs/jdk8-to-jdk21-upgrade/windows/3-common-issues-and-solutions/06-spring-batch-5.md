# 06 - Spring Batch 5 Issues (Windows)

> All Spring Batch 5 code changes are **identical to Mac** — Java code is OS-independent.

Please refer to the [Mac version](../../mac/3-common-issues-and-solutions/06-spring-batch-5.md) for all issues and fixes.

---

## Windows Tip: Find Spring Batch 4 Patterns

```powershell
# Find JobBuilderFactory / StepBuilderFactory usage (removed in Batch 5)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "JobBuilderFactory|StepBuilderFactory"

# Find @EnableBatchProcessing (conflicts with Spring Boot 3 auto-config)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "@EnableBatchProcessing"

# Find SimpleJobLauncher (renamed to TaskExecutorJobLauncher)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "SimpleJobLauncher"

# Find old chunk() without transaction manager
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "\.chunk\(\d+"
```

---

## Key Spring Batch 5 Fixes (Summary)

| Removed | Replace With |
|---|---|
| `JobBuilderFactory.get("name")` | `new JobBuilder("name", jobRepository)` |
| `StepBuilderFactory.get("name")` | `new StepBuilder("name", jobRepository)` |
| `.chunk(100)` | `.chunk(100, transactionManager)` |
| `@EnableBatchProcessing` | Remove (Spring Boot 3 auto-configures) |
| `SimpleJobLauncher` | `TaskExecutorJobLauncher` |
| `addDate(Date)` in JobParameters | `addLocalDate(LocalDate)` |
