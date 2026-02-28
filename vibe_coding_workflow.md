# AI 辅助开发工作流指南 (Vibe Coding with Claude Code)

> 本文档面向有一定工程经验的开发者，旨在说明如何在团队中规范化使用 AI 编码工具（以 Claude Code 为例），避免常见陷阱，并将程序员的判断力真正融入 AI 辅助开发流程中。

---

## 目录

1. [背景与项目介绍](#1-背景与项目介绍)
   - 1.1 什么是 Vibe Coding
   - 1.2 本文档适用场景
   - 1.3 案例项目说明（Job Application Tracker）

2. [完整开发流程](#2-完整开发流程)
   - 2.1 调研与分析（Research）
   - 2.2 编程计划（Planning）
   - 2.3 任务拆分与迭代（Iteration）
   - 2.4 AI 编程实现（Implementation）
   - 2.5 人工审查（Human Review）
   - 2.6 自动化测试（Testing）
   - 2.7 重构（Refactor）
   - 2.8 PR Review 与合并
   - 2.9 文档维护（CLAUDE.md）

3. [Vibe Coding 的副作用](#3-vibe-coding-的副作用)
   - 3.1 重复冗余代码
   - 3.2 不遵守项目代码规范
   - 3.3 接口设计随意（乱编 API）
   - 3.4 过度设计与不必要抽象
   - 3.5 测试"作弊"（刷数量不保质量）
   - 3.6 配置不规范、密钥泄露风险
   - 3.7 错误处理不完整
   - 3.8 真实案例：CloudFront 路径匹配 Bug

4. [程序员的职责](#4-程序员的职责)
   - 4.1 理解与拆解需求
   - 4.2 架构与接口设计（人决策，AI 不能替代）
   - 4.3 制定并维护代码规范
   - 4.4 监督 AI 输出
   - 4.5 代码审查
   - 4.6 重构判断
   - 4.7 安全与权限决策

5. [提前设计：让 AI 有规则可循](#5-提前设计让-ai-有规则可循)
   - 5.1 接口设计文档（名称、类型、用法示例）
   - 5.2 代码风格规范（Linting、格式化）
   - 5.3 项目强制要求（例：ORM 必须用 Spring Data JPA，禁止 local imports）
   - 5.4 CLAUDE.md 的结构与写法
   - 5.5 Agent / Command 模板结构

6. [AI 自动化测试](#6-ai-自动化测试)
   - 6.1 AI 测试的常见问题
   - 6.2 如何写好测试 Prompt（核心逻辑、边界条件、mock 策略）
   - 6.3 人工审查测试的 Checklist
   - 6.4 Unit Test vs Integration Test 的边界

7. [AI 自动化代码审查](#7-ai-自动化代码审查)
   - 7.1 提前定义审查规则
   - 7.2 自动化工具与脚本（Linter、Typecheck、Unit Test Gate）
   - 7.3 关键 Pattern / Regex 检查项
   - 7.4 Security Checklist、Maintainability Checklist
   - 7.5 AI Review 的边界（不让它决定架构和业务规则）

8. [进阶功能](#8-进阶功能)
   - 8.1 Skills（内置能力模块）
   - 8.2 Commands（重复工作流自动化）
   - 8.3 Agents（专业身份切换）
   - 8.4 MCP（接入外界工具）
   - 8.5 Plugin 创建与发布

9. [AI 的局限性](#9-ai-的局限性)
   - 9.1 视觉处理能力弱（UI 微调效果差）
   - 9.2 对表格 / Dataframe 具体内容理解有限
   - 9.3 不适合做架构和业务规则决策
   - 9.4 上下文丢失导致的前后不一致

10. [附录](#10-附录)
    - 10.1 CLAUDE.md 模板
    - 10.2 Agent 定义模板
    - 10.3 Command 模板
    - 10.4 Code Review Checklist
    - 10.5 测试审查 Checklist

---

## 1. 背景与项目介绍

### 1.1 什么是 Vibe Coding

Vibe Coding 是指开发者以自然语言描述意图，由 AI（如 Claude Code、GitHub Copilot）直接生成代码，开发者不逐行编写，而是以审查、引导、验收为主要工作方式的开发模式。

这种模式的核心价值在于**加速产出**：重复性代码、样板代码、初版实现都可以交给 AI 完成，开发者把时间留在更高价值的判断上。

但"Vibe"这个词也暗含风险——如果缺乏规范，AI 会凭"感觉"写代码：能跑，但不一定对；能交付，但不一定能维护。本文档的目标就是把这个"感觉"换成**可控的工程流程**。

### 1.2 本文档适用场景

本文档适合以下情况参考：

- 团队希望引入 AI 编码工具，但担心代码质量和规范一致性
- 个人使用 Claude Code / Copilot 开发项目，想建立一套可重复的工作流
- 技术 Lead 或架构师需要制定 AI 辅助开发的团队规范
- 对 Vibe Coding 感兴趣，但不清楚"人"在其中应该做什么

本文档**不适合**：完全不写代码、只想让 AI 全自动交付的场景。AI 无法替代程序员在需求理解、架构决策、安全审查上的判断。

### 1.3 案例项目说明（Job Application Tracker）

本文档中的所有例子均来自一个真实的 Vibe Coding 项目：**Job Application Tracker（求职申请追踪器）**，以下简称 JAT。

**项目功能：**
- 追踪求职申请进度（投递、面试、Offer 等状态）
- 管理针对不同岗位的简历定制内容与备注
- 异步截止日期提醒（如面试截止、作业提交等）

**技术栈：**

| 层次 | 技术 |
|---|---|
| 前端 | React 18 + TypeScript、Vite、TanStack Query、React Hook Form + Zod、Tailwind CSS |
| 后端 | Java 17、Spring Boot 3.x、Spring Cloud Gateway |
| 消息队列 | RabbitMQ（Amazon MQ），事件驱动异步通知 |
| 数据库 | MySQL 8.0（AWS RDS） |
| 缓存 | Redis（AWS ElastiCache） |
| 部署 | AWS ECS Fargate + ALB + CloudFront + S3 |
| IaC | Terraform |
| CI/CD | GitHub Actions |

**服务拆分：**

```
browser
  └── CloudFront
        ├── /（默认）→ S3（React SPA 静态资源）
        ├── /auth/*        ─┐
        ├── /applications* ─┤→ ALB → gateway-service（ECS）
        └── /notifications* ─┘         ├── auth-service
                                        ├── application-service
                                        └── notification-service
```

这个项目从 0 到部署上线，**全程使用 Claude Code 辅助开发**。本文档记录了这个过程中踩过的坑、总结出的规范，以及让 AI 真正"好用"的方法。

---

## 2. 完整开发流程

整体流程如下，每一步都有明确的人机分工：

```
调研/分析 → 计划 → 任务拆分 → 编写 CLAUDE.md/agents/commands
    → AI 实现 → 人工审查 → 测试 → 重构 → PR Review → CLAUDE.md 维护
```

### 2.1 调研与分析（Research）

**AI 做什么：** 收集信息、整理方案、横向对比各技术方案的优劣。
**人做什么：** 核实信息、结合项目实际做最终决策。

**JAT 项目案例：**

在 JAT 项目初期，用 AI 辅助做了以下技术选型对比：

| 决策点 | 选择 | 主要理由（AI 辅助分析） |
|---|---|---|
| 消息队列 | RabbitMQ（Amazon MQ）而非 Kafka | JAT 的通知量极低（用户操作触发），Kafka 的分布式日志模型对此场景过重；RabbitMQ 的 per-message 重试和 DLQ 机制更直接；Amazon MQ 托管免运维 |
| API Gateway | Spring Cloud Gateway 而非 Kong / Nginx | 与 Spring Boot 生态深度集成，CORS 和路由配置以代码形式管理，无需额外学习 Lua 脚本；项目团队 Java 背景，维护成本低 |
| 数据库 | MySQL（RDS）而非 PostgreSQL | Spring Data JPA 对两者支持相当；AWS RDS MySQL 多租户经验更成熟；无需 JSONB 等 PG 特性 |
| 缓存 | Redis（ElastiCache）仅用于 application-service | Dashboard 数据聚合查询频繁但变更不频繁，Redis simple key-value 够用；notification-service 无需缓存 |

> **注意**：AI 给出的对比结论要人来核实。例如 AI 曾建议"可以用 Kafka 的 consumer group 实现重试"，但这个建议忽略了项目部署在 Amazon MQ 而非自建集群的前提，被否掉。

### 2.2 编程计划（Planning）

**AI 做什么：** 根据需求草稿列出功能点、拆分 feature、识别风险点。
**人做什么：** 决定优先级和边界、制定验收标准、砍掉超出范围的功能。

**JAT 项目案例——被砍掉的功能：**

最初 AI 建议的 feature list 包含以下内容，最终全部砍掉或推迟：

| 功能 | 砍掉原因 |
|---|---|
| 简历 PDF 解析与自动填充 | 需要 NLP/OCR，依赖复杂，与核心功能无关 |
| Google Calendar 集成（截止日同步） | 需要 OAuth 授权流，范围蔓延，核心价值不高 |
| 邮件通知（SMTP） | RabbitMQ 异步通知已够验证消息队列，email 是重复工作 |
| 招聘网站自动抓取 | 法律合规风险，且爬虫维护成本高 |
| 面试准备 AI 建议（LLM 调用） | 引入外部 API 依赖，超出本项目架构边界 |

**验收标准（最终确定）：**

```
1. 用户可以注册/登录/登出
2. 可以创建、查看、编辑、删除求职申请
3. 可以为每条申请添加简历定制笔记
4. 创建含截止日期的申请后，notification-service 能收到 RabbitMQ 事件并生成通知记录
5. Dashboard 展示聚合数据，Redis 缓存有效
6. 整个链路在 AWS 上可访问（CloudFront URL 能正常使用）
```

### 2.3 任务拆分与迭代（Iteration）

**原则：** 按依赖顺序拆分，大任务拆小，每轮迭代都要能独立验证（实现 → 验证 → review）。

**JAT 项目的实现顺序：**

```
auth-service → application-service → notification-service → gateway-service → frontend
```

选择这个顺序的原因：
- `auth-service` 是其他一切的前置（JWT 发行、用户体系）
- `application-service` 是核心业务，依赖 auth
- `notification-service` 消费 application-service 发布的 RabbitMQ 事件，依赖双方提前约定好的 `DeadlineEventPayload` schema（而非 application-service 进程本身，开发上两者可并行）
- `gateway-service` 路由所有服务，最后配置
- `frontend` 调用 gateway，因此放最后

**每个 service 的迭代拆法示例（以 application-service 为例）：**

```
Iteration 1：基础 CRUD（创建/查询/更新/删除申请）+ Flyway schema
Iteration 2：Redis 缓存 dashboard 聚合数据
Iteration 3：RabbitMQ 发布截止日期事件
Iteration 4：targeting notes（简历定制笔记）功能
```

每轮迭代结束后，人工验证本轮新增功能，再进入下一轮，**不在同一个 PR 里混入多个 iteration 的内容**。

### 2.4 AI 编程实现（Implementation）

**关键前提：** AI 开始写代码之前，以下三件事必须已经准备好：
1. 该模块的 `CLAUDE.md`（架构说明、规范、禁忌）
2. 针对该任务的 agent 或 command（限定 AI 的角色和输出范围）
3. 明确的验收条件（写在 prompt 里）

**JAT 项目使用的 agent 示例：**

| Agent | 负责范围 |
|---|---|
| `backend-spring` | Spring Boot controller、service、repository、DTO、JWT |
| `aws-deploy` | ECS task definition、Terraform、CloudFront 配置 |
| `frontend-react` | React 页面、React Query hooks、表单、路由 |
| `system-architect` | 新 feature 的跨服务设计、API contract |

**JAT 项目使用的 command 示例：**

| Command | 用途 |
|---|---|
| `/scaffold-service` | 新建 Spring Boot service 骨架（含 Dockerfile、Flyway、基础配置） |
| `/implement-endpoint` | 端到端实现一个 REST 接口 |
| `/add-rabbitmq-event` | 添加 producer + consumer + DLQ 一套配置 |
| `/review-pr` | PR 合并前代码审查 |

**好的 prompt 结构（以实现一个接口为例）：**

```
背景：application-service，已有 Application 实体和 Repository
任务：实现 PATCH /applications/{id}/status，更新申请状态
约束：
  - 只能修改 status 字段，不能改其他字段
  - 写完后要清除 Redis dashboard 缓存（key: app:dashboard:{userId}）
  - 用 DTO 接收请求，不暴露 JPA entity
  - 加 @Transactional
不需要：不要改 Flyway schema，status 字段已存在
验收：返回更新后的 ApplicationResponse，HTTP 200
```

### 2.5 人工审查（Human Review）

AI 实现后，人工审查的重点：

1. **大致逻辑是否正确**（不是逐行读，而是理解主干流程）
2. **代码是否过度复杂**（有没有为了"优雅"引入不必要的抽象）
3. **有没有遗漏**（边界条件、错误处理、权限校验）
4. **有没有超出任务范围**（AI 是否"顺手"改了不该改的地方）

**JAT 项目反面案例——不 review 的后果：**

登录后跳转 dashboard，页面一闪然后完全空白。全程 axios 返回 200，控制台没有报错。

排查了很久才发现根因：CloudFront 路径配置写错了一个字符：

```
# 错误写法（AI 自己发挥）
path_pattern = "/notifications/*"   # 只匹配子路径，/notifications 本身不匹配

# 正确写法（规范要求）
path_pattern = "/notifications*"    # 同时匹配 /notifications 接口本身
```

`GET /notifications` 请求没有被路由到 ALB，被 CloudFront 当作静态资源请求，返回了 S3 的 HTML，axios 收到 200 但内容不是 JSON，导致前端渲染崩溃。

**根本原因：** 当时 `aws-deploy.md` 这个 agent 里没有写 CloudFront 路径规范，AI 没有约束，自由发挥写了错误的 terraform 配置。后来把规范补进了 CLAUDE.md。

> **教训：** review 不是可选项。AI 写的代码"能跑"不等于"正确"。越是基础设施配置这类不报错但行为错误的代码，越需要人来检查。

### 2.6 自动化测试（Testing）

**AI 负责：** 根据 prompt 生成测试骨架、常见 case、mock 配置。
**人负责：** 审查测试是否真正覆盖风险点，而不是刷数量。

**JAT 项目的测试分层：**

| 类型 | 工具 | 覆盖范围 | 真实文件（所属服务） |
|---|---|---|---|
| Unit Test | JUnit 5 + Mockito（`@ExtendWith(MockitoExtension.class)`） | Service 层业务逻辑、边界条件、事件发布行为 | `ApplicationServiceTest.java`（application-service）、`DeadlineEventConsumerTest.java`（notification-service）、`NotificationServiceTest.java`（notification-service） |
| Web Layer Test | `@WebMvcTest` + MockMvc + `@MockBean` | Controller HTTP 路由、状态码、JSON 格式；Service 层 mock 掉，不启动真实 DB | `AuthControllerTest.java`（auth-service）、`ApplicationControllerTest.java`（application-service）、`NotificationControllerTest.java`（notification-service） |
| 前端 | 手动验证为主 | 关键流程：登录 → 创建申请 → 查看 Dashboard | — |

**好的测试长什么样 — 来自 `ApplicationServiceTest.java` 的真实案例：**

同一个方法 `create()`，AI 写出了两个互补的 case，分别验证 deadline 存在和不存在两种路径：

```java
// services/application-service/src/test/java/com/jobtracker/application/service/ApplicationServiceTest.java

@Test
void create_withDeadline_savesAndPublishesEvent() {
    // deadline 不为 null → 必须触发 RabbitMQ 事件
    LocalDate deadline = LocalDate.of(2026, 3, 1);
    // ... 构造请求，mock repository.save()
    applicationService.create(USER_ID, req);

    ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
    verify(deadlineEventPublisher).publishDeadlineEvent(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(APP_ID);
}

@Test
void create_withoutDeadline_doesNotPublishEvent() {
    // deadline 为 null → 不能发事件
    // ... 构造无 deadline 的请求
    applicationService.create(USER_ID, req);

    verify(deadlineEventPublisher, never()).publishDeadlineEvent(any());
}
```

以及权限边界：

```java
@Test
void getById_differentOwner_throwsForbiddenException() {
    // 申请属于另一个 userId → 必须抛 403，不能返回数据
    JobApplication app = buildApplication(APP_ID, OTHER_USER_ID, null);
    when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

    assertThatThrownBy(() -> applicationService.getById(USER_ID, APP_ID))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("You do not own this application");
}
```

**这些 case 覆盖的是真正的风险点**（业务分支、权限边界），而不是在重复测试框架本身的行为。写测试 prompt 时要明确指出这些点，而不是让 AI 自由发挥测"所有方法"。

### 2.7 重构（Refactor）

**原则：** 一次只改一个问题，行为不变需要证明（测试通过），人来最终 approve。

**JAT 项目案例：**

文件：`services/application-service/src/main/java/com/jobtracker/application/service/ApplicationService.java`

AI 最初写出的版本里，`getById`、`update`、`delete` 三个方法各自内联了一套相同的"查记录 + 校验归属"逻辑：

```java
// 重构前
public ApplicationResponse getById(Long userId, Long id) {
    JobApplication app = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    if (!app.getUserId().equals(userId)) {
        throw new ForbiddenException("You do not own this application");
    }
    return toResponse(app);
}

@Transactional
@CacheEvict(cacheNames = CacheNames.DASHBOARD_SUMMARY, key = "#userId")
public ApplicationResponse update(Long userId, Long id, UpdateApplicationRequest req) {
    JobApplication app = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    if (!app.getUserId().equals(userId)) {
        throw new ForbiddenException("You do not own this application");
    }
    // ... 字段更新逻辑
    return toResponse(applicationRepository.save(app));
}

@Transactional
@CacheEvict(cacheNames = CacheNames.DASHBOARD_SUMMARY, key = "#userId")
public void delete(Long userId, Long id) {
    JobApplication app = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    if (!app.getUserId().equals(userId)) {
        throw new ForbiddenException("You do not own this application");
    }
    applicationRepository.delete(app);
}
```

问题：同一个"查找 + 权限校验"的四行代码出现了三次。如果异常消息要改、或者权限校验逻辑要扩展（例如加管理员 bypass），就必须同步修改三个地方，漏改一处就会行为可能不一致。

重构目标：**只消除重复，不改任何业务逻辑**。

使用 `/refactor` command，Prompt：
```
文件：ApplicationService.java
目标：remove duplicate — getById、update、delete 三个方法里有相同的
      "findById + 权限校验"代码块，提取为私有方法 loadAndVerifyOwnership(Long userId, Long id)
约束：
  - 不改三个方法的对外签名和注解
  - 不改任何权限校验逻辑，只是搬移代码
  - 不要"顺便"修改其他地方
验收：ApplicationServiceTest 全部通过
```

重构后：

```java
// 重构后（当前文件实际状态）
public ApplicationResponse getById(Long userId, Long id) {
    return toResponse(loadAndVerifyOwnership(userId, id));
}

@Transactional
@CacheEvict(cacheNames = CacheNames.DASHBOARD_SUMMARY, key = "#userId")
public ApplicationResponse update(Long userId, Long id, UpdateApplicationRequest req) {
    JobApplication app = loadAndVerifyOwnership(userId, id);
    // ... 字段更新逻辑
    return toResponse(applicationRepository.save(app));
}

@Transactional
@CacheEvict(cacheNames = CacheNames.DASHBOARD_SUMMARY, key = "#userId")
public void delete(Long userId, Long id) {
    applicationRepository.delete(loadAndVerifyOwnership(userId, id));
}

private JobApplication loadAndVerifyOwnership(Long userId, Long id) {
    JobApplication app = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    if (!app.getUserId().equals(userId)) {
        throw new ForbiddenException("You do not own this application");
    }
    return app;
}
```

`ApplicationServiceTest` 原有 case 全部通过（行为未变）→ 可以 merge。测试里专门覆盖了这段逻辑的两个分支：

```java
// getById_notFound_throwsResourceNotFoundException — 验证 404 路径
// getById_differentOwner_throwsForbiddenException  — 验证 403 路径
```

这两个 cases 在重构前后都能通过，证明提取没有改变行为。

> **关键点**：prompt 里写 `remove duplicate` 而不是"优化一下"——明确告诉 AI 目标是消除重复，AI 就不会顺手增加功能或改变接口。验收条件永远是"测试通过"，不是"看起来更好"。

### 2.8 PR Review 与合并

每个 PR 合并前，使用 `/review-pr` command 让 AI 做一遍初步审查，再由人最终决定。

**AI review 负责：** 逻辑错误、重复代码、命名不一致、明显的 bug、测试覆盖是否充分。
**人 review 负责：** 架构合不合理、业务逻辑对不对、安全和权限是否正确、是否符合项目规范。

> AI 做的 review 是过滤器，不是最终裁判。

### 2.9 文档维护（CLAUDE.md）

CLAUDE.md 是 AI 的"永久记忆"——每次对话都会读取，用来约束 AI 的行为范围。

**JAT 项目的 CLAUDE.md 层级结构：**

```
/CLAUDE.md                          ← 全局：技术栈、架构规则、服务边界
/frontend/CLAUDE.md                 ← React 规范：auth flow、React Query hooks、Axios 配置
/services/auth-service/CLAUDE.md    ← JWT 签发、BCrypt、FilterChain、Flyway schema
/services/application-service/CLAUDE.md  ← CRUD、Redis 缓存、RabbitMQ 发布
/services/notification-service/CLAUDE.md ← RabbitMQ 消费、DLQ 策略
/services/gateway-service/CLAUDE.md ← 路由、CORS、ECS Service Connect
/infra/CLAUDE.md                    ← Docker Compose、环境变量
/infra/terraform/CLAUDE.md          ← AWS 资源：VPC、ECS、RDS、CloudFront 路径规范
```

**什么时候更新 CLAUDE.md：**
- 踩了一个 AI 容易犯的坑之后（例如 CloudFront 路径规范就是踩坑后加进去的）
- 引入了新的技术约束（例如新增了某个必须遵守的包或禁用的写法）
- 一个 pattern 被 AI 反复写错

> CLAUDE.md 不是一次性文档，而是随项目演进持续更新的"活文档"。

---

## 3. Vibe Coding 的副作用

AI 辅助编码的核心价值是加速产出，但"加速"本身也会放大某些固有问题。本节梳理最常见的七类副作用，以及如何识别和防御。

### 3.1 重复冗余代码

AI 在生成代码时缺乏对整个代码库的全局视野。每一次 prompt 对它来说是一个相对独立的上下文窗口，因此它很容易在不同文件或不同函数中写出语义相同、结构几乎一致的代码，而不知道已有可复用的实现。

**JAT 项目案例**：`ApplicationService` 的 `getById`、`update`、`delete` 三个方法，AI 最初在每个方法里各自内联了一套相同的"查记录 + 校验归属"逻辑（详见 2.7 节重构案例）。功能正确，但同一块四行代码出现了三次。一旦权限校验逻辑需要扩展，必须同步修改三处，漏改任意一处就会造成行为不一致。最终通过 refactor 提取成 `loadAndVerifyOwnership()` 私有方法消除了重复。

重复代码的危害不只是"代码量变多"——更重要的是，它把一个本应只在一处做的决策分散到了多处，使每次修改都需要保持多处同步。

**防御方法**：在 CLAUDE.md 里维护已有工具/配置清单，并在 prompt 里明确要求："如果发现已有类似实现，优先复用，不要新建"。

### 3.2 不遵守项目代码规范

AI 的训练数据来自互联网上各种风格的代码，在没有约束的情况下，它会自动套用它认为"合理"的通用写法，而不是项目规定的写法。这是最容易被忽视、也最容易累积技术债的一类副作用。

**典型反例 —— TypeScript 中滥用 `any`**：前端规范明确禁止 `any`，所有 props 和 API response 必须有明确类型定义。但 AI 在处理复杂嵌套的 API 响应时，往往直接写 `const data: any = response.data`——类型安全从此形同虚设，TypeScript 编译器的保护完全失效，下游所有属性访问都变成运行时风险，且 IDE 无法提供任何补全或错误提示。

**防御方法**：在 CLAUDE.md 里把规范写成**明确的禁止项**（如"不得使用 `any`"），而不是只写"应该怎么做"。AI 对明确的 constraint 比对隐含的 convention 服从得更好。

### 3.3 接口设计随意（乱编 API）

AI 在实现一个功能时，如果接口文档或 DTO 定义不完整，它会根据"合理猜测"自行发明字段名、路径和请求格式。这些发明往往逻辑上说得通，但与前后端其他地方的约定不一致，只在集成时才暴露出不匹配。

**场景 1**：假设让 AI 给通知服务实现"标记通知已读"接口，但 prompt 里没有指定路径和字段名。AI 可能生成 `PATCH /notifications/{id}/read`，body 为 `{ "isRead": true }`；但前端按另一套理解实现了 `PUT /notifications/{id}/markRead`，body 为 `{ "read": true }`。两套约定哪个都不算错，但只要有一方先上线，另一方就必须返工。

**场景 2**：分页参数约定冲突。JAT 后端使用 Spring Data 的 Pageable 约定（`page=0&size=10`，0-indexed）。但 AI 有时自行使用 `pageNum=1&pageSize=10`（1-indexed），前端调用时语义不一致，第一页数据可能被跳过，且不报任何错误。

**防御方法**：在进入实现 iteration 之前，先通过 `system-architect` agent 确定 API contract（路径、HTTP 方法、request/response 字段名），写入 CLAUDE.md 或 prompt，并明确要求"不得自行发明字段名或路径"。

### 3.4 过度设计与不必要抽象

AI 有时会把一个简单的问题设计得过于复杂，引入当前并不需要的抽象层和扩展点。这源于它的训练数据里充斥着大量"为可扩展性而设计"的示例代码——它会模仿这种风格，即使当前规模根本不需要。

结果是：代码能跑，但改一个功能需要同时理解三四个类之间的关系，维护成本反而高于直接写的简单版本。

**防御方法**：在 prompt 里明确 **YAGNI（You Aren't Gonna Need It）原则**："只实现当前需求，不为假设的未来扩展预留接口"。review 时重点检查有没有只用了一次的抽象、没有实现类的接口、或者为了"可扩展"引入的空 base class。

### 3.5 测试"作弊"（刷数量不保质量）

AI 写的测试在 CI 里全部通过，覆盖率报告也很好看，但很多测试并没有真正在验证业务逻辑——它们在用各种方式"作弊"。这是最难从表面上发现的副作用。

**常见作弊模式**：

- **只验证 mock 本身**：mock 了 repository 的 `findById`，然后 assert service 返回了 mock 返回的值。这只证明了 mock 配置正确，对 service 内部逻辑没有任何验证。
- **assertion 永远为 true**：`assertNotNull(result)` 这类断言，只要代码不抛出异常就能通过，完全没有验证返回内容是否正确。
- **只测 happy path，不测关键分支**：测试了"申请存在时返回申请"，但没有测试"申请不存在时抛出 `ResourceNotFoundException`"或"归属人不匹配时抛出 `ForbiddenException`"——而这些边界路径往往才是真正容易出 bug 的地方。

对比来看，JAT 项目中真正有价值的测试案例（见 2.6 节）明确验证了：`deadline` 为 null 时不发 RabbitMQ 事件、不同用户访问他人申请时抛出 `ForbiddenException`——这些都是业务上有实际意义的分支。

**防御方法**：测试 prompt 里明确列出"核心业务逻辑是什么"、"哪些是边界条件"、"哪些分支容易出 bug"，以及"哪些 case 不需要写"。不要笼统说"帮我写完所有测试"——AI 会用数量填满覆盖率，而不是用质量覆盖风险。人工审查测试的 checklist 见第 6 节。

### 3.6 配置不规范、密钥泄露风险

**配置不规范**：AI 生成配置时，倾向于先让程序能跑起来，因此容易把值直接写死，而不是通过环境变量注入。这导致同一份代码无法在不同环境之间灵活切换，也无法在不改代码的情况下调整行为。JAT 规范要求：所有外部 host/port/credential 配置必须是环境变量驱动，production profile 里不得有指向 localhost 的硬编码默认值。

**密钥泄露风险**：这是后果最严重的一类。AI 的训练数据里充斥着教程和示例代码，这类代码经常直接把凭证写在配置文件里。如果 prompt 里没有明确"凭证必须从环境变量读取"，AI 很可能照着这个习惯生成配置。一旦 commit 进 git，密码就进入 history，之后即使删除文件，history 里仍然存在，所有相关凭证都必须 rotate。

**防御方法**：在 CLAUDE.md 里明确规定"secrets 只能通过 AWS Secrets Manager 注入，`.env.example` 里只能有占位符，绝不提交真实值"；AI 每次生成配置文件后人工检查是否含有硬编码凭证。

### 3.7 错误处理不完整

AI 实现 happy path 很顺手，但错误处理往往是敷衍的：要么只捕获最宽泛的 `Exception`，要么在 catch 块里只打一行 log 就把异常吞掉，要么返回的 HTTP status code 不准确（比如业务校验失败返回 500 而不是 422）。

JAT 项目通过 `@RestControllerAdvice` 全局统一处理异常，规定了固定的错误响应格式：`{ status, error, message, timestamp, path }`。如果 AI 在某个 controller 里绕过这个全局处理器、自己 try-catch 并返回不一样的格式，前端的错误处理代码就必须应对两套格式，且这种不一致性很难被测试捕获——只有在错误路径被真正触发时才会暴露。

**防御方法**：在 prompt 里明确"所有异常必须通过全局 `@RestControllerAdvice` 处理，不得在 controller 层自行 try-catch 并返回自定义格式"。code review checklist 里专设一栏检查错误路径的处理方式。

### 3.8 真实案例：CloudFront 路径匹配 Bug

以上各类副作用中，"配置不规范"叠加"agent 上下文缺失"的实际破坏力，本项目有一个完整的真实案例（详见 2.5 节）。

简要回顾：`aws-deploy.md` 这个 agent 里遗漏了 CloudFront path pattern 规范，AI 自由发挥，把 `/notifications*` 写成了 `/notifications/*`。这一个字符的差异导致 `GET /notifications` 请求无法被路由到 ALB，前端 dashboard 完全白屏，而全程 axios 返回 200，没有任何报错信号。

这个案例综合体现了本节讨论的几类副作用：**配置不规范**（path pattern 语义错误）+ **agent 上下文缺失时 AI 自由发挥**（没有 CloudFront 规范的约束）+ **错误无声无息**（HTTP 200 掩盖了真正的路由错误）。

事后处理：最直接的修复是把 CloudFront path pattern 规范补进了 `aws-deploy.md`（这个 agent 本来就是负责 AWS 基础设施的，规范缺失在这里），同时也同步到了根 `CLAUDE.md` 和 `infra/terraform/CLAUDE.md`；前端补充了 `Array.isArray()` 防御，避免非预期响应格式引发白屏。**踩坑之后立刻更新相关 agent 文件和 CLAUDE.md，是防止同类错误再次发生的唯一可靠方式。**

---

## 4. 程序员的职责

### 4.1 理解与拆解需求

### 4.2 架构与接口设计（人决策，AI 不能替代）

### 4.3 制定并维护代码规范

### 4.4 监督 AI 输出

### 4.5 代码审查

### 4.6 重构判断

### 4.7 安全与权限决策

---

## 5. 提前设计：让 AI 有规则可循

### 5.1 接口设计文档（名称、类型、用法示例）

### 5.2 代码风格规范（Linting、格式化）

### 5.3 项目强制要求（例：ORM 必须用 Spring Data JPA，禁止 local imports）

### 5.4 CLAUDE.md 的结构与写法

### 5.5 Agent / Command 模板结构

---

## 6. AI 自动化测试

### 6.1 AI 测试的常见问题

### 6.2 如何写好测试 Prompt（核心逻辑、边界条件、mock 策略）

### 6.3 人工审查测试的 Checklist

### 6.4 Unit Test vs Integration Test 的边界

---

## 7. AI 自动化代码审查

### 7.1 提前定义审查规则

### 7.2 自动化工具与脚本（Linter、Typecheck、Unit Test Gate）

### 7.3 关键 Pattern / Regex 检查项

### 7.4 Security Checklist、Maintainability Checklist

### 7.5 AI Review 的边界（不让它决定架构和业务规则）

---

## 8. 进阶功能

### 8.1 Skills（内置能力模块）

### 8.2 Commands（重复工作流自动化）

### 8.3 Agents（专业身份切换）

### 8.4 MCP（接入外界工具）

### 8.5 Plugin 创建与发布

---

## 9. AI 的局限性

### 9.1 视觉处理能力弱（UI 微调效果差）

### 9.2 对表格 / Dataframe 具体内容理解有限

### 9.3 不适合做架构和业务规则决策

### 9.4 上下文丢失导致的前后不一致

---

## 10. 附录

### 10.1 CLAUDE.md 模板

### 10.2 Agent 定义模板

### 10.3 Command 模板

### 10.4 Code Review Checklist

### 10.5 测试审查 Checklist
