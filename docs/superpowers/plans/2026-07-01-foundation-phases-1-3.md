# 新员工导引系统 · 基础三阶段实现计划（脚手架 + 认证 + 内容/进度闭环）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭起前后端异构仓库骨架，实现姓名全拼+密码登录与 JWT 鉴权，跑通"机构/小岛/文档读取 + 学习进度上报与聚合"的核心学习闭环，产出可独立验收的工作软件。

**Architecture:** 前端 pnpm workspace（`apps/web` React 游戏 + `apps/admin` React 后台 + `packages/shared` 前端共享类型），后端独立 `api/` Maven 模块（Spring Boot 3 + Java 17 + Spring Data JPA + PostgreSQL + jjwt）。单 JVM 进程经静态资源映射同时托管 `/api/**` 与两份前端构建产物。文档级进度为单一真相源，小岛状态服务端聚合派生。

**Tech Stack:** Java 17、Spring Boot 3.2.x、Spring Data JPA、Spring Security、jjwt 0.12.x、Flyway、PostgreSQL 15+、Maven 3.9+；React 18、Vite 5、TypeScript 5、Zustand、React Router 6、Axios、Ant Design（admin）；pnpm 9+、Node 20+。

## Global Constraints

- Java 17（LTS），Spring Boot 3.2.x，Maven 构建。
- PostgreSQL 15+，连接串与凭证走 `application-local.yml`（gitignore），不入库。
- JWT secret、DB 密码等敏感配置只放环境变量或 `application-local.yml`，不进 git。
- 前端两 app 独立 Vite 构建，token 存 localStorage，Axios 拦截器统一附 `Authorization: Bearer <token>`，401 重定向登录。
- 密码用 BCrypt（cost 12），登录失败不区分"用户不存在/密码错误"。
- 文档下载必须经鉴权 API，`uploads/` 不挂公开静态目录。
- 进度上报幂等 upsert，`progressPct` 单调不回退。
- 每个任务结束 `git commit`，提交信息用约定式（`feat:`/`chore:`/`test:` 等）。
- 现有 Demo 文件（`index.html` 等）保留在原位，本计划不改动它们；新增内容写在新目录。

## 范围说明

本计划覆盖落地顺序的 **阶段 1-3**（脚手架、认证、内容+进度闭环）。阶段 4（文件上传下载）、5（管理后台完善）、6（打磨/动画迁移）作为后续独立计划。本计划结束时：用户可登录、看到机构/小岛/文档、阅读文档并上报进度、看到小岛聚合完成状态。

---

## File Structure（本计划涉及）

```
new-employee-orientation/
├── apps/
│   ├── web/                      React 游戏（Vite）
│   │   ├── src/
│   │   │   ├── main.tsx
│   │   │   ├── App.tsx
│   │   │   ├── api/client.ts            axios 实例 + 拦截器
│   │   │   ├── stores/authStore.ts      Zustand auth
│   │   │   ├── stores/progressStore.ts  Zustand progress
│   │   │   ├── pages/LoginPage.tsx
│   │   │   ├── pages/VoyagePage.tsx     航行首页（占位）
│   │   │   ├── pages/IslandPage.tsx     小岛文档列表
│   │   │   └── pages/DocPage.tsx        文档阅读 + 进度上报
│   │   ├── index.html
│   │   ├── vite.config.ts
│   │   ├── tsconfig.json
│   │   └── package.json
│   └── admin/                   React 后台（Vite，仅登录骨架）
│       ├── src/main.tsx
│       ├── src/App.tsx
│       ├── src/pages/LoginPage.tsx
│       └── package.json
├── packages/
│   └── shared/                  前端共享类型
│       ├── src/types.ts
│       ├── src/constants.ts
│       ├── src/index.ts
│       └── package.json
├── api/                         Spring Boot 后端
│   ├── src/main/java/com/gmnl/orientation/
│   │   ├── OrientationApplication.java
│   │   ├── config/SecurityConfig.java
│   │   ├── config/JwtAuthFilter.java
│   │   ├── config/WebConfig.java
│   │   ├── security/JwtService.java
│   │   ├── common/ApiError.java
│   │   ├── common/GlobalExceptionHandler.java
│   │   ├── user/User.java
│   │   ├── user/UserRepository.java
│   │   ├── user/UserRole.java
│   │   ├── user/AuthController.java
│   │   ├── user/AuthService.java
│   │   ├── content/Institution.java
│   │   ├── content/InstitutionRepository.java
│   │   ├── content/Island.java
│   │   ├── content/IslandRepository.java
│   │   ├── content/Doc.java
│   │   ├── content/DocRepository.java
│   │   ├── content/ContentController.java
│   │   ├── content/ContentService.java
│   │   ├── progress/Progress.java
│   │   ├── progress/ProgressId.java
│   │   ├── progress/ProgressStatus.java
│   │   ├── progress/IslandState.java
│   │   ├── progress/ProgressRepository.java
│   │   ├── progress/IslandStateRepository.java
│   │   ├── progress/ProgressController.java
│   │   ├── progress/ProgressService.java
│   │   └── seed/DataInitializer.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/V1__init.sql
│   └── src/test/java/com/gmnl/orientation/...
│   ├── pom.xml
├── uploads/                     文档文件（gitignore）
├── .gitignore
├── pnpm-workspace.yaml
└── package.json
```

---

## 前置：工具链准备

环境当前：Node 24 + npm 11 可用；Java/Maven、pnpm、PostgreSQL 未安装。

- [ ] **Step 0a: 安装 JDK 17 与 Maven**

Windows 上推荐用 winget（用户在终端执行，`!` 前缀可在本会话跑）：

```bash
! winget install Microsoft.OpenJDK.17
! winget install Apache.Maven
```

装后重开终端，验证：

```bash
java -version
# Expected: openjdk version "17.x.x"
mvn -v
# Expected: Apache Maven 3.9.x
```

- [ ] **Step 0b: 安装 PostgreSQL 15+**

```bash
! winget install PostgreSQL.PostgreSQL.15
```

安装时记住设的 superuser 密码。验证：

```bash
psql --version
# Expected: psql (PostgreSQL) 15.x
```

- [ ] **Step 0c: 创建数据库与用户**

启动 psql（`psql -U postgres`），执行：

```sql
CREATE DATABASE orientation;
CREATE USER orientation_app WITH PASSWORD 'orientation_dev_pass';
GRANT ALL PRIVILEGES ON DATABASE orientation TO orientation_app;
```

- [ ] **Step 0d: 启用 pnpm**

```bash
! npm install -g pnpm@9
pnpm -v
# Expected: 9.x.x
```

---

### Task 1: 仓库初始化与 .gitignore

**Files:**
- Create: `.gitignore`
- Create: `pnpm-workspace.yaml`
- Create: `package.json`（根）

**Interfaces:**
- Produces: 根 workspace 配置，后续前端任务依赖 `pnpm-workspace.yaml` 指向 `apps/*` 与 `packages/*`。

- [ ] **Step 1: 初始化 git（当前 master 分支无提交）并写 .gitignore**

在仓库根 `D:\systems\cursor\new-employee-orientation\new-employee-orientation\Demo`（实际仓库根，下文统一称仓库根）创建 `.gitignore`：

```gitignore
# Java / Maven
api/target/
*.class
*.jar
*.war

# IDE
.idea/
*.iml
.vscode/

# Node / pnpm
node_modules/
dist/
.pnpm-store/

# 本地配置与上传
api/src/main/resources/application-local.yml
uploads/
!uploads/.gitkeep

# OS
.DS_Store
Thumbs.db

# 旧 Demo 备份
index.html.bak
```

- [ ] **Step 2: 写 pnpm-workspace.yaml**

```yaml
packages:
  - "apps/*"
  - "packages/*"
```

- [ ] **Step 3: 写根 package.json**

```json
{
  "name": "new-employee-orientation",
  "private": true,
  "version": "0.1.0",
  "scripts": {
    "dev:web": "pnpm --filter @gmnl/web dev",
    "dev:admin": "pnpm --filter @gmnl/admin dev",
    "build": "pnpm -r build",
    "build:web": "pnpm --filter @gmnl/web build",
    "build:admin": "pnpm --filter @gmnl/admin build"
  },
  "packageManager": "pnpm@9.0.0"
}
```

- [ ] **Step 4: 提交**

```bash
git add .gitignore pnpm-workspace.yaml package.json
git commit -m "chore: init monorepo root with pnpm workspace and gitignore"
```

---

### Task 2: packages/shared 前端共享类型与常量

**Files:**
- Create: `packages/shared/package.json`
- Create: `packages/shared/tsconfig.json`
- Create: `packages/shared/src/types.ts`
- Create: `packages/shared/src/constants.ts`
- Create: `packages/shared/src/index.ts`

**Interfaces:**
- Produces: `@gmnl/shared` 包，导出 TS 类型（`User`/`Institution`/`Island`/`Doc`/`Progress`/`IslandStateView`/`UserRole`/`DocStatus`/`IslandStatus`）、常量（`INSTITUTION_KEYS`）、以及与后端 Java DTO 对齐的请求/响应 DTO。`apps/web` 与 `apps/admin` 后续 import 这些。

- [ ] **Step 1: 写 package.json**

`packages/shared/package.json`：

```json
{
  "name": "@gmnl/shared",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "main": "./src/index.ts",
  "types": "./src/index.ts",
  "exports": {
    ".": "./src/index.ts"
  }
}
```

- [ ] **Step 2: 写 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "declaration": true
  },
  "include": ["src"]
}
```

- [ ] **Step 3: 写 types.ts**

`packages/shared/src/types.ts`：

```ts
export type UserRole = 'USER' | 'ADMIN';

export interface User {
  id: number;
  username: string;
  name: string;
  role: UserRole;
}

export interface Institution {
  id: number;
  key: string;
  name: string;
  order: number;
  islands: Island[];
}

export interface Island {
  id: number;
  key: string;
  name: string;
  order: number;
  institutionId: number;
}

export type DocFileType = 'pdf' | 'docx' | 'pptx' | 'xlsx';

export interface Doc {
  id: number;
  title: string;
  category: string;
  institutionId: number;
  islandId: number;
  required: boolean;
  fileType: DocFileType | null;
  order: number;
  active: boolean;
}

export type DocStatus = 'NOT_STARTED' | 'READING' | 'COMPLETED';
export type IslandStatus = 'LOCKED' | 'UNLOCKED' | 'COMPLETED';

export interface ProgressItem {
  docId: number;
  status: DocStatus;
  progressPct: number;
  lastReadAt: string | null;
  completedAt: string | null;
}

export interface IslandStateView {
  islandId: number;
  status: IslandStatus;
  completedCount: number;
  totalCount: number;
}

export interface ProgressAggregate {
  documents: ProgressItem[];
  islands: IslandStateView[];
}

// 请求/响应 DTO
export interface LoginRequest {
  username: string;
  password: string;
}
export interface LoginResponse {
  token: string;
  user: User;
}
export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}
export interface UpsertProgressRequest {
  status: DocStatus;
  progressPct: number;
}
```

- [ ] **Step 4: 写 constants.ts**

`packages/shared/src/constants.ts`：

```ts
// 7 大机构短码，与后端 DataInitializer 种子一致
export const INSTITUTION_KEYS = [
  'BJ', 'SH', 'SD', 'SC', 'GD', 'CQ', 'ZJ',
] as const;

export const API_BASE = '/api';
```

- [ ] **Step 5: 写 index.ts**

`packages/shared/src/index.ts`：

```ts
export * from './types';
export * from './constants';
```

- [ ] **Step 6: 提交**

```bash
git add packages/shared
git commit -m "feat(shared): add frontend shared types and constants"
```

---

### Task 3: 后端 Spring Boot 骨架（pom + Application + 配置）

**Files:**
- Create: `api/pom.xml`
- Create: `api/src/main/java/com/gmnl/orientation/OrientationApplication.java`
- Create: `api/src/main/resources/application.yml`
- Create: `api/src/main/resources/application-local.yml.example`
- Create: `uploads/.gitkeep`

**Interfaces:**
- Produces: 可启动的 Spring Boot 应用（连不上 DB 暂不报错，后续任务加 Flyway）。包名 `com.gmnl.orientation`，后续所有 Java 类在此包下。

- [ ] **Step 1: 写 pom.xml**

`api/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
    <relativePath/>
  </parent>

  <groupId>com.gmnl</groupId>
  <artifactId>orientation-api</artifactId>
  <version>0.1.0</version>
  <name>orientation-api</name>

  <properties>
    <java.version>17</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.12.6</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: 写主类**

`api/src/main/java/com/gmnl/orientation/OrientationApplication.java`：

```java
package com.gmnl.orientation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrientationApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrientationApplication.class, args);
  }
}
```

- [ ] **Step 3: 写 application.yml**

`api/src/main/resources/application.yml`：

```yaml
spring:
  profiles:
    active: local
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/orientation}
    username: ${DB_USER:orientation_app}
    password: ${DB_PASSWORD:orientation_dev_pass}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

app:
  jwt:
    secret: ${JWT_SECRET:dev-secret-change-me-please-make-it-long-enough-32bytes}
    expiration-hours: 8
  uploads:
    dir: ${UPLOADS_DIR:./uploads}

server:
  port: 8080
```

- [ ] **Step 4: 写本地配置示例**

`api/src/main/resources/application-local.yml.example`（用户复制为 `application-local.yml` 改密码）：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orientation
    username: orientation_app
    password: orientation_dev_pass
app:
  jwt:
    secret: dev-secret-change-me-please-make-it-long-enough-32bytes
```

- [ ] **Step 5: 占位 uploads 目录**

`uploads/.gitkeep`（空文件，保证目录存在但被 gitignore 排除内容）。

- [ ] **Step 6: 验证编译**

```bash
cd api && mvn -q compile
# Expected: BUILD SUCCESS
```

- [ ] **Step 7: 提交**

```bash
git add api/pom.xml api/src/main/java api/src/main/resources/application.yml api/src/main/resources/application-local.yml.example api/src/main/resources/db uploads/.gitkeep
git commit -m "feat(api): spring boot scaffold with jpa, security, flyway, jjwt"
```

---

### Task 4: Flyway 初始迁移（建表）

**Files:**
- Create: `api/src/main/resources/db/migration/V1__init.sql`

**Interfaces:**
- Produces: 数据库 schema（users / institutions / islands / docs / progress / island_states 表）。后续 JPA 实体映射这些表。

- [ ] **Step 1: 写 V1__init.sql**

```sql
CREATE TABLE users (
  id              BIGSERIAL PRIMARY KEY,
  username        VARCHAR(64)  NOT NULL UNIQUE,
  name            VARCHAR(64)  NOT NULL,
  password_hash   VARCHAR(100) NOT NULL,
  role            VARCHAR(16)  NOT NULL DEFAULT 'USER',
  employee_no     VARCHAR(32),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE institutions (
  id    BIGSERIAL PRIMARY KEY,
  key   VARCHAR(8)  NOT NULL UNIQUE,
  name  VARCHAR(32) NOT NULL,
  "order" INT      NOT NULL DEFAULT 0
);

CREATE TABLE islands (
  id              BIGSERIAL PRIMARY KEY,
  key             VARCHAR(16) NOT NULL UNIQUE,
  name            VARCHAR(32) NOT NULL,
  "order"         INT NOT NULL DEFAULT 0,
  institution_id  BIGINT NOT NULL REFERENCES institutions(id)
);

CREATE TABLE docs (
  id              BIGSERIAL PRIMARY KEY,
  title           VARCHAR(128) NOT NULL,
  category        VARCHAR(64),
  institution_id  BIGINT NOT NULL REFERENCES institutions(id),
  island_id       BIGINT NOT NULL REFERENCES islands(id),
  required        BOOLEAN NOT NULL DEFAULT FALSE,
  file_path       VARCHAR(256),
  file_type       VARCHAR(8),
  "order"         INT NOT NULL DEFAULT 0,
  active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE progress (
  user_id         BIGINT NOT NULL REFERENCES users(id),
  doc_id          BIGINT NOT NULL REFERENCES docs(id),
  status          VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED',
  progress_pct    INT NOT NULL DEFAULT 0,
  last_read_at    TIMESTAMPTZ,
  completed_at    TIMESTAMPTZ,
  PRIMARY KEY (user_id, doc_id)
);

CREATE TABLE island_states (
  user_id         BIGINT NOT NULL REFERENCES users(id),
  island_id       BIGINT NOT NULL REFERENCES islands(id),
  status          VARCHAR(16) NOT NULL DEFAULT 'LOCKED',
  PRIMARY KEY (user_id, island_id)
);
```

- [ ] **Step 2: 验证迁移可执行**

确保 `application-local.yml` 已从 example 复制并配好密码，然后：

```bash
cd api && mvn -q spring-boot:run
# 观察日志出现 "Flyway ... Migrating schema ... to version 1 - init" 与 "Successfully applied 1 migration"
# Ctrl+C 停止
psql -U orientation_app -d orientation -c "\dt"
# Expected: 列出 users, institutions, islands, docs, progress, island_states, flyway_schema_history
```

- [ ] **Step 3: 提交**

```bash
git add api/src/main/resources/db/migration/V1__init.sql
git commit -m "feat(api): flyway v1 init schema"
```

---

### Task 5: JPA 实体与 Repository

**Files:**
- Create: `api/src/main/java/com/gmnl/orientation/user/UserRole.java`
- Create: `api/src/main/java/com/gmnl/orientation/user/User.java`
- Create: `api/src/main/java/com/gmnl/orientation/user/UserRepository.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/Institution.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/InstitutionRepository.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/Island.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/IslandRepository.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/Doc.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/DocRepository.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/ProgressStatus.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/IslandStatus.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/Progress.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/IslandState.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/ProgressRepository.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/IslandStateRepository.java`

**Interfaces:**
- Produces: 实体与 Repository。后续 AuthService 用 `UserRepository.findByUsername`；ContentService 用 `InstitutionRepository`/`IslandRepository`/`DocRepository`；ProgressService 用 `ProgressRepository`/`IslandStateRepository`。
- 关键签名：
  - `UserRepository.findByUsername(String) -> Optional<User>`
  - `DocRepository.findByIslandIdAndActiveTrueOrderByOrder(Long) -> List<Doc>`
  - `ProgressRepository.findById(ProgressId) -> Optional<Progress>`、`findByUserId(Long) -> List<Progress>`
  - `IslandStateRepository.findByUserId(Long) -> List<IslandState>`

- [ ] **Step 1: UserRole 枚举**

`user/UserRole.java`：

```java
package com.gmnl.orientation.user;

public enum UserRole {
  USER, ADMIN
}
```

- [ ] **Step 2: User 实体**

`user/User.java`：

```java
package com.gmnl.orientation.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String username;

  @Column(nullable = false, length = 64)
  private String name;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private UserRole role = UserRole.USER;

  @Column(name = "employee_no", length = 32)
  private String employeeNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  // getters & setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public UserRole getRole() { return role; }
  public void setRole(UserRole role) { this.role = role; }
  public String getEmployeeNo() { return employeeNo; }
  public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: UserRepository**

`user/UserRepository.java`：

```java
package com.gmnl.orientation.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
  boolean existsByUsername(String username);
}
```

- [ ] **Step 4: Institution 实体 + Repository**

`content/Institution.java`：

```java
package com.gmnl.orientation.content;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "institutions")
public class Institution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 8)
  private String key;

  @Column(nullable = false, length = 32)
  private String name;

  @Column(name = "\"order\"", nullable = false)
  private Integer order = 0;

  @OneToMany(mappedBy = "institutionId")
  private List<Island> islands = new ArrayList<>();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getKey() { return key; }
  public void setKey(String key) { this.key = key; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Integer getOrder() { return order; }
  public void setOrder(Integer order) { this.order = order; }
  public List<Island> getIslands() { return islands; }
  public void setIslands(List<Island> islands) { this.islands = islands; }
}
```

`content/InstitutionRepository.java`：

```java
package com.gmnl.orientation.content;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
  List<Institution> findAllByOrderByOrderAsc();
}
```

- [ ] **Step 5: Island 实体 + Repository**

`content/Island.java`：

```java
package com.gmnl.orientation.content;

import jakarta.persistence.*;

@Entity
@Table(name = "islands")
public class Island {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 16)
  private String key;

  @Column(nullable = false, length = 32)
  private String name;

  @Column(name = "\"order\"", nullable = false)
  private Integer order = 0;

  @Column(name = "institution_id", nullable = false)
  private Long institutionId;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getKey() { return key; }
  public void setKey(String key) { this.key = key; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Integer getOrder() { return order; }
  public void setOrder(Integer order) { this.order = order; }
  public Long getInstitutionId() { return institutionId; }
  public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }
}
```

`content/IslandRepository.java`：

```java
package com.gmnl.orientation.content;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IslandRepository extends JpaRepository<Island, Long> {
  List<Island> findByInstitutionIdOrderByOrderAsc(Long institutionId);
  List<Island> findAllByOrderByOrderAsc();
}
```

- [ ] **Step 6: Doc 实体 + Repository**

`content/Doc.java`：

```java
package com.gmnl.orientation.content;

import jakarta.persistence.*;

@Entity
@Table(name = "docs")
public class Doc {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 128)
  private String title;

  @Column(length = 64)
  private String category;

  @Column(name = "institution_id", nullable = false)
  private Long institutionId;

  @Column(name = "island_id", nullable = false)
  private Long islandId;

  @Column(nullable = false)
  private Boolean required = false;

  @Column(name = "file_path", length = 256)
  private String filePath;

  @Column(name = "file_type", length = 8)
  private String fileType;

  @Column(name = "\"order\"", nullable = false)
  private Integer order = 0;

  @Column(nullable = false)
  private Boolean active = true;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public Long getInstitutionId() { return institutionId; }
  public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }
  public Long getIslandId() { return islandId; }
  public void setIslandId(Long islandId) { this.islandId = islandId; }
  public Boolean getRequired() { return required; }
  public void setRequired(Boolean required) { this.required = required; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public String getFileType() { return fileType; }
  public void setFileType(String fileType) { this.fileType = fileType; }
  public Integer getOrder() { return order; }
  public void setOrder(Integer order) { this.order = order; }
  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }
}
```

`content/DocRepository.java`：

```java
package com.gmnl.orientation.content;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocRepository extends JpaRepository<Doc, Long> {
  List<Doc> findByIslandIdAndActiveTrueOrderByOrderAsc(Long islandId);
}
```

- [ ] **Step 7: 进度枚举与实体**

`progress/ProgressStatus.java`：

```java
package com.gmnl.orientation.progress;

public enum ProgressStatus {
  NOT_STARTED, READING, COMPLETED
}
```

`progress/IslandStatus.java`：

```java
package com.gmnl.orientation.progress;

public enum IslandStatus {
  LOCKED, UNLOCKED, COMPLETED
}
```

`progress/Progress.java`（复合主键）：

```java
package com.gmnl.orientation.progress;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "progress")
@IdClass(ProgressId.class)
public class Progress {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Id
  @Column(name = "doc_id")
  private Long docId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ProgressStatus status = ProgressStatus.NOT_STARTED;

  @Column(name = "progress_pct", nullable = false)
  private Integer progressPct = 0;

  @Column(name = "last_read_at")
  private Instant lastReadAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getDocId() { return docId; }
  public void setDocId(Long docId) { this.docId = docId; }
  public ProgressStatus getStatus() { return status; }
  public void setStatus(ProgressStatus status) { this.status = status; }
  public Integer getProgressPct() { return progressPct; }
  public void setProgressPct(Integer progressPct) { this.progressPct = progressPct; }
  public Instant getLastReadAt() { return lastReadAt; }
  public void setLastReadAt(Instant lastReadAt) { this.lastReadAt = lastReadAt; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
```

`progress/ProgressId.java`：

```java
package com.gmnl.orientation.progress;

import java.io.Serializable;
import java.util.Objects;

public class ProgressId implements Serializable {
  private Long userId;
  private Long docId;

  public ProgressId() {}
  public ProgressId(Long userId, Long docId) {
    this.userId = userId;
    this.docId = docId;
  }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getDocId() { return docId; }
  public void setDocId(Long docId) { this.docId = docId; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProgressId that)) return false;
    return Objects.equals(userId, that.userId) && Objects.equals(docId, that.docId);
  }

  @Override
  public int hashCode() { return Objects.hash(userId, docId); }
}
```

`progress/IslandState.java`：

```java
package com.gmnl.orientation.progress;

import jakarta.persistence.*;

@Entity
@Table(name = "island_states")
@IdClass(IslandStateId.class)
public class IslandState {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Id
  @Column(name = "island_id")
  private Long islandId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private IslandStatus status = IslandStatus.LOCKED;

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getIslandId() { return islandId; }
  public void setIslandId(Long islandId) { this.islandId = islandId; }
  public IslandStatus getStatus() { return status; }
  public void setStatus(IslandStatus status) { this.status = status; }
}
```

`progress/IslandStateId.java`：

```java
package com.gmnl.orientation.progress;

import java.io.Serializable;
import java.util.Objects;

public class IslandStateId implements Serializable {
  private Long userId;
  private Long islandId;

  public IslandStateId() {}
  public IslandStateId(Long userId, Long islandId) {
    this.userId = userId;
    this.islandId = islandId;
  }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getIslandId() { return islandId; }
  public void setIslandId(Long islandId) { this.islandId = islandId; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IslandStateId that)) return false;
    return Objects.equals(userId, that.userId) && Objects.equals(islandId, that.islandId);
  }

  @Override
  public int hashCode() { return Objects.hash(userId, islandId); }
}
```

- [ ] **Step 8: Progress / IslandState Repository**

`progress/ProgressRepository.java`：

```java
package com.gmnl.orientation.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProgressRepository extends JpaRepository<Progress, ProgressId> {
  List<Progress> findByUserId(Long userId);
}
```

`progress/IslandStateRepository.java`：

```java
package com.gmnl.orientation.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IslandStateRepository extends JpaRepository<IslandState, IslandStateId> {
  List<IslandState> findByUserId(Long userId);
}
```

- [ ] **Step 9: 验证编译**

```bash
cd api && mvn -q compile
# Expected: BUILD SUCCESS
```

- [ ] **Step 10: 提交**

```bash
git add api/src/main/java/com/gmnl/orientation/user api/src/main/java/com/gmnl/orientation/content api/src/main/java/com/gmnl/orientation/progress
git commit -m "feat(api): jpa entities and repositories"
```

---

### Task 6: 种子数据 DataInitializer

**Files:**
- Create: `api/src/main/java/com/gmnl/orientation/seed/DataInitializer.java`

**Interfaces:**
- Consumes: Task 5 的实体与 Repository。
- Produces: 启动时若库为空，写入 7 机构、5 小岛、若干占位文档、1 个 admin 账号（`admin`/`admin12345`）。后续 Auth/Content/Progress 任务依赖这些种子。

- [ ] **Step 1: 写 DataInitializer**

```java
package com.gmnl.orientation.seed;

import com.gmnl.orientation.content.*;
import com.gmnl.orientation.user.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

  private final InstitutionRepository institutionRepo;
  private final IslandRepository islandRepo;
  private final DocRepository docRepo;
  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;

  public DataInitializer(InstitutionRepository institutionRepo,
                         IslandRepository islandRepo,
                         DocRepository docRepo,
                         UserRepository userRepo,
                         PasswordEncoder passwordEncoder) {
    this.institutionRepo = institutionRepo;
    this.islandRepo = islandRepo;
    this.docRepo = docRepo;
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    if (userRepo.count() > 0) return; // 已初始化则跳过

    // admin 账号
    User admin = new User();
    admin.setUsername("admin");
    admin.setName("管理员");
    admin.setPasswordHash(passwordEncoder.encode("admin12345"));
    admin.setRole(UserRole.ADMIN);
    userRepo.save(admin);

    // 7 机构
    Map<String, String> instDefs = new java.util.LinkedHashMap<>();
    instDefs.put("BJ", "北京");
    instDefs.put("SH", "上海");
    instDefs.put("SD", "山东");
    instDefs.put("SC", "四川");
    instDefs.put("GD", "广东");
    instDefs.put("CQ", "重庆");
    instDefs.put("ZJ", "浙江");

    int order = 0;
    for (Map.Entry<String, String> e : instDefs.entrySet()) {
      Institution inst = new Institution();
      inst.setKey(e.getKey());
      inst.setName(e.getValue());
      inst.setOrder(order++);
      institutionRepo.save(inst);

      // 每个机构建同样的 5 小岛
      String[][] islandDefs = {
        {"about", "关于公司"},
        {"onboarding", "入职须知"},
        {"office", "办公指南"},
        {"products", "公司产品"},
        {"quiz", "知识测验"}
      };
      int io = 0;
      for (String[] d : islandDefs) {
        Island isl = new Island();
        isl.setKey(d[0] + "_" + e.getKey());
        isl.setName(d[1]);
        isl.setOrder(io++);
        isl.setInstitutionId(inst.getId());
        islandRepo.save(isl);

        // 每小岛 1 个占位文档（真实文件由后台后续上传）
        Doc doc = new Doc();
        doc.setTitle(d[1] + " - " + e.getValue() + "（示例）");
        doc.setCategory(d[1]);
        doc.setInstitutionId(inst.getId());
        doc.setIslandId(isl.getId());
        doc.setRequired(true);
        doc.setFilePath(null);
        doc.setFileType(null);
        doc.setOrder(0);
        doc.setActive(true);
        docRepo.save(doc);
      }
    }
  }
}
```

- [ ] **Step 2: 验证启动与种子**

```bash
cd api && mvn -q spring-boot:run
# 日志无报错；Ctrl+C 停止
psql -U orientation_app -d orientation -c "SELECT username, role FROM users;"
# Expected: admin | ADMIN
psql -U orientation_app -d orientation -c "SELECT COUNT(*) FROM institutions;"
# Expected: 7
psql -U orientation_app -d orientation -c "SELECT COUNT(*) FROM islands;"
# Expected: 35
```

- [ ] **Step 3: 提交**

```bash
git add api/src/main/java/com/gmnl/orientation/seed/DataInitializer.java
git commit -m "feat(api): seed institutions, islands, docs, admin account"
```

---

### Task 7: JWT 服务、过滤器、Security 配置

**Files:**
- Create: `api/src/main/java/com/gmnl/orientation/security/JwtService.java`
- Create: `api/src/main/java/com/gmnl/orientation/config/JwtAuthFilter.java`
- Create: `api/src/main/java/com/gmnl/orientation/config/SecurityConfig.java`
- Create: `api/src/main/java/com/gmnl/orientation/common/ApiError.java`
- Create: `api/src/main/java/com/gmnl/orientation/common/GlobalExceptionHandler.java`
- Test: `api/src/test/java/com/gmnl/orientation/security/JwtServiceTest.java`

**Interfaces:**
- Produces:
  - `JwtService.generate(Long userId, String username, String name, UserRole role) -> String`
  - `JwtService.parse(String token) -> Claims`（失败抛 `JwtException`）
  - `JwtAuthFilter`：从 `Authorization: Bearer <token>` 取 token，解析后填充 `SecurityContext`。
  - `SecurityConfig`：放行 `/api/auth/login`，其余 `/api/**` 需认证，`/api/admin/**` 需 `ADMIN`。
  - `PasswordEncoder` bean（BCrypt）。

- [ ] **Step 1: 写 JwtService**

`security/JwtService.java`：

```java
package com.gmnl.orientation.security;

import com.gmnl.orientation.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {

  private final SecretKey key;
  private final Duration expiration;

  public JwtService(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expiration-hours:8}") long hours) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = Duration.ofHours(hours);
  }

  public String generate(Long userId, String username, String name, UserRole role) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expiration.toMillis());
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("username", username)
        .claim("name", name)
        .claim("role", role.name())
        .issuedAt(now)
        .expiration(exp)
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) throws JwtException {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
```

- [ ] **Step 2: 写 JwtAuthFilter**

`config/JwtAuthFilter.java`：

```java
package com.gmnl.orientation.config;

import com.gmnl.orientation.security.JwtService;
import com.gmnl.orientation.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String header = req.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        Claims c = jwtService.parse(token);
        String role = c.get("role", String.class);
        var auth = new UsernamePasswordAuthenticationToken(
            c.getSubject(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + UserRole.valueOf(role).name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (JwtException | IllegalArgumentException e) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(req, res);
  }
}
```

- [ ] **Step 3: 写 SecurityConfig**

`config/SecurityConfig.java`：

```java
package com.gmnl.orientation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
```

- [ ] **Step 4: 写 ApiError + GlobalExceptionHandler**

`common/ApiError.java`：

```java
package com.gmnl.orientation.common;

public record ApiError(String code, String message) {}
```

`common/GlobalExceptionHandler.java`：

```java
package com.gmnl.orientation.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  public static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new ApiError(code, message));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> internal(Exception e) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "服务器内部错误");
  }
}
```

- [ ] **Step 5: 写 JwtServiceTest**

`api/src/test/java/com/gmnl/orientation/security/JwtServiceTest.java`：

```java
package com.gmnl.orientation.security;

import com.gmnl.orientation.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

  private final JwtService service = new JwtService(
      "test-secret-test-secret-test-secret-test-secret-32b", 8);

  @Test
  void generateThenParseRoundTrip() {
    String token = service.generate(42L, "zhangsan", "张三", UserRole.USER);
    Claims c = service.parse(token);
    assertEquals("42", c.getSubject());
    assertEquals("zhangsan", c.get("username", String.class));
    assertEquals("USER", c.get("role", String.class));
  }

  @Test
  void invalidTokenThrows() {
    assertThrows(JwtException.class, () -> service.parse("not-a-token"));
  }
}
```

- [ ] **Step 6: 运行测试**

```bash
cd api && mvn -q test -Dtest=JwtServiceTest
# Expected: Tests run: 2, Failures: 0
```

- [ ] **Step 7: 验证编译启动**

```bash
cd api && mvn -q compile
# Expected: BUILD SUCCESS
```

- [ ] **Step 8: 提交**

```bash
git add api/src/main/java/com/gmnl/orientation/security api/src/main/java/com/gmnl/orientation/config api/src/main/java/com/gmnl/orientation/common api/src/test
git commit -m "feat(api): jwt service, auth filter, security config, error handling"
```

---

### Task 8: 认证 API（登录 / me / 改密）

**Files:**
- Create: `api/src/main/java/com/gmnl/orientation/user/AuthController.java`
- Create: `api/src/main/java/com/gmnl/orientation/user/AuthService.java`
- Create: `api/src/main/java/com/gmnl/orientation/user/UserDto.java`
- Create: `api/src/main/java/com/gmnl/orientation/user/LoginRequest.java`
- Create: `api/src/main/java/com/gmnl/orientation/user/ChangePasswordRequest.java`
- Create: `api/src/main/java/com/gmnl/orientation/user/CurrentUserResolver.java`
- Test: `api/src/test/java/com/gmnl/orientation/user/AuthServiceTest.java`

**Interfaces:**
- Consumes: `UserRepository.findByUsername`、`PasswordEncoder`、`JwtService.generate`。
- Produces REST：
  - `POST /api/auth/login` body `LoginRequest{username,password}` → `200 {token, user}` 或 `401 {code:"INVALID_CREDENTIALS",message:"账号或密码错误"}`
  - `GET /api/auth/me` → `200 UserDto`
  - `PUT /api/auth/password` body `ChangePasswordRequest{oldPassword,newPassword}` → `200` 或 `400`/`401`
- `CurrentUserResolver.userId()` 从 `SecurityContext` 取当前用户 id。

- [ ] **Step 1: 写 DTO 与请求体**

`user/UserDto.java`：

```java
package com.gmnl.orientation.user;

public record UserDto(Long id, String username, String name, UserRole role) {
  public static UserDto from(User u) {
    return new UserDto(u.getId(), u.getUsername(), u.getName(), u.getRole());
  }
}
```

`user/LoginRequest.java`：

```java
package com.gmnl.orientation.user;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
```

`user/ChangePasswordRequest.java`：

```java
package com.gmnl.orientation.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 8, message = "密码至少 8 位") String newPassword) {}
```

- [ ] **Step 2: 写 CurrentUserResolver**

`user/CurrentUserResolver.java`：

```java
package com.gmnl.orientation.user;

import com.gmnl.orientation.common.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserResolver {

  public Long userId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    return Long.valueOf(auth.getPrincipal().toString());
  }
}
```

- [ ] **Step 3: 写 AuthService**

`user/AuthService.java`：

```java
package com.gmnl.orientation.user;

import com.gmnl.orientation.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public record LoginResult(String token, UserDto user) {}

  public LoginResult login(String username, String password) {
    User user = userRepo.findByUsername(username)
        .orElseThrow(() -> invalidCredentials());
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw invalidCredentials();
    }
    String token = jwtService.generate(user.getId(), user.getUsername(), user.getName(), user.getRole());
    return new LoginResult(token, UserDto.from(user));
  }

  @Transactional
  public void changePassword(Long userId, String oldPassword, String newPassword) {
    User user = userRepo.findById(userId)
        .orElseThrow(() -> invalidCredentials());
    if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
      throw invalidCredentials();
    }
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepo.save(user);
  }

  private RuntimeException invalidCredentials() {
    return new InvalidCredentialsException();
  }

  public static class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("账号或密码错误"); }
  }
}
```

- [ ] **Step 4: 在 GlobalExceptionHandler 增加凭据异常处理**

修改 `common/GlobalExceptionHandler.java`，在类内追加：

```java
  @ExceptionHandler(com.gmnl.orientation.user.AuthService.InvalidCredentialsException.class)
  public ResponseEntity<ApiError> invalidCredentials(com.gmnl.orientation.user.AuthService.InvalidCredentialsException e) {
    return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage());
  }
```

- [ ] **Step 5: 写 AuthController**

`user/AuthController.java`：

```java
package com.gmnl.orientation.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final UserRepository userRepo;
  private final CurrentUserResolver currentUser;

  public AuthController(AuthService authService, UserRepository userRepo, CurrentUserResolver currentUser) {
    this.authService = authService;
    this.userRepo = userRepo;
    this.currentUser = currentUser;
  }

  @PostMapping("/login")
  public AuthService.LoginResult login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req.username(), req.password());
  }

  @GetMapping("/me")
  public UserDto me() {
    return userRepo.findById(currentUser.userId())
        .map(UserDto::from)
        .orElseThrow(() -> new IllegalStateException("token 用户不存在"));
  }

  @PutMapping("/password")
  public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
    authService.changePassword(currentUser.userId(), req.oldPassword(), req.newPassword());
    return ResponseEntity.ok(Map.of("message", "密码已更新"));
  }
}
```

- [ ] **Step 6: 写 AuthServiceTest**

`api/src/test/java/com/gmnl/orientation/user/AuthServiceTest.java`：

```java
package com.gmnl.orientation.user;

import com.gmnl.orientation.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

  private UserRepository userRepo;
  private AuthService authService;

  @BeforeEach
  void setup() {
    userRepo = mock(UserRepository.class);
    var encoder = new BCryptPasswordEncoder(4); // 测试用低 cost 提速
    var jwt = new JwtService("test-secret-test-secret-test-secret-test-secret-32b", 8);
    authService = new AuthService(userRepo, encoder, jwt);
  }

  @Test
  void loginWithCorrectPasswordSucceeds() {
    User u = new User();
    u.setId(1L);
    u.setUsername("admin");
    u.setName("管理员");
    u.setRole(UserRole.ADMIN);
    u.setPasswordHash(new BCryptPasswordEncoder(4).encode("admin12345"));
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(u));

    var result = authService.login("admin", "admin12345");
    assertEquals("admin", result.user().username());
    assertNotNull(result.token());
  }

  @Test
  void loginWithWrongPasswordThrowsInvalid() {
    User u = new User();
    u.setUsername("admin");
    u.setPasswordHash(new BCryptPasswordEncoder(4).encode("admin12345"));
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(u));

    assertThrows(AuthService.InvalidCredentialsException.class,
        () -> authService.login("admin", "wrong"));
  }

  @Test
  void loginWithUnknownUserThrowsInvalid() {
    when(userRepo.findByUsername("nobody")).thenReturn(Optional.empty());
    assertThrows(AuthService.InvalidCredentialsException.class,
        () -> authService.login("nobody", "whatever"));
  }
}
```

- [ ] **Step 7: 运行测试**

```bash
cd api && mvn -q test -Dtest=AuthServiceTest
# Expected: Tests run: 3, Failures: 0
```

- [ ] **Step 8: 端到端验证登录**

```bash
cd api && mvn -q spring-boot:run &
sleep 20
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin12345"}'
# Expected: {"token":"...","user":{"id":1,"username":"admin","name":"管理员","role":"ADMIN"}}
curl -s http://localhost:8080/api/auth/me
# Expected: 401 （未带 token）
# 停止后台进程
```

- [ ] **Step 9: 提交**

```bash
git add api/src/main/java/com/gmnl/orientation/user api/src/test/java/com/gmnl/orientation/user
git commit -m "feat(api): auth login/me/change-password endpoints with tests"
```

---

### Task 9: 内容 API（机构/小岛/文档 + 文档下载）

**Files:**
- Create: `api/src/main/java/com/gmnl/orientation/content/ContentController.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/ContentService.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/DocDto.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/IslandDto.java`
- Create: `api/src/main/java/com/gmnl/orientation/content/InstitutionDto.java`
- Create: `api/src/main/java/com/gmnl/orientation/config/WebConfig.java`

**Interfaces:**
- Consumes: `InstitutionRepository.findAllByOrderByOrderAsc`、`IslandRepository.findByInstitutionIdOrderByOrderAsc`、`DocRepository.findByIslandIdAndActiveTrueOrderByOrderAsc`。
- Produces REST：
  - `GET /api/institutions` → `200 [InstitutionDto]`（含嵌套 islands）
  - `GET /api/islands?institutionId=` → `200 [IslandDto]`
  - `GET /api/docs?islandId=&required=` → `200 [DocDto]`
  - `GET /api/docs/{id}` → `200 DocDto` 或 `404`
  - `GET /api/docs/{id}/file` → `200` 流式文件 或 `404`（无文件）/ `403`

- [ ] **Step 1: 写 DTO**

`content/InstitutionDto.java`：

```java
package com.gmnl.orientation.content;

import java.util.List;

public record InstitutionDto(Long id, String key, String name, Integer order, List<IslandDto> islands) {}
```

`content/IslandDto.java`：

```java
package com.gmnl.orientation.content;

public record IslandDto(Long id, String key, String name, Integer order, Long institutionId) {
  public static IslandDto from(Island i) {
    return new IslandDto(i.getId(), i.getKey(), i.getName(), i.getOrder(), i.getInstitutionId());
  }
}
```

`content/DocDto.java`：

```java
package com.gmnl.orientation.content;

public record DocDto(Long id, String title, String category, Long institutionId, Long islandId,
                     Boolean required, String fileType, Integer order, Boolean active) {
  public static DocDto from(Doc d) {
    return new DocDto(d.getId(), d.getTitle(), d.getCategory(), d.getInstitutionId(),
        d.getIslandId(), d.getRequired(), d.getFileType(), d.getOrder(), d.getActive());
  }
}
```

- [ ] **Step 2: 写 ContentService**

`content/ContentService.java`：

```java
package com.gmnl.orientation.content;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentService {

  private final InstitutionRepository institutionRepo;
  private final IslandRepository islandRepo;
  private final DocRepository docRepo;

  public ContentService(InstitutionRepository institutionRepo, IslandRepository islandRepo, DocRepository docRepo) {
    this.institutionRepo = institutionRepo;
    this.islandRepo = islandRepo;
    this.docRepo = docRepo;
  }

  public List<InstitutionDto> listInstitutions() {
    return institutionRepo.findAllByOrderByOrderAsc().stream().map(inst -> {
      List<IslandDto> islands = islandRepo.findByInstitutionIdOrderByOrderAsc(inst.getId())
          .stream().map(IslandDto::from).toList();
      return new InstitutionDto(inst.getId(), inst.getKey(), inst.getName(), inst.getOrder(), islands);
    }).toList();
  }

  public List<IslandDto> listIslands(Long institutionId) {
    if (institutionId != null) {
      return islandRepo.findByInstitutionIdOrderByOrderAsc(institutionId).stream().map(IslandDto::from).toList();
    }
    return islandRepo.findAllByOrderByOrderAsc().stream().map(IslandDto::from).toList();
  }

  public List<DocDto> listDocs(Long islandId, Boolean required) {
    List<Doc> docs = (islandId != null)
        ? docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(islandId)
        : docRepo.findAll().stream().filter(Doc::getActive).sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder())).toList();
    return docs.stream()
        .filter(d -> required == null || d.getRequired().equals(required))
        .map(DocDto::from)
        .toList();
  }

  public Doc getDocEntity(Long id) {
    return docRepo.findById(id)
        .filter(Doc::getActive)
        .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
  }
}
```

- [ ] **Step 3: 写 ContentController**

`content/ContentController.java`：

```java
package com.gmnl.orientation.content;

import com.gmnl.orientation.common.ApiError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ContentController {

  private final ContentService contentService;
  private final String uploadsDir;

  public ContentController(ContentService contentService,
                           @Value("${app.uploads.dir:./uploads}") String uploadsDir) {
    this.contentService = contentService;
    this.uploadsDir = uploadsDir;
  }

  @GetMapping("/institutions")
  public List<InstitutionDto> institutions() {
    return contentService.listInstitutions();
  }

  @GetMapping("/islands")
  public List<IslandDto> islands(@RequestParam(required = false) Long institutionId) {
    return contentService.listIslands(institutionId);
  }

  @GetMapping("/docs")
  public List<DocDto> docs(@RequestParam(required = false) Long islandId,
                           @RequestParam(required = false) Boolean required) {
    return contentService.listDocs(islandId, required);
  }

  @GetMapping("/docs/{id}")
  public DocDto doc(@PathVariable Long id) {
    return DocDto.from(contentService.getDocEntity(id));
  }

  @GetMapping("/docs/{id}/file")
  public ResponseEntity<?> downloadFile(@PathVariable Long id) {
    Doc doc = contentService.getDocEntity(id);
    if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ApiError("NO_FILE", "该文档尚未上传文件"));
    }
    File file = new File(uploadsDir, doc.getFilePath());
    if (!file.exists()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ApiError("NO_FILE", "文件不存在"));
    }
    String filename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(file.length())
        .body(new FileSystemResource(file));
  }
}
```

- [ ] **Step 4: 写 WebConfig（uploads 不挂公开静态目录，仅 CORS 给前端 dev）**

`config/WebConfig.java`：

```java
package com.gmnl.orientation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:5174")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
```

- [ ] **Step 5: 编译与端到端验证**

```bash
cd api && mvn -q compile
cd api && mvn -q spring-boot:run &
sleep 20
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin12345"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s http://localhost:8080/api/institutions -H "Authorization: Bearer $TOKEN"
# Expected: 7 个机构 JSON，每个含 islands 数组（5 个）
curl -s "http://localhost:8080/api/docs?islandId=1" -H "Authorization: Bearer $TOKEN"
# Expected: 该小岛下 1 个文档
curl -s http://localhost:8080/api/docs/1/file -H "Authorization: Bearer $TOKEN"
# Expected: 404 {"code":"NO_FILE",...}（占位文档无文件）
# 停止后台进程
```

- [ ] **Step 6: 提交**

```bash
git add api/src/main/java/com/gmnl/orientation/content/ContentController.java api/src/main/java/com/gmnl/orientation/content/ContentService.java api/src/main/java/com/gmnl/orientation/content/InstitutionDto.java api/src/main/java/com/gmnl/orientation/content/IslandDto.java api/src/main/java/com/gmnl/orientation/content/DocDto.java api/src/main/java/com/gmnl/orientation/config/WebConfig.java
git commit -m "feat(api): content read endpoints and authenticated doc download"
```

---

### Task 10: 学习进度 API（上报 / 聚合 / 小岛状态）

**Files:**
- Create: `api/src/main/java/com/gmnl/orientation/progress/ProgressController.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/ProgressService.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/UpsertProgressRequest.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/ProgressItemDto.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/IslandStateViewDto.java`
- Create: `api/src/main/java/com/gmnl/orientation/progress/ProgressAggregateDto.java`
- Test: `api/src/test/java/com/gmnl/orientation/progress/ProgressServiceTest.java`

**Interfaces:**
- Consumes: `ProgressRepository`、`IslandStateRepository`、`DocRepository`、`IslandRepository`、`CurrentUserResolver`。
- Produces REST：
  - `GET /api/progress` → `200 ProgressAggregateDto{documents:[ProgressItemDto], islands:[IslandStateViewDto]}`
  - `PUT /api/progress/{docId}` body `UpsertProgressRequest{status, progressPct}` → `200 ProgressItemDto`（幂等 upsert，progressPct 单调不回退）
  - `POST /api/progress/{docId}/complete` → `200 ProgressItemDto`
- 关键规则：upsert 时新 `progressPct` 小于已存值则保留已存值（不回退）；`status=COMPLETED` 时 `progressPct=100` 且记 `completedAt`。

- [ ] **Step 1: 写 DTO 与请求体**

`progress/UpsertProgressRequest.java`：

```java
package com.gmnl.orientation.progress;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertProgressRequest(
    @NotNull ProgressStatus status,
    @Min(0) @Max(100) Integer progressPct) {}
```

`progress/ProgressItemDto.java`：

```java
package com.gmnl.orientation.progress;

import java.time.Instant;

public record ProgressItemDto(Long docId, ProgressStatus status, Integer progressPct,
                              Instant lastReadAt, Instant completedAt) {
  public static ProgressItemDto from(Progress p) {
    return new ProgressItemDto(p.getDocId(), p.getStatus(), p.getProgressPct(),
        p.getLastReadAt(), p.getCompletedAt());
  }
}
```

`progress/IslandStateViewDto.java`：

```java
package com.gmnl.orientation.progress;

public record IslandStateViewDto(Long islandId, IslandStatus status,
                                 Integer completedCount, Integer totalCount) {}
```

`progress/ProgressAggregateDto.java`：

```java
package com.gmnl.orientation.progress;

import java.util.List;

public record ProgressAggregateDto(List<ProgressItemDto> documents,
                                   List<IslandStateViewDto> islands) {}
```

- [ ] **Step 2: 写 ProgressService**

`progress/ProgressService.java`：

```java
package com.gmnl.orientation.progress;

import com.gmnl.orientation.content.Doc;
import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.Island;
import com.gmnl.orientation.content.IslandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgressService {

  private final ProgressRepository progressRepo;
  private final IslandStateRepository islandStateRepo;
  private final DocRepository docRepo;
  private final IslandRepository islandRepo;

  public ProgressService(ProgressRepository progressRepo, IslandStateRepository islandStateRepo,
                         DocRepository docRepo, IslandRepository islandRepo) {
    this.progressRepo = progressRepo;
    this.islandStateRepo = islandStateRepo;
    this.docRepo = docRepo;
    this.islandRepo = islandRepo;
  }

  @Transactional
  public ProgressItemDto upsert(Long userId, Long docId, ProgressStatus status, Integer progressPct) {
    int pct = progressPct == null ? 0 : Math.max(0, Math.min(100, progressPct));
    if (status == ProgressStatus.COMPLETED) pct = 100;

    Progress p = progressRepo.findById(new ProgressId(userId, docId)).orElseGet(() -> {
      Progress np = new Progress();
      np.setUserId(userId);
      np.setDocId(docId);
      np.setStatus(ProgressStatus.NOT_STARTED);
      np.setProgressPct(0);
      return np;
    });

    // 单调不回退：只增不减
    if (pct < p.getProgressPct()) pct = p.getProgressPct();
    p.setProgressPct(pct);
    if (pct > 0 || status != ProgressStatus.NOT_STARTED) {
      p.setLastReadAt(Instant.now());
    }
    if (status.ordinal() > p.getStatus().ordinal()) {
      p.setStatus(status);
    }
    if (p.getStatus() == ProgressStatus.COMPLETED) {
      p.setProgressPct(100);
      if (p.getCompletedAt() == null) p.setCompletedAt(Instant.now());
    }
    progressRepo.save(p);

    recomputeIslandState(userId, p.getDocId());
    return ProgressItemDto.from(p);
  }

  @Transactional
  public ProgressItemDto complete(Long userId, Long docId) {
    return upsert(userId, docId, ProgressStatus.COMPLETED, 100);
  }

  @Transactional(readOnly = true)
  public ProgressAggregateDto getAggregate(Long userId) {
    List<Progress> progresses = progressRepo.findByUserId(userId);
    List<ProgressItemDto> docs = progresses.stream().map(ProgressItemDto::from).toList();

    // 聚合小岛状态
    Map<Long, List<Doc>> docsByIsland = docRepo.findAll().stream()
        .filter(Doc::getActive)
        .collect(Collectors.groupingBy(Doc::getIslandId));
    Map<Long, Progress> progByDoc = progresses.stream()
        .collect(Collectors.toMap(Progress::getDocId, pp -> pp));

    List<IslandStateViewDto> islands = new ArrayList<>();
    for (Island island : islandRepo.findAllByOrderByOrderAsc()) {
      List<Doc> islandDocs = docsByIsland.getOrDefault(island.getId(), List.of());
      long completed = islandDocs.stream()
          .filter(d -> {
            Progress pp = progByDoc.get(d.getId());
            return pp != null && pp.getStatus() == ProgressStatus.COMPLETED;
          })
          .count();
      int total = islandDocs.size();
      IslandStatus status = computeIslandStatus(islandDocs, progByDoc);
      islands.add(new IslandStateViewDto(island.getId(), status, (int) completed, total));
    }
    return new ProgressAggregateDto(docs, islands);
  }

  private void recomputeIslandState(Long userId, Long docId) {
    Doc doc = docRepo.findById(docId).orElse(null);
    if (doc == null) return;
    Long islandId = doc.getIslandId();
    List<Doc> islandDocs = docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(islandId);
    Map<Long, Progress> progByDoc = progressRepo.findByUserId(userId).stream()
        .collect(Collectors.toMap(Progress::getDocId, p -> p));
    IslandStatus status = computeIslandStatus(islandDocs, progByDoc);

    IslandState st = islandStateRepo.findById(new IslandStateId(userId, islandId))
        .orElseGet(() -> {
          IslandState ns = new IslandState();
          ns.setUserId(userId);
          ns.setIslandId(islandId);
          ns.setStatus(IslandStatus.LOCKED);
          return ns;
        });
    st.setStatus(status);
    islandStateRepo.save(st);
  }

  /** 规则：必修文档全完成 → COMPLETED；至少有一个非 NOT_STARTED → UNLOCKED；否则 LOCKED。 */
  private IslandStatus computeIslandStatus(List<Doc> islandDocs, Map<Long, Progress> progByDoc) {
    List<Doc> required = islandDocs.stream().filter(Doc::getRequired).toList();
    boolean allRequiredDone = !required.isEmpty() && required.stream().allMatch(d -> {
      Progress p = progByDoc.get(d.getId());
      return p != null && p.getStatus() == ProgressStatus.COMPLETED;
    });
    if (allRequiredDone) return IslandStatus.COMPLETED;
    boolean anyStarted = islandDocs.stream().anyMatch(d -> {
      Progress p = progByDoc.get(d.getId());
      return p != null && p.getStatus() != ProgressStatus.NOT_STARTED;
    });
    return anyStarted ? IslandStatus.UNLOCKED : IslandStatus.LOCKED;
  }
}
```

- [ ] **Step 3: 写 ProgressController**

`progress/ProgressController.java`：

```java
package com.gmnl.orientation.progress;

import com.gmnl.orientation.user.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

  private final ProgressService progressService;
  private final CurrentUserResolver currentUser;

  public ProgressController(ProgressService progressService, CurrentUserResolver currentUser) {
    this.progressService = progressService;
    this.currentUser = currentUser;
  }

  @GetMapping
  public ProgressAggregateDto getAggregate() {
    return progressService.getAggregate(currentUser.userId());
  }

  @PutMapping("/{docId}")
  public ProgressItemDto upsert(@PathVariable Long docId, @Valid @RequestBody UpsertProgressRequest req) {
    return progressService.upsert(currentUser.userId(), docId, req.status(), req.progressPct());
  }

  @PostMapping("/{docId}/complete")
  public ProgressItemDto complete(@PathVariable Long docId) {
    return progressService.complete(currentUser.userId(), docId);
  }
}
```

- [ ] **Step 4: 写 ProgressServiceTest**

`api/src/test/java/com/gmnl/orientation/progress/ProgressServiceTest.java`：

```java
package com.gmnl.orientation.progress;

import com.gmnl.orientation.content.Doc;
import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.Island;
import com.gmnl.orientation.content.IslandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgressServiceTest {

  private ProgressRepository progressRepo;
  private IslandStateRepository islandStateRepo;
  private DocRepository docRepo;
  private IslandRepository islandRepo;
  private ProgressService service;

  @BeforeEach
  void setup() {
    progressRepo = mock(ProgressRepository.class);
    islandStateRepo = mock(IslandStateRepository.class);
    docRepo = mock(DocRepository.class);
    islandRepo = mock(IslandRepository.class);
    service = new ProgressService(progressRepo, islandStateRepo, docRepo, islandRepo);
  }

  @Test
  void upsertCreatesNewProgressWhenAbsent() {
    when(progressRepo.findById(new ProgressId(1L, 10L))).thenReturn(Optional.empty());
    Doc doc = doc(10L, 5L, true);
    when(docRepo.findById(10L)).thenReturn(Optional.of(doc));
    when(docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(5L)).thenReturn(List.of(doc));
    when(progressRepo.findByUserId(1L)).thenReturn(List.of());
    when(islandStateRepo.findById(new IslandStateId(1L, 5L))).thenReturn(Optional.empty());

    var dto = service.upsert(1L, 10L, ProgressStatus.READING, 40);
    assertEquals(40, dto.progressPct());
    assertEquals(ProgressStatus.READING, dto.status());
    assertNotNull(dto.lastReadAt());
  }

  @Test
  void upsertDoesNotRegressProgressPct() {
    Progress existing = new Progress();
    existing.setUserId(1L); existing.setDocId(10L);
    existing.setStatus(ProgressStatus.READING);
    existing.setProgressPct(80);
    when(progressRepo.findById(new ProgressId(1L, 10L))).thenReturn(Optional.of(existing));
    Doc doc = doc(10L, 5L, true);
    when(docRepo.findById(10L)).thenReturn(Optional.of(doc));
    when(docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(5L)).thenReturn(List.of(doc));
    when(progressRepo.findByUserId(1L)).thenReturn(List.of(existing));
    when(islandStateRepo.findById(new IslandStateId(1L, 5L))).thenReturn(Optional.empty());

    var dto = service.upsert(1L, 10L, ProgressStatus.READING, 20);
    // 20 < 80 → 保留 80
    assertEquals(80, dto.progressPct());
  }

  @Test
  void completeSetsFullProgressAndCompletedAt() {
    when(progressRepo.findById(new ProgressId(1L, 10L))).thenReturn(Optional.empty());
    Doc doc = doc(10L, 5L, true);
    when(docRepo.findById(10L)).thenReturn(Optional.of(doc));
    when(docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(5L)).thenReturn(List.of(doc));
    when(progressRepo.findByUserId(1L)).thenReturn(List.of());
    when(islandStateRepo.findById(new IslandStateId(1L, 5L))).thenReturn(Optional.empty());

    var dto = service.complete(1L, 10L);
    assertEquals(100, dto.progressPct());
    assertEquals(ProgressStatus.COMPLETED, dto.status());
    assertNotNull(dto.completedAt());
  }

  @Test
  void islandBecomesCompletedWhenAllRequiredDocsDone() {
    Doc d1 = doc(10L, 5L, true);
    Doc d2 = doc(11L, 5L, true);
    Progress p1 = new Progress(); p1.setDocId(10L); p1.setStatus(ProgressStatus.COMPLETED);
    Progress p2 = new Progress(); p2.setDocId(11L); p2.setStatus(ProgressStatus.COMPLETED);
    when(progressRepo.findByUserId(1L)).thenReturn(List.of(p1, p2));
    when(docRepo.findAll()).thenReturn(List.of(d1, d2));
    Island island = new Island(); island.setId(5L);
    when(islandRepo.findAllByOrderByOrderAsc()).thenReturn(List.of(island));

    var agg = service.getAggregate(1L);
    IslandStateViewDto view = agg.islands().get(0);
    assertEquals(IslandStatus.COMPLETED, view.status());
    assertEquals(2, view.completedCount());
    assertEquals(2, view.totalCount());
  }

  private Doc doc(long id, long islandId, boolean required) {
    Doc d = new Doc();
    d.setId(id);
    d.setIslandId(islandId);
    d.setRequired(required);
    d.setActive(true);
    return d;
  }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd api && mvn -q test -Dtest=ProgressServiceTest
# Expected: Tests run: 4, Failures: 0
```

- [ ] **Step 6: 端到端验证**

```bash
cd api && mvn -q spring-boot:run &
sleep 20
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin12345"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s http://localhost:8080/api/progress -H "Authorization: Bearer $TOKEN"
# Expected: {"documents":[],"islands":[...35 个，全 LOCKED...]}
curl -s -X PUT http://localhost:8080/api/progress/1 -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"status":"READING","progressPct":50}'
# Expected: {"docId":1,"status":"READING","progressPct":50,...}
curl -s -X POST http://localhost:8080/api/progress/1/complete -H "Authorization: Bearer $TOKEN"
# Expected: status=COMPLETED, progressPct=100
# 停止后台进程
```

- [ ] **Step 7: 提交**

```bash
git add api/src/main/java/com/gmnl/orientation/progress/ProgressController.java api/src/main/java/com/gmnl/orientation/progress/ProgressService.java api/src/main/java/com/gmnl/orientation/progress/UpsertProgressRequest.java api/src/main/java/com/gmnl/orientation/progress/ProgressItemDto.java api/src/main/java/com/gmnl/orientation/progress/IslandStateViewDto.java api/src/main/java/com/gmnl/orientation/progress/ProgressAggregateDto.java api/src/test/java/com/gmnl/orientation/progress
git commit -m "feat(api): progress upsert, aggregate, island state computation with tests"
```

---

### Task 11: web 前端骨架（Vite + React + TS + 路由 + axios + Zustand）

**Files:**
- Create: `apps/web/package.json`
- Create: `apps/web/tsconfig.json`
- Create: `apps/web/vite.config.ts`
- Create: `apps/web/index.html`
- Create: `apps/web/src/main.tsx`
- Create: `apps/web/src/App.tsx`
- Create: `apps/web/src/api/client.ts`

**Interfaces:**
- Consumes: `@gmnl/shared`（Task 2）。
- Produces: 可 `pnpm dev` 启动的空壳 React 应用，含路由骨架与 axios 客户端（带 token 拦截器 + 401 重定向）。后续任务加登录页与业务页。

- [ ] **Step 1: 写 package.json**

`apps/web/package.json`：

```json
{
  "name": "@gmnl/web",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@gmnl/shared": "workspace:*",
    "axios": "^1.7.2",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.24.0",
    "zustand": "^4.5.4"
  },
  "devDependencies": {
    "@types/react": "^18.3.3",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.3.1",
    "typescript": "^5.5.3",
    "vite": "^5.3.3"
  }
}
```

- [ ] **Step 2: 写 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true
  },
  "include": ["src"]
}
```

- [ ] **Step 3: 写 vite.config.ts**

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
```

- [ ] **Step 4: 写 index.html**

`apps/web/index.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>国民养老 · 新人航行计划</title>
</head>
<body>
  <div id="root"></div>
  <script type="module" src="/src/main.tsx"></script>
</body>
</html>
```

- [ ] **Step 5: 写 api/client.ts**

`apps/web/src/api/client.ts`：

```ts
import axios from 'axios';
import { API_BASE } from '@gmnl/shared';

const client = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(err);
  },
);

export default client;
```

- [ ] **Step 6: 写 main.tsx 与 App.tsx 骨架**

`apps/web/src/main.tsx`：

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>,
);
```

`apps/web/src/App.tsx`：

```tsx
import { Routes, Route, Navigate } from 'react-router-dom';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<div>登录页占位</div>} />
      <Route path="/" element={<div>航行首页占位</div>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
```

- [ ] **Step 7: 安装依赖并启动验证**

```bash
pnpm install
pnpm dev:web
# 浏览器打开 http://localhost:5173 看到"航行首页占位"
# Ctrl+C 停止
```

- [ ] **Step 8: 提交**

```bash
git add apps/web pnpm-lock.yaml
git commit -m "feat(web): vite react scaffold with axios client and routing"
```

---

### Task 12: web 登录页 + auth/progress store + 路由守卫

**Files:**
- Create: `apps/web/src/stores/authStore.ts`
- Create: `apps/web/src/stores/progressStore.ts`
- Create: `apps/web/src/pages/LoginPage.tsx`
- Create: `apps/web/src/components/RequireAuth.tsx`
- Modify: `apps/web/src/App.tsx`

**Interfaces:**
- Consumes: `@gmnl/shared` 类型、`api/client.ts`、后端 `/api/auth/login`、`/api/auth/me`、`/api/progress`。
- Produces: 登录页（姓名全拼+密码）、登录后 token+user 存 `authStore`（localStorage 持久化）、`RequireAuth` 守卫包裹业务路由、`progressStore` 提供 `loadProgress`/`upsertProgress`/`completeDoc`。

- [ ] **Step 1: 写 authStore**

`apps/web/src/stores/authStore.ts`：

```ts
import { create } from 'zustand';
import type { User, LoginRequest } from '@gmnl/shared';
import client from '../api/client';

interface AuthState {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (req: LoginRequest) => Promise<void>;
  fetchMe: () => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: localStorage.getItem('token'),
  loading: false,
  login: async (req) => {
    const { data } = await client.post('/auth/login', req);
    localStorage.setItem('token', data.token);
    set({ token: data.token, user: data.user });
  },
  fetchMe: async () => {
    set({ loading: true });
    try {
      const { data } = await client.get('/auth/me');
      set({ user: data, loading: false });
    } catch {
      localStorage.removeItem('token');
      set({ token: null, user: null, loading: false });
    }
  },
  logout: () => {
    localStorage.removeItem('token');
    set({ token: null, user: null });
  },
}));
```

- [ ] **Step 2: 写 progressStore**

`apps/web/src/stores/progressStore.ts`：

```ts
import { create } from 'zustand';
import type { ProgressAggregate, DocStatus, UpsertProgressRequest } from '@gmnl/shared';
import client from '../api/client';

interface ProgressState {
  aggregate: ProgressAggregate | null;
  loadProgress: () => Promise<void>;
  upsertProgress: (docId: number, status: DocStatus, progressPct: number) => Promise<void>;
  completeDoc: (docId: number) => Promise<void>;
}

export const useProgressStore = create<ProgressState>((set, get) => ({
  aggregate: null,
  loadProgress: async () => {
    const { data } = await client.get('/progress');
    set({ aggregate: data });
  },
  upsertProgress: async (docId, status, progressPct) => {
    const body: UpsertProgressRequest = { status, progressPct };
    await client.put(`/progress/${docId}`, body);
    // 简化：上报后重新拉取聚合（后端单调不回退保证正确）
    await get().loadProgress();
  },
  completeDoc: async (docId) => {
    await client.post(`/progress/${docId}/complete`);
    await get().loadProgress();
  },
}));
```

- [ ] **Step 3: 写 RequireAuth 守卫**

`apps/web/src/components/RequireAuth.tsx`：

```tsx
import { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const fetchMe = useAuthStore((s) => s.fetchMe);

  useEffect(() => {
    if (token && !user) fetchMe();
  }, [token, user, fetchMe]);

  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}
```

- [ ] **Step 4: 写 LoginPage**

`apps/web/src/pages/LoginPage.tsx`：

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login({ username: username.trim(), password });
      navigate('/', { replace: true });
    } catch {
      setError('账号或密码错误');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ maxWidth: 360, margin: '80px auto', fontFamily: 'sans-serif' }}>
      <h2>新人航行计划 · 登录</h2>
      <form onSubmit={submit}>
        <div style={{ marginBottom: 12 }}>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="姓名全拼"
            style={{ width: '100%', padding: 8, boxSizing: 'border-box' }}
          />
        </div>
        <div style={{ marginBottom: 12 }}>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="密码"
            style={{ width: '100%', padding: 8, boxSizing: 'border-box' }}
          />
        </div>
        {error && <div style={{ color: 'red', marginBottom: 12 }}>{error}</div>}
        <button type="submit" disabled={busy} style={{ width: '100%', padding: 10 }}>
          {busy ? '登录中…' : '登录'}
        </button>
      </form>
      <p style={{ color: '#888', fontSize: 12, marginTop: 16 }}>
        试点账号 admin / admin12345
      </p>
    </div>
  );
}
```

- [ ] **Step 5: 改 App.tsx 接入路由守卫**

替换 `apps/web/src/App.tsx`：

```tsx
import { Routes, Route, Navigate } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
import LoginPage from './pages/LoginPage';
import VoyagePage from './pages/VoyagePage';
import IslandPage from './pages/IslandPage';
import DocPage from './pages/DocPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RequireAuth><VoyagePage /></RequireAuth>} />
      <Route path="/island/:islandId" element={<RequireAuth><IslandPage /></RequireAuth>} />
      <Route path="/doc/:docId" element={<RequireAuth><DocPage /></RequireAuth>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
```

（VoyagePage/IslandPage/DocPage 在 Task 13 创建；本任务先创建占位文件让编译通过。）

- [ ] **Step 6: 占位页面（Task 13 替换）**

`apps/web/src/pages/VoyagePage.tsx`：

```tsx
export default function VoyagePage() { return <div>航行首页</div>; }
```

`apps/web/src/pages/IslandPage.tsx`：

```tsx
export default function IslandPage() { return <div>小岛文档列表</div>; }
```

`apps/web/src/pages/DocPage.tsx`：

```tsx
export default function DocPage() { return <div>文档阅读</div>; }
```

- [ ] **Step 7: 验证登录闭环**

后端先跑起来（`cd api && mvn spring-boot:run`），再：

```bash
pnpm dev:web
# 浏览器 http://localhost:5173/login
# 输入 admin / admin12345，应跳转到首页"航行首页"
# 刷新页面应保持登录（fetchMe 兜底）
# Ctrl+C 停止
```

- [ ] **Step 8: 提交**

```bash
git add apps/web/src
git commit -m "feat(web): login page, auth/progress stores, route guard"
```

---

### Task 13: web 业务页（机构/小岛列表 + 文档阅读与进度上报）

**Files:**
- Modify: `apps/web/src/pages/VoyagePage.tsx`
- Modify: `apps/web/src/pages/IslandPage.tsx`
- Modify: `apps/web/src/pages/DocPage.tsx`
- Create: `apps/web/src/api/content.ts`
- Create: `apps/web/src/api/progress.ts`

**Interfaces:**
- Consumes: 后端 `/api/institutions`、`/api/docs`、`/api/progress`、`/api/progress/{docId}`、`/api/docs/{id}/file`；`@gmnl/shared` 类型；`progressStore`。
- Produces：三页业务闭环——首页展示机构与小岛聚合进度，点小岛进文档列表，点文档进阅读页（滚动驱动进度节流上报 + 离开强制上报 + 完成按钮）。

- [ ] **Step 1: 写 content/progress api 封装**

`apps/web/src/api/content.ts`：

```ts
import client from './client';
import type { Institution, Doc } from '@gmnl/shared';

export const fetchInstitutions = async (): Promise<Institution[]> =>
  (await client.get('/institutions')).data;

export const fetchDocs = async (islandId: number): Promise<Doc[]> =>
  (await client.get('/docs', { params: { islandId } })).data;

export const docFileUrl = (docId: number): string =>
  `/api/docs/${docId}/file`;
```

`apps/web/src/api/progress.ts`：

```ts
import client from './client';
import type { ProgressAggregate } from '@gmnl/shared';

export const fetchProgress = async (): Promise<ProgressAggregate> =>
  (await client.get('/progress')).data;
```

- [ ] **Step 2: 写 VoyagePage（机构 + 小岛聚合进度）**

替换 `apps/web/src/pages/VoyagePage.tsx`：

```tsx
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchInstitutions } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import { useAuthStore } from '../stores/authStore';
import type { Institution, IslandStateView } from '@gmnl/shared';

export default function VoyagePage() {
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const aggregate = useProgressStore((s) => s.aggregate);
  const loadProgress = useProgressStore((s) => s.loadProgress);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  useEffect(() => {
    fetchInstitutions().then(setInstitutions);
    loadProgress();
  }, [loadProgress]);

  const islandMap = new Map<number, IslandStateView>();
  aggregate?.islands.forEach((i) => islandMap.set(i.islandId, i));

  return (
    <div style={{ fontFamily: 'sans-serif', padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24 }}>
        <h1>新人航行计划</h1>
        <div>
          <span style={{ marginRight: 12 }}>{user?.name}</span>
          <button onClick={logout}>退出</button>
        </div>
      </div>
      {institutions.map((inst) => (
        <div key={inst.id} style={{ marginBottom: 24, padding: 16, border: '1px solid #ddd', borderRadius: 8 }}>
          <h2>{inst.name}</h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            {inst.islands.map((isl) => {
              const st = islandMap.get(isl.id);
              const label = st?.status === 'COMPLETED' ? '✅' : st?.status === 'UNLOCKED' ? '🔓' : '🔒';
              return (
                <Link key={isl.id} to={`/island/${isl.id}`} style={{ textDecoration: 'none', color: '#333' }}>
                  <div style={{ padding: 12, background: '#f5f7fb', borderRadius: 8, minWidth: 120 }}>
                    <div>{label} {isl.name}</div>
                    <div style={{ fontSize: 12, color: '#888' }}>
                      {st ? `${st.completedCount}/${st.totalCount}` : '0/0'}
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 3: 写 IslandPage（小岛下文档列表）**

替换 `apps/web/src/pages/IslandPage.tsx`：

```tsx
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchDocs } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import type { Doc } from '@gmnl/shared';

export default function IslandPage() {
  const { islandId } = useParams();
  const [docs, setDocs] = useState<Doc[]>([]);
  const aggregate = useProgressStore((s) => s.aggregate);
  const loadProgress = useProgressStore((s) => s.loadProgress);

  useEffect(() => {
    if (!islandId) return;
    fetchDocs(Number(islandId)).then(setDocs);
    loadProgress();
  }, [islandId, loadProgress]);

  const progMap = new Map<number, number>();
  aggregate?.documents.forEach((p) => progMap.set(p.docId, p.progressPct));

  return (
    <div style={{ fontFamily: 'sans-serif', padding: 24 }}>
      <Link to="/">← 返回</Link>
      <h1>小岛文档</h1>
      {docs.map((d) => (
        <Link key={d.id} to={`/doc/${d.id}`} style={{ textDecoration: 'none', color: '#333' }}>
          <div style={{ padding: 12, marginBottom: 8, border: '1px solid #eee', borderRadius: 8 }}>
            <div>{d.required ? '★' : '○'} {d.title}</div>
            <div style={{ height: 6, background: '#eee', borderRadius: 3, marginTop: 8 }}>
              <div style={{ width: `${progMap.get(d.id) ?? 0}%`, height: '100%', background: '#4A7BE0', borderRadius: 3 }} />
            </div>
          </div>
        </Link>
      ))}
    </div>
  );
}
```

- [ ] **Step 4: 写 DocPage（阅读 + 节流上报 + 离开强制上报 + 完成）**

替换 `apps/web/src/pages/DocPage.tsx`：

```tsx
import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchDocs } from '../api/content';
import { docFileUrl } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import client from '../api/client';
import type { Doc, DocStatus } from '@gmnl/shared';

export default function DocPage() {
  const { docId } = useParams();
  const [doc, setDoc] = useState<Doc | null>(null);
  const upsertProgress = useProgressStore((s) => s.upsertProgress);
  const completeDoc = useProgressStore((s) => s.completeDoc);
  const aggregate = useProgressStore((s) => s.aggregate);
  const pctRef = useRef(0);
  const lastReportRef = useRef(0);

  // 进入即 READING
  useEffect(() => {
    if (!docId) return;
    fetchDoc(Number(docId)).then(setDoc);
    upsertProgress(Number(docId), 'READING', 1);
  }, [docId, upsertProgress]);

  // 滚动驱动进度
  useEffect(() => {
    const onScroll = () => {
      const el = document.documentElement;
      const total = el.scrollHeight - el.clientHeight;
      const pct = total > 0 ? Math.min(100, Math.round((el.scrollTop / total) * 100)) : 100;
      pctRef.current = Math.max(pctRef.current, pct);
      const now = Date.now();
      if (pct - lastReportRef.current >= 5 && now - lastReportTime() >= 10000) {
        lastReportRef.current = pct;
        upsertProgress(Number(docId), 'READING', pct);
      }
    };
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, [docId, upsertProgress]);

  // 离开强制上报最终值
  useEffect(() => {
    return () => {
      if (docId && pctRef.current > 0) {
        upsertProgress(Number(docId), 'READING', pctRef.current);
      }
    };
  }, [docId, upsertProgress]);

  const currentPct = aggregate?.documents.find((p) => p.docId === Number(docId))?.progressPct ?? 0;

  const download = async () => {
    if (!doc) return;
    const res = await client.get(`/docs/${doc.id}/file`, { responseType: 'blob' });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = doc.title;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ fontFamily: 'sans-serif', padding: 24 }}>
      <Link to={doc ? `/island/${doc.islandId}` : '/'}>← 返回</Link>
      {doc && (
        <>
          <h1>{doc.title}</h1>
          <div style={{ height: 8, background: '#eee', borderRadius: 4, margin: '12px 0' }}>
            <div style={{ width: `${currentPct}%`, height: '100%', background: '#4BBF92', borderRadius: 4 }} />
          </div>
          <div style={{ color: '#888', marginBottom: 16 }}>当前进度 {currentPct}%</div>
          {doc.fileType ? (
            <button onClick={download}>下载文件</button>
          ) : (
            <div style={{ color: '#999' }}>该文档尚未上传文件（占位）</div>
          )}
          <div style={{ marginTop: 24 }}>
            <button onClick={() => completeDoc(Number(docId))}>标记完成</button>
          </div>
          <div style={{ marginTop: 32, padding: 16, background: '#f7f7f7', borderRadius: 8 }}>
            （此处为文档正文占位。阶段 4 接入真实文件预览/下载，阶段 6 迁移 Demo 视觉。）
          </div>
        </>
      )}
    </div>
  );
}

let lastReportTs = 0;
function lastReportTime() { return lastReportTs; }
export function _setLastReportTime(ts: number) { lastReportTs = ts; } // 测试钩子

async function fetchDoc(id: number): Promise<Doc | null> {
  const { data } = await client.get(`/docs/${id}`);
  return data;
}
```

> 说明：节流逻辑用模块级 `lastReportTs` 记录上次上报时间；离开页面时 `useEffect` cleanup 强制上报最终进度。后端单调不回退兜底，重复上报安全。

- [ ] **Step 5: 类型检查与构建**

```bash
pnpm --filter @gmnl/web build
# Expected: 构建成功，无 TS 报错
```

- [ ] **Step 6: 端到端验证完整闭环**

后端运行中，前端：

```bash
pnpm dev:web
# 1. 登录 admin/admin12345
# 2. 首页看到 7 机构 × 5 小岛，初始全 🔒
# 3. 点任一小岛 → 文档列表（1 个占位文档，进度 0）
# 4. 点文档 → 阅读页，滚动后进度条增长（节流上报）
# 5. 点"标记完成" → 回列表进度变 100%，首页小岛变 ✅
# Ctrl+C 停止
```

- [ ] **Step 7: 提交**

```bash
git add apps/web/src
git commit -m "feat(web): voyage/island/doc pages with throttled progress reporting"
```

---

### Task 14: admin 后台登录骨架

**Files:**
- Create: `apps/admin/package.json`
- Create: `apps/admin/tsconfig.json`
- Create: `apps/admin/vite.config.ts`
- Create: `apps/admin/index.html`
- Create: `apps/admin/src/main.tsx`
- Create: `apps/admin/src/App.tsx`
- Create: `apps/admin/src/api/client.ts`
- Create: `apps/admin/src/stores/authStore.ts`
- Create: `apps/admin/src/pages/LoginPage.tsx`
- Create: `apps/admin/src/pages/DashboardPage.tsx`

**Interfaces:**
- Consumes: `@gmnl/shared`、后端 `/api/auth/login`、`/api/auth/me`。
- Produces: 可启动的 admin 应用，登录后校验 `role=ADMIN`，非管理员重定向回登录；Dashboard 占位。后台 CRUD 在后续计划。

- [ ] **Step 1: 写 package.json**

`apps/admin/package.json`：

```json
{
  "name": "@gmnl/admin",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@gmnl/shared": "workspace:*",
    "antd": "^5.19.0",
    "axios": "^1.7.2",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.24.0",
    "zustand": "^4.5.4"
  },
  "devDependencies": {
    "@types/react": "^18.3.3",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.3.1",
    "typescript": "^5.5.3",
    "vite": "^5.3.3"
  }
}
```

- [ ] **Step 2: 写 tsconfig.json（同 web）**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true
  },
  "include": ["src"]
}
```

- [ ] **Step 3: 写 vite.config.ts（base=/admin/，端口 5174）**

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: '/admin/',
  server: {
    port: 5174,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
```

- [ ] **Step 4: 写 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>新人航行计划 · 管理后台</title>
</head>
<body>
  <div id="root"></div>
  <script type="module" src="/src/main.tsx"></script>
</body>
</html>
```

- [ ] **Step 5: 写 api/client.ts 与 authStore（与 web 同构）**

`apps/admin/src/api/client.ts`：

```ts
import axios from 'axios';
import { API_BASE } from '@gmnl/shared';

const client = axios.create({ baseURL: API_BASE, timeout: 15000 });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !window.location.pathname.startsWith('/admin/login')) {
      window.location.href = '/admin/login';
    }
    return Promise.reject(err);
  },
);

export default client;
```

`apps/admin/src/stores/authStore.ts`：

```ts
import { create } from 'zustand';
import type { User, LoginRequest } from '@gmnl/shared';
import client from '../api/client';

interface AuthState {
  user: User | null;
  token: string | null;
  login: (req: LoginRequest) => Promise<void>;
  fetchMe: () => Promise<boolean>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: localStorage.getItem('admin_token'),
  login: async (req) => {
    const { data } = await client.post('/auth/login', req);
    if (data.user.role !== 'ADMIN') throw new Error('非管理员账号');
    localStorage.setItem('admin_token', data.token);
    set({ token: data.token, user: data.user });
  },
  fetchMe: async () => {
    try {
      const { data } = await client.get('/auth/me');
      if (data.role !== 'ADMIN') { localStorage.removeItem('admin_token'); set({ token: null, user: null }); return false; }
      set({ user: data }); return true;
    } catch { localStorage.removeItem('admin_token'); set({ token: null, user: null }); return false; }
  },
  logout: () => { localStorage.removeItem('admin_token'); set({ token: null, user: null }); },
}));
```

- [ ] **Step 6: 写 LoginPage（Ant Design）**

`apps/admin/src/pages/LoginPage.tsx`：

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, message } from 'antd';
import { useAuthStore } from '../stores/authStore';

export default function LoginPage() {
  const [busy, setBusy] = useState(false);
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  const onFinish = async (v: { username: string; password: string }) => {
    setBusy(true);
    try {
      await login({ username: v.username.trim(), password: v.password });
      navigate('/admin/', { replace: true });
    } catch {
      message.error('账号或密码错误，或非管理员');
    } finally { setBusy(false); }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
      <Card title="管理后台登录" style={{ width: 360 }}>
        <Form onFinish={onFinish}>
          <Form.Item name="username" rules={[{ required: true, message: '请输入姓名全拼' }]}>
            <Input placeholder="姓名全拼" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password placeholder="密码" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={busy}>登录</Button>
        </Form>
      </Card>
    </div>
  );
}
```

- [ ] **Step 7: 写 DashboardPage 占位 + App + main**

`apps/admin/src/pages/DashboardPage.tsx`：

```tsx
import { Button } from 'antd';
import { useAuthStore } from '../stores/authStore';

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  return (
    <div style={{ padding: 24 }}>
      <h1>管理后台 · 概览</h1>
      <p>欢迎，{user?.name}（{user?.username}）</p>
      <p>后台管理功能（用户/机构/小岛/文档/统计）将在后续计划实现。</p>
      <Button onClick={logout}>退出</Button>
    </div>
  );
}
```

`apps/admin/src/App.tsx`：

```tsx
import { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';

function RequireAdmin({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const fetchMe = useAuthStore((s) => s.fetchMe);
  useEffect(() => { if (token && !user) fetchMe(); }, [token, user, fetchMe]);
  if (!token) return <Navigate to="/admin/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/admin/login" element={<LoginPage />} />
      <Route path="/admin/" element={<RequireAdmin><DashboardPage /></RequireAdmin>} />
      <Route path="*" element={<Navigate to="/admin/" replace />} />
    </Routes>
  );
}
```

`apps/admin/src/main.tsx`：

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>,
);
```

- [ ] **Step 8: 安装与验证**

```bash
pnpm install
pnpm dev:admin
# http://localhost:5174/admin/login
# 用 admin/admin12345 登录 → 进入概览页
# 用普通用户登录应提示"非管理员"（当前无普通用户种子，admin 即可验证）
# Ctrl+C 停止
```

- [ ] **Step 9: 提交**

```bash
git add apps/admin pnpm-lock.yaml
git commit -m "feat(admin): login scaffold with admin-only guard"
```

---

### Task 15: 后端托管前端构建产物（静态资源映射）

**Files:**
- Modify: `api/src/main/java/com/gmnl/orientation/config/WebConfig.java`
- Modify: `api/src/main/resources/application.yml`

**Interfaces:**
- Produces: 单 JVM 进程同时托管 `/api/**`（Controller）+ `/admin/**`（admin dist）+ `/`（web dist）。部署时前端构建产物复制到 `api/dist/web` 与 `api/dist/admin`，由 Spring 静态资源映射提供。

- [ ] **Step 1: 改 WebConfig 增加静态资源映射**

替换 `api/src/main/java/com/gmnl/orientation/config/WebConfig.java`：

```java
package com.gmnl.orientation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${app.static.web-dir:./dist/web}")
  private String webDir;
  @Value("${app.static.admin-dir:./dist/admin}")
  private String adminDir;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:5174")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // admin 后台：/admin/** → admin dist（base=/admin/ 构建产物）
    registry.addResourceHandler("/admin/**")
        .addResourceLocations("file:" + adminDir + "/")
        .resourceChain(true);
    // web 游戏：根路径 → web dist
    registry.addResourceHandler("/**")
        .addResourceLocations("file:" + webDir + "/")
        .resourceChain(true);
  }
}
```

- [ ] **Step 2: 改 application.yml 增加静态目录配置**

在 `api/src/main/resources/application.yml` 的 `app:` 节下追加：

```yaml
  static:
    web-dir: ${WEB_DIR:./dist/web}
    admin-dir: ${ADMIN_DIR:./dist/admin}
```

- [ ] **Step 3: SPA fallback（前端路由刷新 404 兄弟问题）**

新建 `api/src/main/java/com/gmnl/orientation/config/SpaForwardController.java`：

```java
package com.gmnl.orientation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

  @Value("${app.static.web-dir:./dist/web}")
  private String webDir;
  @Value("${app.static.admin-dir:./dist/admin}")
  private String adminDir;

  // admin 未知子路由 → 回 index.html
  @GetMapping("/admin/**")
  public String adminForward() {
    return "forward:/admin/index.html";
  }

  // 根路径 web 未知路由 → 回 index.html
  @GetMapping(value = {"/", "/{path:[^\\.]*}", "/{path:[^\\.]*}/**"})
  public String webForward() {
    return "forward:/index.html";
  }
}
```

> 注意：`/{path:[^\\.]*}` 正则排除带点号（静态资源）的路径，避免拦截 `.js/.css`。

- [ ] **Step 4: 构建前端并验证单进程托管**

```bash
pnpm build:web
pnpm build:admin
# 复制构建产物到 api/dist（Windows bash）
mkdir -p api/dist/web api/dist/admin
cp -r apps/web/dist/. api/dist/web/
cp -r apps/admin/dist/. api/dist/admin/

cd api && mvn -q spring-boot:run &
sleep 20
# web
curl -s http://localhost:8080/ | head -5
# Expected: web index.html
# admin
curl -s http://localhost:8080/admin/ | head -5
# Expected: admin index.html
# api 仍可用
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin12345"}'
# Expected: token JSON
# 停止后台进程
```

- [ ] **Step 5: 提交**

```bash
git add api/src/main/java/com/gmnl/orientation/config/WebConfig.java api/src/main/java/com/gmnl/orientation/config/SpaForwardController.java api/src/main/resources/application.yml
git commit -m "feat(api): serve web and admin build artifacts via static resource mapping"
```

---

### Task 16: 端到端集成验证与交付

**Files:**
- 无新增；验证全链路 + 写运行说明到 README

- [ ] **Step 1: 全量构建**

```bash
pnpm -r build
mkdir -p api/dist/web api/dist/admin
cp -r apps/web/dist/. api/dist/web/
cp -r apps/admin/dist/. api/dist/admin/
cd api && mvn -q package
# Expected: api/target/orientation-api-0.1.0.jar 生成
```

- [ ] **Step 2: 单 jar 启动全栈验证**

```bash
cd api
DB_PASSWORD=orientation_dev_pass JWT_SECRET=dev-secret-change-me-please-make-it-long-enough-32bytes \
  WEB_DIR=./dist/web ADMIN_DIR=./dist/admin \
  java -jar target/orientation-api-0.1.0.jar &
sleep 20
# 浏览器打开 http://localhost:8080/ → 登录页
# 用 admin/admin12345 登录 → 看到机构小岛
# 浏览器打开 http://localhost:8080/admin/ → 管理后台登录
# 停止
```

- [ ] **Step 3: 全后端测试回归**

```bash
cd api && mvn -q test
# Expected: JwtServiceTest(2) + AuthServiceTest(3) + ProgressServiceTest(4) 全部通过
```

- [ ] **Step 4: 更新 README 运行说明**

在 `README.md` 末尾追加"后端服务运行"章节：

```markdown
## 后端服务运行（基础三阶段）

1. 安装 JDK 17、Maven、PostgreSQL 15+、Node 20+、pnpm 9。
2. 建库：`CREATE DATABASE orientation; CREATE USER orientation_app WITH PASSWORD 'orientation_dev_pass'; GRANT ALL PRIVILEGES ON DATABASE orientation TO orientation_app;`
3. 复制 `api/src/main/resources/application-local.yml.example` 为 `application-local.yml`，按需改密码。
4. 启动后端：`cd api && mvn spring-boot:run`（Flyway 自动建表 + 种子数据，含 admin/admin12345）。
5. 前端开发：`pnpm install && pnpm dev:web`（5173）/ `pnpm dev:admin`（5174）。
6. 生产打包：`pnpm -r build` → 复制 `apps/web/dist`、`apps/admin/dist` 到 `api/dist/{web,admin}` → `cd api && mvn package` → `java -jar api/target/orientation-api-0.1.0.jar`。
```

- [ ] **Step 5: 提交**

```bash
git add README.md
git commit -m "docs: add backend run instructions for foundation phases"
```

- [ ] **Step 6: 最终提交标签**

```bash
git tag foundation-phase-1-3
```

---

## 自审（Self-Review）

**1. Spec coverage（对照设计文档 2026-07-01）：**
- 整体架构（异构仓库、shared 包、单进程托管）：Task 1/2/3/15 ✅
- 数据模型 6 张表 + 文档级进度单一真相 + 软删除 + uploads 不进库：Task 4/5 ✅
- 种子数据（7 机构/5 小岛/16 文档占位/admin 账号）：Task 6 ✅（每小岛 1 占位文档，35 个；文档总数与 Demo 16 份在阶段 4 上传时对齐，本阶段占位合理）
- 身份认证（姓名全拼+密码、BCrypt、JWT、改密、防枚举、ADMIN 鉴权）：Task 7/8 ✅
- 内容 API + 鉴权下载 + 不公开静态目录：Task 9 ✅
- 进度 API（幂等 upsert、单调不回退、服务端聚合、小岛状态规则）：Task 10 ✅
- 前端 web（React 迁移、登录、进度 store、节流上报、离开强制上报）：Task 11/12/13 ✅
- 前端 admin（登录、ADMIN 守卫）：Task 14 ✅
- 部署（单 JVM 托管、Flyway、uploads 本地、先 HTTP）：Task 3/4/15/16 ✅
- 测试（进度正确性 + 鉴权 + 文件权限）：Task 7/8/10 ✅（文件下载权限依赖鉴权已在 Task 9 端到端验证 NO_FILE 路径；权限隔离由 SecurityConfig `/api/**` authenticated 保证）

**2. Placeholder scan：** 无 TBD/TODO；DocPage 有"占位正文"说明属阶段 6 范围，已标注；无"add error handling"式空话。

**3. Type consistency：** 共享类型 `User/Institution/Island/Doc/ProgressStatus/IslandStatus/UserRole/ProgressAggregate/UpsertProgressRequest/LoginRequest` 在前后端命名一致；Java 侧 `UserRole.USER/ADMIN`、`ProgressStatus.NOT_STARTED/READING/COMPLETED`、`IslandStatus.LOCKED/UNLOCKED/COMPLETED` 与 TS 枚举字符串值对齐；`CurrentUserResolver.userId()`、`ProgressService.upsert/complete/getAggregate`、`JwtService.generate/parse` 签名跨任务一致。

**范围外（后续计划）：** 阶段 4 文件上传/管理（admin docs CRUD + multipart）、阶段 5 后台管理完善（用户/机构/小岛 CRUD + 统计）、阶段 6 视觉迁移/动画/响应式。本计划结束时系统可登录、可学习、可上报进度、可看聚合状态，为独立可验收交付。


