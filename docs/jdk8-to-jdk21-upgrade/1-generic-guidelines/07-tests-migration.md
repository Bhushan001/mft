# 07 - Tests Migration

> **Confluence Page:** Generic Guidelines / Tests Migration
> **Owner:** All team members

---

## Overview

Spring Boot 3.x uses JUnit 5 exclusively. Spring Boot 2.7.x already included JUnit 5 by default, so most tests should compile without change. However, several patterns need updating.

---

## JUnit 5 Alignment

### Verify No JUnit 4 Usage

Spring Boot 3 removes the JUnit 4 `vintage` engine by default. Check for JUnit 4:

```bash
# Find JUnit 4 imports
grep -rn "import org.junit.Test" backend/ --include="*.java"
grep -rn "import org.junit.Before" backend/ --include="*.java"
grep -rn "@RunWith" backend/ --include="*.java"
```

If found, migrate to JUnit 5:

```java
// BEFORE — JUnit 4
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {
    @Before
    public void setUp() { ... }

    @Test
    public void shouldLogin() { ... }
}

// AFTER — JUnit 5
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @BeforeEach
    void setUp() { ... }

    @Test
    void shouldLogin() { ... }
}
```

---

## MockMvc: `javax` → `jakarta`

Any test using `MockMvc` that imports servlet classes must update:

```java
// BEFORE
import javax.servlet.http.HttpServletRequest;

// AFTER
import jakarta.servlet.http.HttpServletRequest;
```

---

## Spring Boot Test: Security Context

```java
// BEFORE
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testUser", roles = {"TENANT_ADMIN"})
    void shouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/1")
                .header("X-Tenant-Id", "tenant-123"))
            .andExpect(status().isOk());
    }
}

// AFTER — same pattern works; just ensure jakarta imports
```

---

## Spring Data JPA Tests

```java
// BEFORE
@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
}

// AFTER — same annotations; Hibernate 6 may cause some query test failures
// See Common Issues page for Hibernate 6 query syntax issues
```

---

## Mockito 5.x on JDK 21

Spring Boot 3.x ships with Mockito 5.x. Key changes:

```java
// Mockito 5 — stricter about unused stubs by default
// This will FAIL if stubbing is not used in a test:
when(repo.findById(anyLong())).thenReturn(Optional.of(entity));
// ... but test never calls repo.findById()

// Fix: either use the stub, or use @MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyServiceTest { }
```

---

## TestContainers (Recommended for Integration Tests)

Add TestContainers for integration tests against real PostgreSQL and Redis:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.20.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.20.1</version>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@Testcontainers
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("chrono_auth")
        .withUsername("postgres")
        .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldRegisterAndLoginUser() {
        // full stack test against real DB
    }
}
```

---

## Test Slices Reference

| Annotation | Tests | Loads |
|---|---|---|
| `@SpringBootTest` | Full integration | Entire context |
| `@WebMvcTest` | Controller layer | Web layer only |
| `@DataJpaTest` | Repository layer | JPA + in-memory DB |
| `@JsonTest` | JSON serialization | Jackson only |
| `@MockBean` | Mock Spring bean | Any slice |

---

## AssertJ — Preferred Assertion Style

```java
// Use AssertJ (included in Spring Boot Test)
import static org.assertj.core.api.Assertions.*;

assertThat(result).isNotNull();
assertThat(result.getUserId()).isEqualTo("uuid-123");
assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
assertThatThrownBy(() -> service.login(invalidRequest))
    .isInstanceOf(BusinessException.class)
    .hasMessageContaining("Invalid credentials");
```

---

## Test Naming Convention

Use descriptive test names (already in use in this project):

```java
@Test
void loginShouldSucceed_whenCredentialsAreValid() { }

@Test
void loginShouldThrowBusinessException_whenAccountIsLocked() { }

@Test
void loginShouldIncrementFailedAttempts_whenPasswordIsWrong() { }
```

---

## Migration Checklist: Tests

- [ ] No JUnit 4 imports (`org.junit.Test`, `@RunWith`) remaining
- [ ] All `javax.servlet` → `jakarta.servlet` in test files
- [ ] All tests pass with Mockito 5 strict stubs (or `LENIENT` where needed)
- [ ] `@DataJpaTest` tests pass with Hibernate 6 (check HQL syntax)
- [ ] `@SpringBootTest` context loads without errors
- [ ] MockMvc tests return expected status codes
- [ ] (Recommended) Add TestContainers for at least auth-service integration test
- [ ] Test coverage does not regress after migration
