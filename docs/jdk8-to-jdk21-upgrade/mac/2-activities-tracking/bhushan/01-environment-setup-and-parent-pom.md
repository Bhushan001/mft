# Task 1 — Environment Setup & Parent POM

> **Owner:** Bhushan Gadekar
> **Sprint:** 1 — Week 1
> **Status:** Pending

---

## Objective

Set up the JDK 21 development environment, create the migration branch, and upgrade the parent POM. This unblocks all other team members.

---

## Step-by-Step Tasks

### Step 1: Install JDK 21

```bash
# macOS (Homebrew)
brew install --cask temurin@21

# Verify
java -version
# Expected: openjdk version "21.x.x" 2023-...

# Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
echo $JAVA_HOME
```

For Linux (Ubuntu):
```bash
sudo apt-get install -y temurin-21-jdk
export JAVA_HOME=/usr/lib/jvm/temurin-21
```

---

### Step 2: Install Maven 3.9.x

```bash
# Verify current Maven version
mvn -version
# Must be Apache Maven 3.9.x for Spring Boot 3 compatibility

# macOS
brew install maven

# Linux
wget https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
tar -xvf apache-maven-3.9.9-bin.tar.gz
export PATH=$HOME/apache-maven-3.9.9/bin:$PATH
```

---

### Step 3: Create Migration Branch

```bash
# Tag current JDK 8 baseline
git tag jdk8-baseline
git push origin jdk8-baseline

# Create migration branch
git checkout -b jdk21-migration
git push -u origin jdk21-migration
```

---

### Step 4: Dependency Audit (Run First)

```bash
# Save current dependency tree as baseline
cd backend/
for module in commons auth-service user-service tenant-service mapper-service engine-service etl-service orchestrator-service audit-service api-gateway; do
  mvn -pl $module dependency:tree > ../docs/jdk8-to-jdk21-upgrade/baseline-deps-$module.txt 2>&1
done

# Count javax imports
grep -rn "import javax\." --include="*.java" | wc -l

# List all javax imports
grep -rn "import javax\." --include="*.java" > ../docs/jdk8-to-jdk21-upgrade/javax-imports-baseline.txt
```

---

### Step 5: Upgrade Parent POM

File: `backend/pom.xml`

**Changes:**
1. Spring Boot parent: `2.7.18` → `3.3.4`
2. Spring Cloud BOM: `2021.0.9` → `2023.0.3`
3. Java version: `1.8` → `21`
4. Maven compiler release: add `<maven.compiler.release>21</maven.compiler.release>`
5. Dependency versions: Lombok `1.18.34`, MapStruct `1.6.2.Final`, JJWT `0.12.6`, Resilience4j `2.2.0`
6. Maven Compiler Plugin: upgrade to `3.13.0` with annotationProcessorPaths
7. Maven Surefire Plugin: upgrade to `3.3.1` with `--add-opens` JVM args

**Validation:**
```bash
# Compile commons only first (no runtime deps)
mvn -pl commons clean compile -e

# Expected: BUILD SUCCESS
# If javax errors appear → proceed to commons migration (Task 2)
```

---

### Step 6: Share Updated POM with Team

Once parent POM compiles `commons` successfully:
1. Push `jdk21-migration` branch
2. Notify Sakshi and Pankaj to rebase their service branches on this
3. Share the [Generic Guidelines — Maven & Build Changes](../../1-generic-guidelines/02-maven-and-build-changes.md) page

---

## Completion Criteria

- [ ] `java -version` shows JDK 21
- [ ] `mvn -version` shows Maven 3.9.x
- [ ] `jdk21-migration` branch created and pushed
- [ ] Javax import count saved to baseline file
- [ ] `mvn -pl commons clean compile` succeeds with new parent POM
- [ ] Parent POM changes committed and branch pushed
- [ ] Team notified to start their service migrations

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
