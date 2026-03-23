# 04 - Hibernate 6.x Issues (Windows)

> All Hibernate 6 code changes are **identical to Mac** — Java/YAML code is OS-independent.

Please refer to the [Mac version](../../mac/3-common-issues-and-solutions/04-hibernate-6x.md) for all issues and fixes.

---

## Windows Tip: Find Hibernate 5 Patterns

```powershell
# Find @Type annotation usage (needs migration in Hibernate 6)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "@Type\("

# Find positional ?1 parameters in JPQL (stricter in Hibernate 6)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "\?1|\?2|\?3"

# Find deprecated dialect configuration in YML
Get-ChildItem -Path . -Filter "*.yml" -Recurse |
    Select-String "hibernate\.dialect"
```

---

## Key Hibernate 6 Fixes (Summary)

| Issue | Fix |
|---|---|
| `@Type(type = "json")` | `@JdbcTypeCode(SqlTypes.JSON)` |
| `?1` positional params | Use `:namedParam` + `@Param` |
| Explicit dialect in yml | Remove — auto-detected |
| `MultipleBagFetchException` | Use `Set` instead of `List` for `@OneToMany` |
