# Identity Service Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up `identity-service` — a new, independent Spring Boot service owning authentication, authorization, user management, and RBAC — as a lift-and-shift of the existing `vetautet` code, runnable on its own DB/Redis, with a JWKS endpoint and two internal APIs so other services can eventually integrate with it.

**Architecture:** Same hexagonal layering as `vetautet` (`domain → application → infrastructure/presentation`), new root package `com.platform.identity`. Extraction proceeds bottom-up through the layers (domain → shared → application → infrastructure → presentation) so each task's `mvn compile` checkpoint is meaningful — by the time presentation is copied, the whole app compiles. **Deviation from the usual step template:** because this is a mechanical port of already-tested code (not new feature work), "port" tasks replace the write-failing-test/implement cycle with copy → rename package → `mvn compile`; a dedicated task near the end ports the existing (small) test suite and runs it green. Tasks that add genuinely new code (JWKS endpoint, internal APIs, log-only notification sender) do follow the normal write-code-then-verify shape, since there's nothing to copy.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Maven, PostgreSQL, Redis + Redisson, Liquibase, MapStruct, Lombok, springdoc-openapi, Nimbus JOSE/JWT (via `spring-boot-starter-security-oauth2-resource-server`).

**Spec:** `docs/superpowers/specs/2026-08-22-identity-service-extraction-design.md`

## Global Constraints

- New project root: `/Users/tigerpro/Documents/AI/vmware-ai/vetautet-app/identity-service` (sibling of `vetautet`), own git repo, remote `https://github.com/duyhoangptit/identity-service.git`.
- Root package: `com.platform.identity`. Maven `groupId=com.platform`, `artifactId=identity-service`.
- Parent `spring-boot-starter-parent:4.1.0`, `java.version=21` — same stack as `vetautet`.
- Server port `8081` (avoid colliding with `vetautet`'s `8080`).
- Own Postgres + own Redis (docker-compose in the new repo). **No Kafka** — nothing in the copied scope needs it.
- Lift-and-shift: copy/adapt existing code as-is. No redesign of domain model, JWT scheme, or RBAC data model.
- `vetautet` is **not modified** in this plan. No FK drops, no code removal there.
- `AuthNotificationSender` gets a genuinely log-only implementation in identity-service (new code, not copied) — no outbox, no Kafka.
- Any pagination-returning repository call must go through `PageableSanitizer.capped(pageable)` at the persistence-adapter boundary; `PageableSanitizer.MAX_PAGE_SIZE = 100` is the one authoritative cap (per `vetautet`'s `CLAUDE.md` pagination rule — carried over verbatim since this is lift-and-shift, not a redesign).
- Fresh empty DB: schema + seed data only, no real data migration.

Two path variables used throughout this plan:
- `SRC` = `/Users/tigerpro/Documents/AI/vmware-ai/vetautet-app/vetautet`
- `DST` = `/Users/tigerpro/Documents/AI/vmware-ai/vetautet-app/identity-service`

---

### Task 1: Scaffold the Maven project

**Files:**
- Create: `$DST/pom.xml`
- Create: `$DST/src/main/java/com/platform/identity/IdentityServiceApplication.java`
- Create: `$DST/.gitignore`
- Create: `$DST/README.md`
- Create: `$DST/Dockerfile`

**Interfaces:**
- Produces: a Maven project that builds an empty Spring Boot app; every later task adds source under `$DST/src/main/java/com/platform/identity/...` and compiles against this `pom.xml`.

- [ ] **Step 1: Create the project skeleton and git repo**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity" "$DST/src/main/resources" "$DST/src/test/java/com/platform/identity"
cd "$DST"
git init -q
git remote add origin https://github.com/duyhoangptit/identity-service.git
```

- [ ] **Step 2: Write `pom.xml`** (full final dependency set up front, so every later task's `mvn compile` just works — no Kafka, no session-data-redis; both confirmed unused by the copied auth/user/rbac scope)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>
    <groupId>com.platform</groupId>
    <artifactId>identity-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>identity-service</name>
    <description>Authentication, authorization, user management and RBAC — shared platform service</description>

    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <redisson.version>4.4.0</redisson.version>
        <postgresql.version>42.7.7</postgresql.version>
        <liquibase.skip>false</liquibase.skip>
        <spotless.version>3.9.0</spotless.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-liquibase</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>${redisson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>8.1</version>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.7.0</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains</groupId>
            <artifactId>annotations</artifactId>
            <version>26.1.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>properties-maven-plugin</artifactId>
                <version>1.2.1</version>
                <executions>
                    <execution>
                        <id>load-dotenv-properties</id>
                        <phase>initialize</phase>
                        <goals>
                            <goal>read-project-properties</goal>
                        </goals>
                        <configuration>
                            <files>
                                <file>${project.basedir}/.env.properties</file>
                            </files>
                            <quiet>true</quiet>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
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
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok-mapstruct-binding.version}</version>
                        </path>
                    </annotationProcessorPaths>
                    <compilerArgs>
                        <arg>-parameters</arg>
                        <arg>-Amapstruct.defaultComponentModel=spring</arg>
                        <arg>-Amapstruct.unmappedTargetPolicy=IGNORE</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>com.diffplug.spotless</groupId>
                <artifactId>spotless-maven-plugin</artifactId>
                <version>${spotless.version}</version>
                <configuration>
                    <java>
                        <googleJavaFormat/>
                        <removeUnusedImports/>
                        <formatAnnotations/>
                    </java>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.liquibase</groupId>
                <artifactId>liquibase-maven-plugin</artifactId>
                <configuration>
                    <changeLogFile>src/main/resources/db/changelog/db.changelog-master.yaml</changeLogFile>
                    <driver>org.postgresql.Driver</driver>
                    <url>${APP_DB_URL}</url>
                    <username>${APP_DB_USERNAME}</username>
                    <password>${APP_DB_PASSWORD}</password>
                    <defaultSchemaName>public</defaultSchemaName>
                    <skip>${liquibase.skip}</skip>
                    <promptOnNonLocalDatabase>false</promptOnNonLocalDatabase>
                    <outputChangeLogFile>src/main/resources/liquibase-outputChangeLog.xml</outputChangeLogFile>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.postgresql</groupId>
                        <artifactId>postgresql</artifactId>
                        <version>${postgresql.version}</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <id>run-liquibase-before-boot</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>update</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Write the application entry point**

```java
package com.platform.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: `.gitignore`** (same as `vetautet`'s, adapted)

```gitignore
HELP.md
target/
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### VS Code ###
.vscode/

/logs/
.env.properties
```

- [ ] **Step 5: `Dockerfile`** (same multi-stage pattern as `vetautet`, port 8081)

```dockerfile
# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -Dliquibase.skip=true -B -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p logs && chown -R appuser:appgroup logs

COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

- [ ] **Step 6: `README.md`**

```markdown
# identity-service

Authentication, authorization, user management and RBAC, extracted from
`vetautet` into an independent platform service. Extraction design/plan:
see `vetautet`'s `docs/superpowers/specs/2026-08-22-identity-service-extraction-design.md`.

## Run locally

```bash
docker compose -f docker-compose-dev.yml up -d
cp .env.properties.example .env.properties   # fill in local values
mvn spring-boot:run
```

Service listens on `http://localhost:8081`. Swagger UI at `/swagger-ui.html`.
```

- [ ] **Step 7: Verify it compiles**

Run: `cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile`
Expected: `BUILD SUCCESS` (a single class, no other source yet).

- [ ] **Step 8: Commit**

```bash
cd "$DST"
git add -A
git commit -q -m "chore: scaffold identity-service Maven project"
```

---

### Task 2: Local infra — docker-compose, env config

**Files:**
- Create: `$DST/docker-compose-dev.yml`
- Create: `$DST/.env.properties.example`

**Interfaces:**
- Produces: a local Postgres on `5433:5432` and Redis on `6380:6379`, plus the `APP_DB_URL`/`APP_DB_USERNAME`/`APP_DB_PASSWORD`/`APP_REDIS_HOST`/`APP_REDIS_PORT` keys every later task's `application.yml` reads via `${...}` placeholders (Task 13).

- [ ] **Step 1: Write `docker-compose-dev.yml`** (own Postgres + Redis only — no Kafka, no observability stack; ports offset from `vetautet`'s so both can run side by side)

```yaml
version: '3.8'

networks:
  identity-network:
    driver: bridge

services:
  postgres:
    image: postgres:16-alpine
    container_name: identity-postgres
    environment:
      POSTGRES_DB: identity
      POSTGRES_USER: identity
      POSTGRES_PASSWORD: identity_local_dev
    ports:
      - "5433:5432"
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
    networks:
      - identity-network

  redis:
    image: redis:7-alpine
    container_name: identity-redis
    ports:
      - "6380:6379"
    networks:
      - identity-network
```

- [ ] **Step 2: Write `.env.properties.example`** (copied at setup time to `.env.properties`, which is gitignored)

```properties
APP_DB_URL=jdbc:postgresql://localhost:5433/identity
APP_DB_USERNAME=identity
APP_DB_PASSWORD=identity_local_dev
APP_REDIS_HOST=localhost
APP_REDIS_PORT=6380
APP_REDIS_PASSWORD=
SERVER_PORT=8081
APP_AUTH_FRONTEND_BASE_URL=http://localhost:3000
INTERNAL_API_KEY=local-dev-internal-key-change-me
```

- [ ] **Step 3: Bring up local infra and verify**

```bash
cd "$DST"
docker compose -f docker-compose-dev.yml up -d
docker compose -f docker-compose-dev.yml ps
```

Expected: both `identity-postgres` and `identity-redis` show `Up`/healthy.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -q -m "chore: add local docker-compose infra (postgres, redis)"
```

---

### Task 3: Database schema — Liquibase changelogs

**Files:**
- Create: `$DST/src/main/resources/db/changelog/db.changelog-master.yaml`
- Create: `$DST/src/main/resources/db/changelog/changes/001-initial-schema-postgresql.sql` (users table only — `member_info` is explicitly excluded, see below)
- Create: `$DST/src/main/resources/db/changelog/changes/002-align-user-and-rsa-schema.sql`
- Create: `$DST/src/main/resources/db/changelog/changes/003-create-auth-flow-and-otp-tables.sql`
- Create: `$DST/src/main/resources/db/changelog/changes/004-inital-schema-for-rbac.sql`
- Create: `$DST/src/main/resources/db/changelog/changes/005-alter-table-otp-session.sql`
- Create: `$DST/src/main/resources/db/changelog/changes/006-rbac-master-data.sql`
- Create: `$DST/src/main/resources/db/changelog/changes/007-add-login-lockout-to-users.sql`

- [ ] **Step 1: Copy the source changelogs**

```bash
mkdir -p "$DST/src/main/resources/db/changelog/changes"
cp "$SRC/src/main/resources/db/changelog/changes/001-initial-schema-postgresql.sql" \
   "$DST/src/main/resources/db/changelog/changes/001-initial-schema-postgresql.sql"
cp "$SRC/src/main/resources/db/changelog/changes/004-align-user-and-rsa-schema.sql" \
   "$DST/src/main/resources/db/changelog/changes/002-align-user-and-rsa-schema.sql"
cp "$SRC/src/main/resources/db/changelog/changes/005-create-auth-flow-and-otp-tables.sql" \
   "$DST/src/main/resources/db/changelog/changes/003-create-auth-flow-and-otp-tables.sql"
cp "$SRC/src/main/resources/db/changelog/changes/012-inital-schema-for-rbac.sql" \
   "$DST/src/main/resources/db/changelog/changes/004-inital-schema-for-rbac.sql"
cp "$SRC/src/main/resources/db/changelog/changes/013-alter-table-otp-session.sql" \
   "$DST/src/main/resources/db/changelog/changes/005-alter-table-otp-session.sql"
cp "$SRC/src/main/resources/db/changelog/changes/014-rbac-master-data.sql" \
   "$DST/src/main/resources/db/changelog/changes/006-rbac-master-data.sql"
cp "$SRC/src/main/resources/db/changelog/changes/015-add-login-lockout-to-users.sql" \
   "$DST/src/main/resources/db/changelog/changes/007-add-login-lockout-to-users.sql"
```

- [ ] **Step 2: Open `001-initial-schema-postgresql.sql` and remove everything except the `users` table definition.** `member_info` is explicitly **excluded** — it holds business-specific membership data (policy number, member company, member number, dependent number) that belongs to a future, separate customer-service domain, not to a platform-wide identity service — so drop the `member_info` table, the `fk_member_info_user` FK, and any other table/FK referencing `booking_orders` or other out-of-scope domains. Check the file's full content first (`cat` it) since it was `vetautet`'s all-in-one initial schema; keep only what the `users` table needs to exist and be valid, standalone Postgres DDL on its own.

- [ ] **Step 3: Write the master changelog**

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-initial-schema-postgresql.sql
  - include:
      file: db/changelog/changes/002-align-user-and-rsa-schema.sql
  - include:
      file: db/changelog/changes/003-create-auth-flow-and-otp-tables.sql
  - include:
      file: db/changelog/changes/004-inital-schema-for-rbac.sql
  - include:
      file: db/changelog/changes/005-alter-table-otp-session.sql
  - include:
      file: db/changelog/changes/006-rbac-master-data.sql
  - include:
      file: db/changelog/changes/007-add-login-lockout-to-users.sql
```

- [ ] **Step 4: Run Liquibase against the local Postgres from Task 2 and verify**

```bash
cd "$DST"
export $(grep -v '^#' .env.properties | xargs)
mvn -q liquibase:update
docker exec identity-postgres psql -U identity -d identity -c "\dt"
```

Expected: `\dt` lists `users`, `member_info`, `rsa_key_pairs`, `auth_flow_tokens`, `otp_sessions`, `roles`, `permissions`, `endpoints`, `portals` (or equivalent RBAC table names — confirm against what `012-inital-schema-for-rbac.sql` actually creates) with no errors during `liquibase:update`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -q -m "feat: port auth/user/rbac Liquibase schema and seed data"
```

**Amendment (discovered during Task 13's live boot test, added post-execution):**
this task's original 7-file changelog selection above missed two of
vetautet's changelogs that later tasks' Java code actually depends on:
`002-add-password-hash.sql` (adds `users.password_hash`, which
`UserJpaEntity` — Task 7 — expects) and `003-create-idempotency-records.sql`
(creates `idempotency_records`, which `IdempotencyRecordJpaEntity`/
`IdempotencyAspect` — Tasks 7/9 — expect). Both were added as new,
additive changesets (`008-add-password-hash.sql`,
`009-create-idempotency-records.sql`, appended to the master changelog) once
found, rather than folded into the original numbering, since renumbering
would have meant re-keying already-applied changesets. If executing this
plan fresh (not resuming a partially-done run), fold these two directly
into the original Step 1 file list instead of adding them as an afterthought
— see the SDD ledger's "Real bug found post-review" entry for the exact SQL
content and the reasoning for why appending (not reordering) was safe here.

---

### Task 4: Domain layer — auth, user, RBAC models

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/domain/auth/**` (mirrors `$SRC/.../domain/auth/**`)
- Create: `$DST/src/main/java/com/platform/identity/domain/user/**` (mirrors `$SRC/.../domain/user/**`)

**Interfaces:**
- Produces: `User`, `UserId`, `Email`, `PersonalInfo`, `UserStatus`, `Role`, `Endpoint`, `DuplicateEmailException`, `Token`, `KeyId`, `PublicKey`, `RsaKeyPair`, `AuthFlowToken`, `AuthFlowTokenStatus`, `AuthFlowType`, `OtpSession`, `OtpSessionStatus`, plus repository interfaces `UserRepository`, `EndpointRepository`, `RsaKeyPairRepository`, `AuthFlowTokenRepository`, `OtpSessionRepository`, and domain services `UserDomainService`, `AuthDomainService` — all under `com.platform.identity.domain.*`, consumed by every later task.

- [ ] **Step 1: Copy the two domain subtrees**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/domain"
cp -R "$SRC/src/main/java/com/vetautet/app/domain/auth" "$DST/src/main/java/com/platform/identity/domain/auth"
cp -R "$SRC/src/main/java/com/vetautet/app/domain/user" "$DST/src/main/java/com/platform/identity/domain/user"
```

- [ ] **Step 2: Rewrite the package prefix on every copied file**

```bash
find "$DST/src/main/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
```

(This exact command is reused, unmodified, at the end of every remaining copy task — safe to re-run, it's a no-op on files already renamed.)

- [ ] **Step 3: Compile**

Run: `cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -q -m "feat: port domain layer (auth, user, RBAC models)"
```

---

### Task 5: Shared/common layer

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/shared/common/util/**`
- Create: `$DST/src/main/java/com/platform/identity/shared/common/exception/**`
- Create: `$DST/src/main/java/com/platform/identity/shared/common/context/**`
- Create: `$DST/src/main/java/com/platform/identity/shared/common/logging/**`

**Interfaces:**
- Consumes: nothing from earlier tasks (this layer has no dependency on `domain`).
- Produces: `PageableSanitizer.capped(Pageable)` / `PageableSanitizer.MAX_PAGE_SIZE`, `AppLogicException`, `ErrorCode`, `DuplicateResourceException`, `ResourceNotFoundException`, `RateLimitExceededException`, `IdempotencyConflictException`, `IdempotencyInProgressException`, `RequestIdContext`, `MessageResolver`, `AuthUtil`, `HashUtil`, `Uuid7Generator`, `SingleFlight`, `JsonUtil`, `MaskUtil`, `SensitiveDataMasker`, `MaskingMessageConverter` — used throughout `application`/`infrastructure`/`presentation`.

- [ ] **Step 1: Copy util, context, logging as-is (nothing domain-specific to trim)**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/shared/common"
cp -R "$SRC/src/main/java/com/vetautet/app/shared/common/util" "$DST/src/main/java/com/platform/identity/shared/common/util"
cp -R "$SRC/src/main/java/com/vetautet/app/shared/common/context" "$DST/src/main/java/com/platform/identity/shared/common/context"
cp -R "$SRC/src/main/java/com/vetautet/app/shared/common/logging" "$DST/src/main/java/com/platform/identity/shared/common/logging"
mkdir -p "$DST/src/main/java/com/platform/identity/shared/common/exception"
for f in AppLogicException DuplicateResourceException IdempotencyConflictException IdempotencyInProgressException RateLimitExceededException ResourceNotFoundException; do
  cp "$SRC/src/main/java/com/vetautet/app/shared/common/exception/$f.java" \
     "$DST/src/main/java/com/platform/identity/shared/common/exception/$f.java"
done
```

- [ ] **Step 2: Write a trimmed `ErrorCode`** — drops entries for domains not in scope (`NOTIFICATION_TEMPLATE_NOT_FOUND`, `INVALID_ORDER_CURSOR`), keeps everything auth/user/RBAC/idempotency/rate-limit related:

```java
package com.platform.identity.shared.common.exception;

import lombok.Getter;

/**
 * Enum defining error codes for the application.
 * Messages are externalized in i18n properties files.
 */
@Getter
public enum ErrorCode {

    // Resource errors (1xxx)
    RESOURCE_NOT_FOUND("ERR-1001"),
    USER_NOT_FOUND("ERR-1002"),
    USER_NOT_FOUND_BY_EMAIL("ERR-1003"),
    RSA_KEY_PAIR_NOT_FOUND("ERR-1004"),
    AUTH_FLOW_TOKEN_NOT_FOUND("ERR-1005"),
    OTP_SESSION_NOT_FOUND("ERR-1006"),

    // Duplicate resource errors (2xxx)
    DUPLICATE_RESOURCE("ERR-2001"),
    DUPLICATE_EMAIL("ERR-2002"),
    DUPLICATE_USERNAME("ERR-2003"),

    // Validation errors (3xxx)
    INVALID_INPUT("ERR-3001"),
    INVALID_EMAIL_FORMAT("ERR-3002"),
    INVALID_USER_STATE("ERR-3003"),
    INVALID_STATUS_TRANSITION("ERR-3004"),
    INVALID_OTP("ERR-3005"),
    OTP_EXPIRED("ERR-3006"),
    OTP_MAX_ATTEMPTS_EXCEEDED("ERR-3007"),
    INVALID_AUTH_FLOW_TOKEN("ERR-3008"),
    AUTH_FLOW_TOKEN_EXPIRED("ERR-3009"),

    // Authentication/Authorization errors (4xxx)
    AUTHENTICATION_FAILED("ERR-4001"),
    INVALID_CREDENTIALS("ERR-4002"),
    TOKEN_EXPIRED("ERR-4003"),
    TOKEN_INVALID("ERR-4004"),
    UNAUTHORIZED_ACCESS("ERR-4005"),
    OTP_REQUIRED("ERR-4006"),
    RATE_LIMIT_EXCEEDED("ERR-4007"),
    ACCOUNT_LOCKED("ERR-4008"),

    // Business logic errors (5xxx)
    BUSINESS_RULE_VIOLATION("ERR-5001"),
    OPERATION_NOT_ALLOWED("ERR-5002"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS("ERR-5003"),
    IDEMPOTENCY_KEY_REUSED("ERR-5004"),

    // System errors (9xxx)
    INTERNAL_SERVER_ERROR("ERR-9001"),
    DATABASE_ERROR("ERR-9002"),
    EXTERNAL_SERVICE_ERROR("ERR-9003");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }
}
```

- [ ] **Step 3: Rename packages and compile**

```bash
find "$DST/src/main/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -q -m "feat: port shared/common layer (util, exceptions, context, logging)"
```

---

### Task 6: Application layer — auth and user use cases

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/application/auth/**`
- Create: `$DST/src/main/java/com/platform/identity/application/user/**`

**Interfaces:**
- Consumes: domain types from Task 4 (`User`, `RsaKeyPair`, `Token`, repository interfaces), shared types from Task 5 (`AppLogicException`, `ErrorCode`).
- Produces: input ports `LoginUseCase`, `RegisterUseCase`, `ActivateAccountUseCase`, `ForgotPasswordUseCase`, `ResetPasswordUseCase`, `VerifyOtpLoginUseCase`, `LogoutUseCase`, `CreateUserUseCase`, `UpdateUserUseCase`, `GetUserUseCase`, `DeleteUserUseCase`, `CheckUserAvailabilityUseCase`; output ports `JwtTokenGenerator`, `PasswordHashEncoder`, `OtpCodeGenerator`, `RsaKeyPairGenerator`, `RoleRepository`, `AuthorizationQuery`, `AuthNotificationSender`, `UserAvailabilityProbe` — all implemented by Task 7/8/9's infrastructure adapters.

- [ ] **Step 1: Copy both application subtrees**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/application"
cp -R "$SRC/src/main/java/com/vetautet/app/application/auth" "$DST/src/main/java/com/platform/identity/application/auth"
cp -R "$SRC/src/main/java/com/vetautet/app/application/user" "$DST/src/main/java/com/platform/identity/application/user"
```

- [ ] **Step 2: Rename packages**

```bash
find "$DST/src/main/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
```

- [ ] **Step 3: Strip the `member_info`-related fields — out of scope (see spec's "Excluded — `member_info`").** `member_info` is business-specific membership data belonging to a future, separate customer-service domain, not identity-service. Remove `memberPolicyNumber`, `memberCompanyId`, `memberNumber`, `dependentNumber` from:
  - `$DST/src/main/java/com/platform/identity/application/user/dto/CreateUserCommand.java`
  - `$DST/src/main/java/com/platform/identity/application/user/dto/UpdateUserCommand.java`
  - `$DST/src/main/java/com/platform/identity/application/user/dto/UserDto.java`

  And remove the corresponding `.memberPolicyNumber(...)`/`.memberCompanyId(...)`/`.memberNumber(...)`/`.dependentNumber(...)` builder calls in `$DST/src/main/java/com/platform/identity/application/user/mapper/UserApplicationMapper.java`. This is a safe removal with no further blast radius: in `vetautet`, none of these four fields ever reach `UserJpaEntity` or the domain `User` model — `UserEntityMapper` never reads them — so nothing downstream (Task 7's persistence layer) needs any corresponding change.

- [ ] **Step 4: Compile**

Run: `cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile`
Expected: **fails** with "package com.platform.identity.infrastructure... does not exist" is NOT expected (application layer only depends on domain + shared, both already present) — expect `BUILD SUCCESS`. If it fails on a missing symbol, it means some application-layer class reaches into infrastructure/presentation directly (a layering violation) or references an out-of-scope domain (booking/notification/messaging) — resolve by checking what the missing import actually is via `grep -rn "import com.vetautet.app" "$SRC/src/main/java/com/vetautet/app/application/auth" "$SRC/src/main/java/com/vetautet/app/application/user"` before this copy and confirming every import target was already copied in Task 4/5.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -q -m "feat: port application layer (auth and user use cases)"
```

---

### Task 7: Infrastructure — persistence (JPA)

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/entity/{BaseJpaEntity,UserJpaEntity,RsaKeyPairJpaEntity,AuthFlowTokenJpaEntity,OtpSessionJpaEntity,RoleJpaEntity,EndpointJpaEntity,PermissionJpaEntity,PortalJpaEntity,IdempotencyRecordJpaEntity}.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/repository/{UserJpaRepository,RsaKeyPairJpaRepository,AuthFlowTokenJpaRepository,OtpSessionJpaRepository,RoleJpaRepository,EndpointJpaRepository,IdempotencyRecordJpaRepository}.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/adapter/{UserRepositoryAdapter,RsaKeyPairRepositoryAdapter,AuthFlowTokenRepositoryAdapter,OtpSessionRepositoryAdapter,RoleRepositoryAdapter,EndpointRepositoryAdapter}.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/persistence/mapper/{UserEntityMapper,RsaKeyPairEntityMapper,AuthFlowTokenEntityMapper,OtpSessionEntityMapper,EndpointEntityMapper}.java`

**Interfaces:**
- Consumes: domain repository interfaces from Task 4, `PageableSanitizer` from Task 5.
- Produces: Spring-managed beans implementing every domain repository interface, so Task 6's use cases resolve at runtime (not needed for `mvn compile`, needed once Spring context is exercised in Task 13).

- [ ] **Step 1: Copy the in-scope entities**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/entity"
for f in BaseJpaEntity UserJpaEntity RsaKeyPairJpaEntity AuthFlowTokenJpaEntity OtpSessionJpaEntity RoleJpaEntity EndpointJpaEntity PermissionJpaEntity PortalJpaEntity IdempotencyRecordJpaEntity; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/persistence/jpa/entity/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/entity/$f.java"
done
```

- [ ] **Step 2: Copy the in-scope Spring Data repositories**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/repository"
for f in UserJpaRepository RsaKeyPairJpaRepository AuthFlowTokenJpaRepository OtpSessionJpaRepository RoleJpaRepository EndpointJpaRepository IdempotencyRecordJpaRepository; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/persistence/jpa/repository/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/repository/$f.java"
done
```

- [ ] **Step 3: Copy the adapters that implement the domain repository ports**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/adapter"
for f in UserRepositoryAdapter RsaKeyPairRepositoryAdapter AuthFlowTokenRepositoryAdapter OtpSessionRepositoryAdapter RoleRepositoryAdapter EndpointRepositoryAdapter; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/persistence/jpa/adapter/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/adapter/$f.java"
done
```

- [ ] **Step 4: Copy the entity mappers**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/persistence/mapper"
for f in UserEntityMapper RsaKeyPairEntityMapper AuthFlowTokenEntityMapper OtpSessionEntityMapper EndpointEntityMapper; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/persistence/mapper/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/persistence/mapper/$f.java"
done
```

- [ ] **Step 5: Confirm `UserRepositoryAdapter`'s paginated methods still call `PageableSanitizer.capped(...)`** — open the copied file and check `findAll`/`searchByKeyword` (or equivalents) wrap their `Pageable` argument with `PageableSanitizer.capped(pageable)` before calling into `jpaRepository`. This is already true in the source (it's `vetautet`'s reference implementation per its `CLAUDE.md`), so the copy should carry it over unchanged — this step is a verification, not an edit.

- [ ] **Step 6: Rename packages and compile**

```bash
find "$DST/src/main/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile
```

Expected: `BUILD SUCCESS`. If a `PermissionJpaEntity`/`PortalJpaEntity` reference is missing an adapter/repository (they may only be referenced from within `RoleJpaEntity`/`EndpointJpaEntity` as JPA relationships, not through their own top-level repository), that's expected — leave them as entities only unless the compiler says otherwise.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -q -m "feat: port JPA persistence layer (entities, repositories, adapters, mappers)"
```

---

### Task 8: Infrastructure — auth/security adapters

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/auth/{JwtTokenGeneratorAdapter,PasswordHashEncoderAdapter,RsaKeyPairGeneratorAdapter,RandomOtpCodeGeneratorAdapter,FixedOtpCodeGeneratorAdapter}.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/security/{CustomJwtAuthenticationConverter,MultiPortalAuthorizationManager}.java`

**Interfaces:**
- Consumes: `JwtTokenGenerator`, `PasswordHashEncoder`, `RsaKeyPairGenerator`, `OtpCodeGenerator` ports from Task 6; `AuthorizationQuery` from Task 6 (implemented in Task 9); `CustomJwtAuthenticationToken` from Task 6's `application.auth.service` package.
- Produces: the beans `SecurityConfig` (Task 11) wires into the filter chain.

- [ ] **Step 1: Copy**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/auth"
for f in JwtTokenGeneratorAdapter PasswordHashEncoderAdapter RsaKeyPairGeneratorAdapter RandomOtpCodeGeneratorAdapter FixedOtpCodeGeneratorAdapter; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/auth/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/auth/$f.java"
done
mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/security"
for f in CustomJwtAuthenticationConverter MultiPortalAuthorizationManager; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/security/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/security/$f.java"
done
```

- [ ] **Step 2: Rename packages and compile**

```bash
find "$DST/src/main/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -q -m "feat: port JWT/RSA/OTP infrastructure adapters and RBAC authorization manager"
```

---

### Task 9: Infrastructure — cache, rate-limit, idempotency, cross-cutting config

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/cache/{AuthorizationQueryImpl,CacheProperties,CustomCacheErrorHandler,RedisConfig,RedisService,RedissonConfig}.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/cache/bloomfilter/{BloomFilterSyncState,RedissonUserAvailabilityProbeAdapter,UserAvailabilityRedisKeys,UserBloomFilterConfig,UserBloomFilterSyncService}.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/ratelimit/{RateLimitAspect,RateLimited}.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/idempotency/{IdempotencyAspect,IdempotencyRecordService,IdempotencyStatus,Idempotent}.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/logging/ExecutionLoggingAspect.java`
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/config/{AuditingConfig,DataSourceConfig,I18nConfig,JacksonConfig,JpaConfig,TaskExecutionConfig}.java`

**Interfaces:**
- Consumes: `AuthorizationQuery` port from Task 6, `Role`/`Endpoint` domain types from Task 4, `RoleRepositoryAdapter`/`EndpointRepositoryAdapter` from Task 7.
- Produces: `AuthorizationQueryImpl` (implements `AuthorizationQuery`, backing `MultiPortalAuthorizationManager` from Task 8), Redis/Redisson beans, bloom filter beans used by `CheckUserAvailabilityUseCaseImpl`, `@RateLimited`/`@Idempotent` aspects used by controllers in Task 12.

- [ ] **Step 1: Copy cache, bloom filter, rate-limit, idempotency, logging, generic config**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/cache/bloomfilter"
for f in AuthorizationQueryImpl CacheProperties CustomCacheErrorHandler RedisConfig RedisService RedissonConfig; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/cache/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/cache/$f.java"
done
for f in BloomFilterSyncState RedissonUserAvailabilityProbeAdapter UserAvailabilityRedisKeys UserBloomFilterConfig UserBloomFilterSyncService; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/cache/bloomfilter/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/cache/bloomfilter/$f.java"
done

mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/ratelimit"
for f in RateLimitAspect RateLimited; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/ratelimit/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/ratelimit/$f.java"
done

mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/idempotency"
for f in IdempotencyAspect IdempotencyRecordService IdempotencyStatus Idempotent; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/idempotency/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/idempotency/$f.java"
done

mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/logging"
cp "$SRC/src/main/java/com/vetautet/app/infrastructure/logging/ExecutionLoggingAspect.java" \
   "$DST/src/main/java/com/platform/identity/infrastructure/logging/ExecutionLoggingAspect.java"

mkdir -p "$DST/src/main/java/com/platform/identity/infrastructure/config"
for f in AuditingConfig DataSourceConfig I18nConfig JacksonConfig JpaConfig TaskExecutionConfig; do
  cp "$SRC/src/main/java/com/vetautet/app/infrastructure/config/$f.java" \
     "$DST/src/main/java/com/platform/identity/infrastructure/config/$f.java"
done
```

- [ ] **Step 2: Rename packages and compile**

```bash
find "$DST/src/main/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -q -m "feat: port cache, rate-limit, idempotency, and cross-cutting infra config"
```

---

### Task 10: New — log-only `AuthNotificationSender`

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/infrastructure/notification/LoggingAuthNotificationSender.java`

**Interfaces:**
- Consumes: `AuthNotificationSender` port (`sendOtp`, `sendAuthFlowLink`) from Task 6's `application.auth.port.output` package, `AuthFlowType` from Task 4.
- Produces: the sole `AuthNotificationSender` bean — `AuthFlowLinkService`/`OtpService` (Task 6) autowire it.

This is genuinely new code (see spec's "Exception — `AuthNotificationSender`" section) — `vetautet`'s class of the same name is not a real log-only sender, it delegates to the full `notification` module which is out of scope.

- [ ] **Step 1: Write the class**

```java
package com.platform.identity.infrastructure.notification;

import com.platform.identity.application.auth.port.output.AuthNotificationSender;
import com.platform.identity.domain.auth.model.AuthFlowType;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Log-only implementation of {@link AuthNotificationSender}. This extraction
 * ships without a real email transport — OTP codes and activation/reset
 * links are logged at INFO instead of sent. Wiring a real transport (e.g.
 * an outbox + message broker, or direct SMTP) is a deliberate follow-up,
 * not part of this extraction (see the design spec's "Out of scope").
 */
@Slf4j
@Component
public class LoggingAuthNotificationSender implements AuthNotificationSender {

    @Override
    public void sendOtp(String email, AuthFlowType flowType, String otpCode) {
        log.info("[LOG-ONLY] OTP for flow={} recipient={} code={}", flowType, email, otpCode);
    }

    @Override
    public void sendAuthFlowLink(String email, AuthFlowType flowType, String link) {
        log.info("[LOG-ONLY] Auth flow link for flow={} recipient={} link={}", flowType, email, link);
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -q -m "feat: add log-only AuthNotificationSender (no email transport in this extraction)"
```

---

### Task 11: Presentation — security config, filters, exception handling

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/presentation/config/{SecurityConfig,OpenApiConfig,WebConfig,RequireBearerAuth}.java`
- Create: `$DST/src/main/java/com/platform/identity/presentation/config/ratelimit/{CaptchaVerifier,IpRateLimiter,LocalFallbackRateLimiter,NoOpCaptchaVerifier,RateLimit,RateLimitDecision,RateLimitInterceptor,RateLimitProperties,RateLimitResponseWriter}.java`
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/filter/{RequestIdFilter,AuthRateLimitFilter}.java`
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/exception/{ErrorResponse,GlobalExceptionHandler,ValidationErrorResponse}.java`

**Interfaces:**
- Consumes: `RsaKeyPairRepository` (Task 4), `CustomJwtAuthenticationConverter`/`MultiPortalAuthorizationManager` (Task 8), `PageableSanitizer` (Task 5), `RateLimitProperties`/`RateLimitInterceptor` (this task).
- Produces: the `SecurityFilterChain` and `JwtDecoder` beans — needed before any controller (Task 12) can be exercised at runtime.

- [ ] **Step 1: Copy config, ratelimit, filter, exception classes as-is**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/presentation/config/ratelimit"
for f in SecurityConfig OpenApiConfig WebConfig RequireBearerAuth; do
  cp "$SRC/src/main/java/com/vetautet/app/presentation/config/$f.java" \
     "$DST/src/main/java/com/platform/identity/presentation/config/$f.java"
done
for f in CaptchaVerifier IpRateLimiter LocalFallbackRateLimiter NoOpCaptchaVerifier RateLimit RateLimitDecision RateLimitInterceptor RateLimitProperties RateLimitResponseWriter; do
  cp "$SRC/src/main/java/com/vetautet/app/presentation/config/ratelimit/$f.java" \
     "$DST/src/main/java/com/platform/identity/presentation/config/ratelimit/$f.java"
done

mkdir -p "$DST/src/main/java/com/platform/identity/presentation/rest/filter"
for f in RequestIdFilter AuthRateLimitFilter; do
  cp "$SRC/src/main/java/com/vetautet/app/presentation/rest/filter/$f.java" \
     "$DST/src/main/java/com/platform/identity/presentation/rest/filter/$f.java"
done

mkdir -p "$DST/src/main/java/com/platform/identity/presentation/rest/exception"
for f in ErrorResponse GlobalExceptionHandler ValidationErrorResponse; do
  cp "$SRC/src/main/java/com/vetautet/app/presentation/rest/exception/$f.java" \
     "$DST/src/main/java/com/platform/identity/presentation/rest/exception/$f.java"
done
```

- [ ] **Step 2: Trim `SecurityConfig`'s `permitAll()` list** — open the copied file and remove the matchers that don't exist in identity-service: `/api/v1/departures/search`, `/api/v1/orders-demo`, `/internal/orders-demo/**`. Keep: `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/register/activate`, `/api/v1/auth/verify-otp-login`, `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password`, `/api/v1/users/availability`, the Swagger/OpenAPI matchers, and the actuator matchers. `.anyRequest().access(multiPortalAuthorizationManager)` and the `oauth2ResourceServer(...)` block stay unchanged. (The internal endpoints added in Task 14/15 get their own matcher, added when those tasks write their controllers.)

- [ ] **Step 3: Rename packages and compile**

```bash
find "$DST/src/main/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -q -m "feat: port security config, rate-limit filters, and global exception handling"
```

---

### Task 12: Presentation — controllers, DTOs, mappers

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/controller/v1/{AuthController,UserController,UserAvailabilityController}.java`
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/dto/request/{ActivateAccountRequest,ForgotPasswordRequest,LoginRequest,RegisterRequest,ResetPasswordRequest,VerifyOtpLoginRequest,CreateUserRequest,UpdateUserRequest}.java`
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/dto/response/{ActivateAccountResponse,AuthFlowLinkResponse,AvailabilityResponse,BaseResponse,OtpChallengeResponse,PageResponse,RegisterResponse,ResetPasswordResponse,SuccessResponse,TokenResponse,UserResponse}.java`
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/mapper/{AuthPresentationMapper,UserPresentationMapper}.java`

**Interfaces:**
- Consumes: every use case port from Task 6, `RequireBearerAuth`/`RateLimit`/`Idempotent` from Tasks 11/9.
- Produces: the full public API surface (`/api/v1/auth/**`, `/api/v1/users/**`). This is the checkpoint where `mvn compile` succeeds for the **entire** application, not just a layer.

- [ ] **Step 1: Copy controllers, DTOs, mappers**

```bash
mkdir -p "$DST/src/main/java/com/platform/identity/presentation/rest/controller/v1"
for f in AuthController UserController UserAvailabilityController; do
  cp "$SRC/src/main/java/com/vetautet/app/presentation/rest/controller/v1/$f.java" \
     "$DST/src/main/java/com/platform/identity/presentation/rest/controller/v1/$f.java"
done

mkdir -p "$DST/src/main/java/com/platform/identity/presentation/rest/dto/request"
for f in ActivateAccountRequest ForgotPasswordRequest LoginRequest RegisterRequest ResetPasswordRequest VerifyOtpLoginRequest CreateUserRequest UpdateUserRequest; do
  cp "$SRC/src/main/java/com/vetautet/app/presentation/rest/dto/request/$f.java" \
     "$DST/src/main/java/com/platform/identity/presentation/rest/dto/request/$f.java"
done

mkdir -p "$DST/src/main/java/com/platform/identity/presentation/rest/dto/response"
for f in ActivateAccountResponse AuthFlowLinkResponse AvailabilityResponse BaseResponse OtpChallengeResponse PageResponse RegisterResponse ResetPasswordResponse SuccessResponse TokenResponse UserResponse; do
  cp "$SRC/src/main/java/com/vetautet/app/presentation/rest/dto/response/$f.java" \
     "$DST/src/main/java/com/platform/identity/presentation/rest/dto/response/$f.java"
done

mkdir -p "$DST/src/main/java/com/platform/identity/presentation/rest/mapper"
for f in AuthPresentationMapper UserPresentationMapper; do
  cp "$SRC/src/main/java/com/vetautet/app/presentation/rest/mapper/$f.java" \
     "$DST/src/main/java/com/platform/identity/presentation/rest/mapper/$f.java"
done
```

- [ ] **Step 2: Strip the `member_info`-related fields — out of scope (see spec's "Excluded — `member_info`").** Same exclusion as Task 6. Remove `memberPolicyNumber`, `memberCompanyId`, `memberNumber`, `dependentNumber` (and their `@Size` validation annotations) from:
  - `$DST/src/main/java/com/platform/identity/presentation/rest/dto/request/CreateUserRequest.java`
  - `$DST/src/main/java/com/platform/identity/presentation/rest/dto/request/UpdateUserRequest.java`
  - `$DST/src/main/java/com/platform/identity/presentation/rest/dto/response/UserResponse.java`

  And remove the corresponding `.memberPolicyNumber(...)`/`.memberCompanyId(...)`/`.memberNumber(...)`/`.dependentNumber(...)` calls from every builder call in `$DST/src/main/java/com/platform/identity/presentation/rest/mapper/UserPresentationMapper.java` (`toCommand(CreateUserRequest)`, `toCommand(String, UpdateUserRequest)`, and `toResponse(UserDto)` all reference these fields today — check each one).

- [ ] **Step 3: Rename packages and compile**

```bash
find "$DST/src/main/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile
```

Expected: `BUILD SUCCESS` — this is the first point the whole application compiles.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -q -m "feat: port auth/user controllers, DTOs, and presentation mappers"
```

---

### Task 13: Resource config, boot the app, smoke-check health

**Files:**
- Create: `$DST/src/main/resources/application.yml`
- Create: `$DST/src/main/resources/i18n/{messages.properties,messages_en.properties,messages_vi.properties}` (copied verbatim — see note below)

**Interfaces:**
- Consumes: every `${...}` property referenced by beans copied in Tasks 8–12 (datasource, redis, cache TTLs, `app.auth.*`, `app.rate-limit.*`, `app.user-availability.*`, `idempotency.*`).
- Produces: a bootable application against the Task 2 docker-compose infra.

- [ ] **Step 1: Copy i18n message bundles as-is.** They contain keys for domains not present here (booking, payment) — harmless: `MessageResolver` only looks up keys it's asked for, unused entries in a `MessageSource` are not an error. Trimming them is optional cleanup, not required for correctness, so it's skipped in this extraction.

```bash
mkdir -p "$DST/src/main/resources/i18n"
cp "$SRC/src/main/resources/i18n/messages.properties" "$DST/src/main/resources/i18n/messages.properties"
cp "$SRC/src/main/resources/i18n/messages_en.properties" "$DST/src/main/resources/i18n/messages_en.properties"
cp "$SRC/src/main/resources/i18n/messages_vi.properties" "$DST/src/main/resources/i18n/messages_vi.properties"
```

- [ ] **Step 2: Write `application.yml`** — trimmed to what the copied scope actually uses: no `spring.kafka.*`, no `app.outbox.*`, no `app.kafka.*`, no `departureSearch`/`train-schedule`/`station`/`departure-search` cache TTLs, `server.port` defaults to `8081`, logging package changed to `com.platform.identity`, Hikari pool name changed:

```yaml
spring:
  threads:
    virtual:
      enabled: true
  application:
    name: identity-service
  config:
    import: optional:file:../../../dev.properties

  cache:
    redis:
      default-ttl: 3600
      caches:
        otp: 300
        portal-role-endpoints: 300
        user-session: 1800

  datasource:
    url: ${APP_DB_URL}
    username: ${APP_DB_USERNAME}
    password: ${APP_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    connection-fetch: lazy
    hikari:
      auto-commit: false
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: HikariPool-identity-service

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: public

  messages:
    basename: i18n/messages
    encoding: UTF-8
    fallback-to-system-locale: false

  data:
    redis:
      host: ${APP_REDIS_HOST}
      port: ${APP_REDIS_PORT}
      password: ${APP_REDIS_PASSWORD:}
      timeout: ${APP_REDIS_TIMEOUT_MS:2000}
      connect-timeout: ${APP_REDIS_CONNECT_TIMEOUT_MS:2000}
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
    web:
      pageable:
        default-page-size: 20
        max-page-size: 50

server:
  port: ${SERVER_PORT:8081}
  servlet:
    context-path: /
  tomcat:
    threads:
      max: 100
      min-spare: 10
      max-queue-capacity: 10000
  forward-headers-strategy: native

logging:
  level:
    root: INFO
    com.platform.identity: DEBUG
    org.springframework.security: INFO
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr([%thread]){magenta} %clr(%-5level) %clr([%X{requestId:-N/A}]){cyan} %clr(%logger{36}){yellow} %clr(-){faint} %msg%n%wEx"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{requestId:-N/A}] %logger{36} - %msg%n"
  file:
    name: logs/application.log

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

app:
  auth:
    frontend-base-url: ${APP_AUTH_FRONTEND_BASE_URL:http://localhost:3000}
    link-expiration-minutes: 15
    otp-expiration-minutes: 5
    otp-max-attempts: 5
    login-max-failed-attempts: 5
    login-lockout-minutes: 30
  logging:
    path: logs
    execution:
      slow-threshold-ms: ${APP_LOGGING_EXECUTION_SLOW_THRESHOLD_MS:1000}
    json:
      file:
        name: logs/application.json
      event:
        dataset: ${spring.application.name}.application
    rolling:
      max-file-size: 50MB
      max-history: 30
      total-size-cap: 5GB
    async:
      queue-size: 8192
      never-block: false
  rate-limit:
    enabled: ${APP_RATE_LIMIT_ENABLED:true}
    scopes:
      register:
        soft-limit-per-minute: 5
        hard-limit-per-hour: 20
        challenge-ttl-minutes: 30
        block-ttl-minutes: 120
      registerActivate:
        soft-limit-per-minute: 10
        hard-limit-per-hour: 30
        challenge-ttl-minutes: 15
        block-ttl-minutes: 60
      forgotPassword:
        soft-limit-per-minute: 5
        hard-limit-per-hour: 20
        challenge-ttl-minutes: 30
        block-ttl-minutes: 120
      resetPassword:
        soft-limit-per-minute: 5
        hard-limit-per-hour: 20
        challenge-ttl-minutes: 30
        block-ttl-minutes: 120
      availability:
        soft-limit-per-minute: 20
        hard-limit-per-hour: 200
        challenge-ttl-minutes: 15
        block-ttl-minutes: 60
  user-availability:
    bloom-filter:
      expected-insertions: 500000
      false-positive-probability: 0.01
      sync-batch-size: 500
      batch-delay-ms: 5
  internal-api:
    key: ${INTERNAL_API_KEY}

idempotency:
  lock:
    wait-time-ms: 100
    lease-time-ms: 5000
```

(`app.internal-api.key` is new — wired up and consumed starting Task 15.)

- [ ] **Step 3: Boot the app against the Task 2 infra**

```bash
cd "$DST"
docker compose -f docker-compose-dev.yml up -d
export $(grep -v '^#' .env.properties | xargs)
mvn -q spring-boot:run &
sleep 20
curl -sf http://localhost:8081/actuator/health
kill %1
```

Expected: the `curl` prints `{"status":"UP",...}`. If it doesn't come up, read the boot log for the first `ERROR`/`Caused by` line — a missing bean almost always means a file from Task 4–12 wasn't copied, or `SecurityConfig`'s trimmed `permitAll()` list (Task 11 Step 2) references a matcher class that no longer compiles.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -q -m "feat: add application.yml and i18n resources; app boots against local infra"
```

---

### Task 14: New — JWKS endpoint

**Files:**
- Modify: `$DST/src/main/java/com/platform/identity/domain/auth/repository/RsaKeyPairRepository.java` (add one method)
- Modify: `$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/adapter/RsaKeyPairRepositoryAdapter.java` (implement it)
- Modify: `$DST/src/main/java/com/platform/identity/infrastructure/persistence/jpa/repository/RsaKeyPairJpaRepository.java` (add the query)
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/controller/jwks/JwksController.java`
- Modify: `$DST/src/main/java/com/platform/identity/presentation/config/SecurityConfig.java` (permit `/.well-known/jwks.json`)

**Interfaces:**
- Consumes: `RsaKeyPairRepository.findAllValid(LocalDateTime now)` (new), `RsaKeyPair`/`PublicKey` from Task 4.
- Produces: `GET /.well-known/jwks.json` returning a standard JWK Set — any future resource server can point `NimbusJwtDecoder.jwkSetUri(...)` at this URL instead of querying identity-service's DB directly.

- [ ] **Step 1: Add `findAllValid` to the domain repository port**

In `RsaKeyPairRepository.java`, add:

```java
    List<RsaKeyPair> findAllValid(LocalDateTime now);
```

- [ ] **Step 2: Add the backing query to the Spring Data repository**

In `RsaKeyPairJpaRepository.java`, add:

```java
    @Query("SELECT k FROM RsaKeyPairJpaEntity k WHERE k.isActive = true "
            + "AND (k.expiresAt IS NULL OR k.expiresAt > :now)")
    List<RsaKeyPairJpaEntity> findAllValid(@Param("now") LocalDateTime now);
```

- [ ] **Step 3: Implement it on the adapter**

In `RsaKeyPairRepositoryAdapter.java`, add:

```java
    @Override
    @Transactional(readOnly = true)
    public List<RsaKeyPair> findAllValid(LocalDateTime now) {
        return jpaRepository.findAllValid(now)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
```

- [ ] **Step 4: Write `JwksController`**

```java
package com.platform.identity.presentation.rest.controller.jwks;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.platform.identity.domain.auth.model.RsaKeyPair;
import com.platform.identity.domain.auth.repository.RsaKeyPairRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;

/**
 * Public JWK Set endpoint. Lets any resource server validate identity-service
 * issued JWTs locally (e.g. via {@code NimbusJwtDecoder.jwkSetUri(...)})
 * without calling back into identity-service on every request.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RsaKeyPairRepository rsaKeyPairRepository;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Object> jwks() {
        List<RsaKeyPair> validKeys = rsaKeyPairRepository.findAllValid(LocalDateTime.now(ZoneId.systemDefault()));

        List<RSAKey> jwkKeys = validKeys.stream()
                .map(this::toRsaKey)
                .toList();

        JWKSet jwkSet = new JWKSet(jwkKeys.stream().map(k -> (com.nimbusds.jose.jwk.JWK) k).toList());
        return ResponseEntity.ok(jwkSet.toJSONObject());
    }

    private RSAKey toRsaKey(RsaKeyPair rsaKeyPair) {
        try {
            String pem = rsaKeyPair.getPublicKey().getPemFormat()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
            return new RSAKey.Builder(publicKey)
                    .keyID(rsaKeyPair.getKeyId().getValue())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build JWK for kid " + rsaKeyPair.getKeyId().getValue(), ex);
        }
    }
}
```

- [ ] **Step 5: Permit the new route in `SecurityConfig`**

Add `"/.well-known/jwks.json"` to the existing `permitAll()` matcher list alongside the Swagger paths.

- [ ] **Step 6: Compile and manually verify**

```bash
cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile
mvn -q spring-boot:run &
sleep 20
curl -sf http://localhost:8081/.well-known/jwks.json
kill %1
```

Expected: `{"keys":[...]}`, `[]` if there are no active sessions yet (empty array is valid JSON and a valid, if trivial, JWK Set).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -q -m "feat: add JWKS endpoint for external JWT validation"
```

---

### Task 15: New — internal API key auth + internal user lookup + internal allowed-endpoints

**Files:**
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/filter/InternalApiKeyFilter.java`
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/controller/internal/{InternalUserController,InternalAuthorizationController}.java`
- Create: `$DST/src/main/java/com/platform/identity/presentation/rest/dto/response/InternalUserResponse.java`
- Modify: `$DST/src/main/java/com/platform/identity/presentation/config/SecurityConfig.java` (register the filter, permit `/internal/**` past the JWT check — the filter does its own auth)

**Interfaces:**
- Consumes: `GetUserUseCase` (Task 6), `AuthorizationQuery` (Task 6, implemented Task 9), `app.internal-api.key` property (Task 13).
- Produces: `GET /internal/users/{id}` and `GET /internal/authorization/allowed-endpoints?portal=&roles=`, both requiring header `X-Internal-Api-Key`.

- [ ] **Step 1: Write the internal API key filter**

```java
package com.platform.identity.presentation.rest.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protects {@code /internal/**} with a shared secret header instead of the
 * public-facing JWT flow — these routes are meant to be called by other
 * services, not browsers/end users. Simplest thing that works for this
 * extraction; mTLS/service-mesh auth can replace it later without changing
 * the route contracts.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    @Value("${app.internal-api.key}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER_NAME);
        if (providedKey == null || !providedKey.equals(expectedKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"missing or invalid " + HEADER_NAME + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Write `InternalUserResponse`**

```java
package com.platform.identity.presentation.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalUserResponse {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
}
```

- [ ] **Step 3: Write `InternalUserController`**

```java
package com.platform.identity.presentation.rest.controller.internal;

import com.platform.identity.application.user.dto.UserDto;
import com.platform.identity.application.user.port.input.GetUserUseCase;
import com.platform.identity.presentation.rest.dto.response.InternalUserResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal (service-to-service) user lookup, protected by
 * {@link com.platform.identity.presentation.rest.filter.InternalApiKeyFilter}.
 * Lets another service (e.g. one rendering a user's name/email on a record
 * it owns) resolve a user without needing its own copy of the users table.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final GetUserUseCase getUserUseCase;

    @GetMapping("/{userId}")
    public ResponseEntity<InternalUserResponse> getUser(@PathVariable String userId) {
        UserDto userDto = getUserUseCase.findById(userId);
        return ResponseEntity.ok(InternalUserResponse.builder()
                .userId(userDto.getUserId())
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .status(userDto.getStatus() != null ? userDto.getStatus().toString() : null)
                .build());
    }
}
```

- [ ] **Step 4: Write `InternalAuthorizationController`**

```java
package com.platform.identity.presentation.rest.controller.internal;

import com.platform.identity.application.auth.dto.EndpointDto;
import com.platform.identity.application.auth.port.output.AuthorizationQuery;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal (service-to-service) RBAC lookup, protected by
 * {@link com.platform.identity.presentation.rest.filter.InternalApiKeyFilter}.
 * Exposes the same portal+role -&gt; allowed-endpoints data that
 * {@code MultiPortalAuthorizationManager} uses for identity-service's own
 * routes, so another service can reuse it instead of owning RBAC tables.
 */
@RestController
@RequestMapping("/internal/authorization")
@RequiredArgsConstructor
public class InternalAuthorizationController {

    private final AuthorizationQuery authorizationQuery;

    @GetMapping("/allowed-endpoints")
    public ResponseEntity<List<EndpointDto>> allowedEndpoints(
            @RequestParam String portal,
            @RequestParam List<String> roles) {
        return ResponseEntity.ok(authorizationQuery.getAllowedEndpoints(portal, roles));
    }
}
```

(Confirm `AuthorizationQuery.getAllowedEndpoints`'s exact parameter types against the copied interface in `application/auth/port/output/AuthorizationQuery.java` before writing this — Task 6 already copied it verbatim from `vetautet`, where `MultiPortalAuthorizationManager` calls it as `authService.getAllowedEndpoints(portalCode, userRoles)` with `Collection<String> userRoles`; adjust the controller's `roles` parameter type/binding to match exactly if it differs from `List<String>`.)

- [ ] **Step 5: Wire the filter and route into `SecurityConfig`**

Register `InternalApiKeyFilter` as a servlet filter before Spring Security's authentication filter (`http.addFilterBefore(internalApiKeyFilter, BearerTokenAuthenticationFilter.class)` or the equivalent already-established pattern for `AuthRateLimitFilter`/`RequestIdFilter` in this codebase — follow that same registration style). Add `/internal/users/**` and `/internal/authorization/**` to `permitAll()` in the authorization rules (the filter is the actual gate, not Spring Security's JWT check — these routes are never meant to carry a user JWT).

- [ ] **Step 6: Compile and verify**

```bash
cd "$DST" && mvn -q -DskipTests -Dliquibase.skip=true compile
mvn -q spring-boot:run &
sleep 20
curl -sf -H "X-Internal-Api-Key: local-dev-internal-key-change-me" \
  "http://localhost:8081/internal/authorization/allowed-endpoints?portal=ADMIN&roles=ADMIN"
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8081/internal/authorization/allowed-endpoints?portal=ADMIN&roles=ADMIN"
kill %1
```

Expected: first `curl` returns `200` with a JSON array (possibly empty depending on seed data); second `curl` (no header) returns `403`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -q -m "feat: add internal API key auth, internal user lookup, and internal allowed-endpoints"
```

---

### Task 16: Port the existing test suite

**Files:**
- Create: `$DST/src/test/java/com/platform/identity/domain/user/model/UserLockoutTest.java`
- Create: `$DST/src/test/java/com/platform/identity/application/user/usecase/CheckUserAvailabilityUseCaseImplTest.java`
- Create: `$DST/src/test/java/com/platform/identity/infrastructure/cache/bloomfilter/{RedissonUserAvailabilityProbeAdapterTest,UserBloomFilterSyncServiceTest}.java`
- Create: `$DST/src/test/java/com/platform/identity/infrastructure/persistence/jpa/adapter/UserRepositoryAdapterExistsByIgnoreCaseTest.java`
- Create: `$DST/src/test/java/com/platform/identity/presentation/rest/controller/v1/UserAvailabilityControllerTest.java`
- Create: `$DST/src/test/java/com/platform/identity/shared/common/util/Uuid7GeneratorTest.java`

**Interfaces:**
- Consumes: every production class exercised by these tests — all already ported by Tasks 4–12.

- [ ] **Step 1: Copy the seven existing test files**

```bash
mkdir -p "$DST/src/test/java/com/platform/identity/domain/user/model"
cp "$SRC/src/test/java/com/vetautet/app/domain/user/model/UserLockoutTest.java" \
   "$DST/src/test/java/com/platform/identity/domain/user/model/UserLockoutTest.java"

mkdir -p "$DST/src/test/java/com/platform/identity/application/user/usecase"
cp "$SRC/src/test/java/com/vetautet/app/application/user/usecase/CheckUserAvailabilityUseCaseImplTest.java" \
   "$DST/src/test/java/com/platform/identity/application/user/usecase/CheckUserAvailabilityUseCaseImplTest.java"

mkdir -p "$DST/src/test/java/com/platform/identity/infrastructure/cache/bloomfilter"
cp "$SRC/src/test/java/com/vetautet/app/infrastructure/cache/bloomfilter/RedissonUserAvailabilityProbeAdapterTest.java" \
   "$DST/src/test/java/com/platform/identity/infrastructure/cache/bloomfilter/RedissonUserAvailabilityProbeAdapterTest.java"
cp "$SRC/src/test/java/com/vetautet/app/infrastructure/cache/bloomfilter/UserBloomFilterSyncServiceTest.java" \
   "$DST/src/test/java/com/platform/identity/infrastructure/cache/bloomfilter/UserBloomFilterSyncServiceTest.java"

mkdir -p "$DST/src/test/java/com/platform/identity/infrastructure/persistence/jpa/adapter"
cp "$SRC/src/test/java/com/vetautet/app/infrastructure/persistence/jpa/adapter/UserRepositoryAdapterExistsByIgnoreCaseTest.java" \
   "$DST/src/test/java/com/platform/identity/infrastructure/persistence/jpa/adapter/UserRepositoryAdapterExistsByIgnoreCaseTest.java"

mkdir -p "$DST/src/test/java/com/platform/identity/presentation/rest/controller/v1"
cp "$SRC/src/test/java/com/vetautet/app/presentation/rest/controller/v1/UserAvailabilityControllerTest.java" \
   "$DST/src/test/java/com/platform/identity/presentation/rest/controller/v1/UserAvailabilityControllerTest.java"

mkdir -p "$DST/src/test/java/com/platform/identity/shared/common/util"
cp "$SRC/src/test/java/com/vetautet/app/shared/common/util/Uuid7GeneratorTest.java" \
   "$DST/src/test/java/com/platform/identity/shared/common/util/Uuid7GeneratorTest.java"
```

- [ ] **Step 2: Rename packages in test sources too**

```bash
find "$DST/src/test/java" -name '*.java' -exec sed -i '' 's/com\.vetautet\.app/com.platform.identity/g' {} +
```

- [ ] **Step 3: Run the suite**

```bash
cd "$DST" && mvn -q test
```

Expected: `BUILD SUCCESS`, 0 failures. If a test needs Testcontainers/an embedded DB and fails only because the local Postgres from Task 2 isn't reachable in this shell, bring it up first (`docker compose -f docker-compose-dev.yml up -d`) and re-run.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -q -m "test: port existing auth/user/RBAC test suite"
```

---

### Task 17: End-to-end smoke test

**Files:** none (verification only).

- [ ] **Step 1: Bring up infra and the app**

```bash
cd "$DST"
docker compose -f docker-compose-dev.yml up -d
export $(grep -v '^#' .env.properties | xargs)
mvn -q spring-boot:run &
sleep 20
```

- [ ] **Step 2: Register → activate → login, following the real flow**

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"smoketest","email":"smoketest@example.com","password":"P@ssw0rd123","firstName":"Smoke","lastName":"Test"}'
```

Expected: `201`/`200` with a `RegisterResponse` body. Because `AuthNotificationSender` is log-only (Task 10), the activation link/OTP is **not emailed** — read it from the application log (`grep "LOG-ONLY" logs/application.log` or the `mvn spring-boot:run` console output) and use it in the next call:

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/register/activate \
  -H "Content-Type: application/json" \
  -d '{"token":"<paste the logged link/token>"}'

curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"smoketest","password":"P@ssw0rd123"}'
```

Expected: the login call returns a `TokenResponse` with an `accessToken` (or triggers OTP verification per the existing flow — follow whatever `LoginUseCaseImpl`/`VerifyOtpLoginUseCaseImpl` actually require, confirmed already-copied and unchanged from `vetautet`).

- [ ] **Step 3: Call a protected endpoint with the issued token**

```bash
TOKEN="<accessToken from step 2>"
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/v1/users/search?keyword=smoke
```

Expected: `200` if the seeded RBAC data (`006-rbac-master-data.sql`, formerly `014-rbac-master-data.sql`) grants the registered user's default role access to this endpoint on this portal; `403` if not — either is an acceptable smoke-test outcome as long as it's not a `401`/`500`, since it confirms JWT issuance + `MultiPortalAuthorizationManager` are both wired correctly end-to-end. A `401` or `500` means something from Tasks 8/9/11 didn't wire up — check the boot log.

- [ ] **Step 4: Verify JWKS and internal endpoints**

```bash
curl -sf http://localhost:8081/.well-known/jwks.json
curl -sf -H "X-Internal-Api-Key: local-dev-internal-key-change-me" \
  "http://localhost:8081/internal/users/<userId from register response>"
```

Expected: both return `200` with the expected JSON shapes.

- [ ] **Step 5: Tear down**

```bash
kill %1
docker compose -f docker-compose-dev.yml down
```

No commit for this task — it's verification only. If any step fails, fix the underlying task and re-run this whole task from Step 1 before proceeding.

---

### Task 18: Push to the GitHub remote

**Files:** none.

- [ ] **Step 1: Check whether the remote already has commits** (don't destroy anything that might already be there)

```bash
cd "$DST"
git ls-remote origin
```

If this lists any refs, **stop and ask the user** how to proceed (the spec's non-goal is "no real data migration" and this plan never anticipated existing content on that remote) — do not force-push. If it's empty (no output besides possibly nothing), continue.

- [ ] **Step 2: Push**

```bash
git branch -M main
git push -u origin main
```

- [ ] **Step 3: Verify**

```bash
git log --oneline -5
git status
```

Expected: `git status` shows `up to date with 'origin/main'`, working tree clean.

---

## Self-Review Notes

- **Spec coverage:** every section of `2026-08-22-identity-service-extraction-design.md` maps to a task — location/package/Maven (Task 1), architecture/layering (Tasks 4–12 ordering), domain scope incl. the `AuthNotificationSender` exception (Task 10), API surface incl. all three new endpoints (Tasks 14–15), data/migration (Task 3), infrastructure/docker-compose (Task 2), testing (Task 16). Out-of-scope items from the spec are deliberately absent from this plan (no `vetautet` file appears as a Create/Modify target anywhere above).
- **Type consistency:** `AuthorizationQuery.getAllowedEndpoints` and `RsaKeyPairRepository.findAllValid` are the two places new code calls into copied/modified ports — Task 15 Step 4 and Task 14 Steps 1–3 flag exactly where to double check the real signatures against the copied source before finalizing, since this plan was written from a prior reading of that source rather than the live file at execution time.
- **Scope check:** 18 tasks, each independently testable (`mvn compile`/`mvn test`/a boot+curl check), ending in a pushed, running service — no further decomposition needed.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-22-identity-service-extraction.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
