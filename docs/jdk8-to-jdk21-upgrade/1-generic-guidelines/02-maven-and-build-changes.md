# 02 - Maven & Build Changes

> **Confluence Page:** Generic Guidelines / Maven & Build Changes
> **Owner:** Bhushan Gadekar

---

## Overview

Build changes must be applied to the **parent POM first**, then propagated to each service module. Compilation failures must be resolved before moving to runtime changes.

---

## 1. Parent POM Changes

### Java Version

```xml
<!-- BEFORE (pom.xml) -->
<properties>
    <java.version>1.8</java.version>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
</properties>

<!-- AFTER -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

> **Note:** Use `<maven.compiler.release>21</maven.compiler.release>` instead of source/target. The `release` flag enforces cross-compilation safety and is the JDK 9+ recommended approach.

---

### Spring Boot Parent

```xml
<!-- BEFORE -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<!-- AFTER -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
</parent>
```

---

### Spring Cloud BOM

```xml
<!-- BEFORE -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>2021.0.9</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- AFTER -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>2023.0.3</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

---

## 2. Maven Compiler Plugin

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>21</release>
        <annotationProcessorPaths>
            <!-- Lombok must come BEFORE MapStruct -->
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

> **Critical:** Lombok must be listed **before** MapStruct in `annotationProcessorPaths`. On JDK 21, incorrect ordering causes `Cannot find symbol` compilation errors on mapped fields.

---

## 3. Maven Surefire Plugin (Tests)

JDK 17+ module system requires explicit `--add-opens` for reflection-heavy tests (Mockito, Spring Test):

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

---

## 4. Docker Build Update

Update all service Dockerfiles:

```dockerfile
# BEFORE
FROM maven:3.8.8-eclipse-temurin-8 AS build
...
FROM eclipse-temurin:8-jre-alpine

# AFTER
FROM maven:3.9.9-eclipse-temurin-21 AS build
...
FROM eclipse-temurin:21-jre-alpine
```

---

## 5. Toolchain Configuration (Optional but Recommended)

If your CI/CD system has multiple JDKs, use Maven Toolchains to pin JDK 21:

**`~/.m2/toolchains.xml`:**
```xml
<toolchains>
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>21</version>
            <vendor>temurin</vendor>
        </provides>
        <configuration>
            <jdkHome>/path/to/jdk-21</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```

**`pom.xml`:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-toolchains-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals><goal>toolchain</goal></goals>
        </execution>
    </executions>
    <configuration>
        <toolchains>
            <jdk>
                <version>21</version>
                <vendor>temurin</vendor>
            </jdk>
        </toolchains>
    </configuration>
</plugin>
```

---

## 6. Spring Boot Configuration Migration Tool

Spring Boot provides a CLI tool to auto-migrate deprecated properties:

```bash
# Install Spring Boot CLI
brew install spring-boot

# Or use the Maven plugin
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.config.additional-location=classpath:/migration/"

# Better: use the properties migrator dependency (add temporarily)
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>
```

> Remove `spring-boot-properties-migrator` once all property warnings are resolved.

---

## 7. Validation: Compilation Check

Run this sequence after parent POM changes:

```bash
# Step 1: Compile commons first (shared library)
mvn -pl commons clean compile -e

# Step 2: Compile all modules
mvn clean compile -e 2>&1 | grep -E "ERROR|WARNING|BUILD"

# Step 3: Full build with tests
mvn clean install
```

Expected output after successful migration:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX:XX min
```

---

## Common Compilation Errors After POM Change

| Error | Cause | Fix |
|---|---|---|
| `package javax.servlet does not exist` | javax→jakarta | Replace `javax.servlet` with `jakarta.servlet` |
| `package javax.validation does not exist` | javax→jakarta | Replace `javax.validation` with `jakarta.validation` |
| `package javax.persistence does not exist` | javax→jakarta | Replace `javax.persistence` with `jakarta.persistence` |
| `Cannot find symbol: @Data` | Lombok not in annotationProcessorPaths | Add Lombok to compiler plugin paths |
| `error: release version 21 not supported` | Old maven-compiler-plugin | Upgrade to 3.13.0 |
