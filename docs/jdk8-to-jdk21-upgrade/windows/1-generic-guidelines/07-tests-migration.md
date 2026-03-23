# 07 - Tests Migration (Windows)

> **Note:** All test code changes are **identical** to Mac. JUnit 5, Mockito, and Spring Boot Test are OS-independent.

Please refer to the [Mac version](../../mac/1-generic-guidelines/07-tests-migration.md) for all code examples.

---

## Windows Tip: Find JUnit 4 Usage

```powershell
# Find JUnit 4 imports (need migration to JUnit 5)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "import org\.junit\.Test|import org\.junit\.Before|@RunWith"

# Find @RunWith(MockitoJUnitRunner) (JUnit 4 style)
Get-ChildItem -Path . -Filter *.java -Recurse |
    Select-String "@RunWith"
```

---

## Running Tests on Windows

```powershell
# Run tests for a single module
mvn -pl auth-service test

# Run tests with verbose output
mvn -pl auth-service test -Dsurefire.useFile=false

# Run a specific test class
mvn -pl auth-service test -Dtest=AuthServiceTest

# Run tests and capture output to file
mvn -pl auth-service test 2>&1 | Out-File test-results.txt
```

---

## TestContainers on Windows

TestContainers requires Docker Desktop to be running:

1. Install Docker Desktop for Windows
2. Ensure Docker Desktop is **running** before running integration tests
3. TestContainers automatically detects Docker Desktop

```powershell
# Verify Docker is running
docker info

# Run integration tests
mvn -pl auth-service test -Dtest=AuthServiceIntegrationTest
```

---

## Migration Checklist

- [ ] No JUnit 4 imports remaining
- [ ] All `javax.servlet` → `jakarta.servlet` in test files
- [ ] Tests pass with Mockito 5
- [ ] `@DataJpaTest` tests pass with Hibernate 6
- [ ] Docker Desktop running before TestContainers tests
