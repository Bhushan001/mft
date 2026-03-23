# 02 - Compilation Errors

---

## Issue 1: `error: release version 21 not supported`

**Error:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.1:compile
error: release version 21 not supported
```

**Root Cause:** Old `maven-compiler-plugin` (3.8.x or earlier) does not support `--release 21`.

**Fix:** Upgrade compiler plugin in parent POM:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>21</release>
    </configuration>
</plugin>
```

---

## Issue 2: `Cannot find symbol @Builder` / Lombok annotations not processed

**Error:**
```
[ERROR] .../UserProfileDto.java:[12,5] cannot find symbol
  symbol: method builder()
```

**Root Cause:** Lombok is not configured as an annotation processor in the Maven compiler plugin.

**Fix:** Add Lombok to `annotationProcessorPaths` **before** MapStruct:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>21</release>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## Issue 3: JJWT `parseClaimsJws` / `parserBuilder` not found

**Error:**
```
[ERROR] cannot find symbol: method parserBuilder()
[ERROR] cannot find symbol: method parseClaimsJws(String)
[ERROR] cannot find symbol: method getBody()
```

**Root Cause:** JJWT 0.12.x renamed these methods.

**Fix:**
```java
// BEFORE (0.11.x)
Claims claims = Jwts.parserBuilder()
    .setSigningKey(key).build()
    .parseClaimsJws(token).getBody();

// AFTER (0.12.x)
Claims claims = Jwts.parser()
    .verifyWith(secretKey).build()
    .parseSignedClaims(token).getPayload();
```

Also update token builder:
```java
// BEFORE
Jwts.builder().setSubject(id).signWith(key, SignatureAlgorithm.HS256).compact()

// AFTER
Jwts.builder().subject(id).signWith(key).compact()
```

**Services affected:** auth-service, api-gateway (JwtUtil class).

---

## Issue 4: `InaccessibleObjectException` at test runtime

**Error:**
```
java.lang.reflect.InaccessibleObjectException: Unable to make
  field private final java.lang.String java.lang.String.value accessible
```

**Root Cause:** JDK 17+ enforces strong module encapsulation. Mockito/Spring use deep reflection internally.

**Fix:** Add JVM args to `maven-surefire-plugin`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <argLine>
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.util=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
        </argLine>
    </configuration>
</plugin>
```

> With modern Mockito 5.x and Spring Boot 3.x, this is usually not needed. Only add if you see this error.

---

## Issue 5: `HttpStatus` vs `HttpStatusCode` in `ResponseEntityExceptionHandler`

**Error:**
```
[ERROR] method does not override or implement a method from a supertype
  handleMethodArgumentNotValid(MethodArgumentNotValidException, HttpHeaders, HttpStatus, WebRequest)
```

**Root Cause:** Spring 6 changed the method signature — `HttpStatus` → `HttpStatusCode` (an interface).

**Fix:**
```java
// BEFORE
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers, HttpStatus status, WebRequest request) {

// AFTER
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    // HttpStatusCode is the interface; HttpStatus implements it
    // Cast if needed: HttpStatus httpStatus = HttpStatus.valueOf(status.value())
```

**Services affected:** `commons` — `GlobalExceptionHandler`.

---

## Issue 6: MapStruct `Cannot find symbol` for mapped fields

**Error:**
```
[ERROR] .../UserMapper.java: cannot find symbol
  symbol: method getFirstName()
```

**Root Cause:** MapStruct processed before Lombok — getter methods not yet generated when MapStruct runs.

**Fix:** Ensure Lombok is listed **before** MapStruct in `annotationProcessorPaths` (see Issue 2 above).

---

## Issue 7: `SignatureAlgorithm` import not found (JJWT 0.12.x)

**Error:**
```
[ERROR] cannot find symbol: variable SignatureAlgorithm.HS256
```

**Root Cause:** In JJWT 0.12.x, when using `SecretKey`, the algorithm is inferred automatically. No need to pass `SignatureAlgorithm`.

**Fix:**
```java
// REMOVE SignatureAlgorithm import and usage
// BEFORE
.signWith(key, SignatureAlgorithm.HS256)

// AFTER
.signWith(key)  // algorithm inferred from key type
```

---

## Issue 8: `spring.config.import` ConfigDataLocationNotFoundException

**Error:**
```
ConfigDataLocationNotFoundException: Config data location 'configserver:http://config-server:8888'
cannot be found
```

**Root Cause:** Config server not running when application starts.

**Fix:** Use `optional:` prefix to make config server optional:
```yaml
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
```

Or set `spring.cloud.config.fail-fast=false`.
