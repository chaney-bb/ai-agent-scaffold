# ai-agent-scaffold

基于 **DDD + Spring Boot + Google ADK** 的 Java AI Agent 脚手架，用 YAML 声明式装配智能体，并对外提供会话 HTTP 接口，便于二次开发与课程跟学落地。

> groupId：`cn.chaney.ai` · Java 17 · Spring Boot 3.4.x

---

## 能做什么

- **声明式装配**：在 `agent/*.yml` 中配置模型、工具、工作流与 Runner，启动时按责任链节点装配进 Spring 容器
- **多 Agent 编排**：支持单体 Agent，以及 Loop / Parallel / Sequential 等工作流组合
- **工具扩展**：MCP（SSE / Stdio / Local）、Skills、Plugin 回调可插拔
- **会话服务**：按 `agentId` 建会话、同步对话、流式对话，可对接简易前端
- **工程骨架**：标准 DDD 分层，开箱可跑、可改、可扩展

---

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 / 框架 | Java 17、Spring Boot 3.4.3 |
| Agent 运行时 | Google ADK 1.1.0、`google-adk-spring-ai` |
| 模型接入 | Spring AI BOM（OpenAI 兼容接口，如阿里云百炼等） |
| 设计模式 | `xfg-wrench-starter-design-framework`（策略路由装配链） |
| 基础设施 | MySQL（HikariCP，可按需启用）、Docker Compose 样例 |

---

## 模块结构

```
ai-agent-scaffold
├── ai-agent-scaffold-api            # 对外接口契约、DTO、统一响应
├── ai-agent-scaffold-app            # 启动模块、application / agent YAML
├── ai-agent-scaffold-domain         # 装配链、ChatService、Agent 领域逻辑
├── ai-agent-scaffold-trigger        # HTTP 入口（AgentServiceController）
├── ai-agent-scaffold-infrastructure # 仓储 / 外部适配（按需扩展）
├── ai-agent-scaffold-types          # 枚举、异常、通用类型
└── docs
    ├── studyDocs/                   # 跟学笔记
    └── dev-ops/                     # Docker、Nginx、启停脚本
```

### 装配主链路（domain）

```
RootNode → AiApiNode → ChatModelNode → AgentNode
        → AgentWorkflowNode（Loop / Parallel / Sequential）
        → RunnerNode（注册 AiAgentRegisterVO）
```

配置表键与 `agentId` 决定容器中的智能体实例；会话层通过 `agentId` 取 Runner 对话。

---

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
-（可选）Docker：本地 MySQL / Nginx，见 `docs/dev-ops/`

### 2. 配置智能体

编辑 `ai-agent-scaffold-app/src/main/resources/agent/` 下 YAML，例如 `only-one-agent.yml`：

- `module.ai-api`：兼容 OpenAI 的 `base-url` / `api-key`
- `module.chat-model`：模型名、MCP、Skills
- `module.agents` / `runner`：指令与入口 Agent

在 `application-dev.yml` 中通过 `spring.config.import` 引入对应配置文件：

```yaml
spring:
  config:
    import:
      - classpath:agent/only-one-agent.yml
```

**密钥说明**：`api-key`、MCP `sse-endpoint` 等请只填在本地；仓库提交请使用占位符，勿把真实密钥推到远程。

### 3. 启动

```bash
# 在项目根目录
mvn clean install -DskipTests
cd ai-agent-scaffold-app
mvn spring-boot:run
```

默认端口：`8091`（见 `application-dev.yml`）。

启动类：`cn.chaney.ai.Application`。

### 4. 验证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/query_ai_agent_config_list` | 查询已装配智能体列表 |
| POST | `/api/v1/create_session` | 创建会话（body：`agentId`、`userId`） |
| POST | `/api/v1/chat` | 同步对话 |
| POST | `/api/v1/chat_stream` | 流式对话 |

简易前端样例：`docs/dev-ops/nginx/html/`。

---

## 内置 Agent 配置样例

| 文件 | 用途 |
|------|------|
| `agent/only-one-agent.yml` | 单体智能体（默认引入） |
| `agent/test-agent.yml` | 跟学 / 联调样例 |
| `agent/parallel_research_app.yml` | 并行工作流样例 |

按需在 `application-*.yml` 中切换 `spring.config.import` 即可。

---

## 运维与文档

- Docker 环境：`docs/dev-ops/docker-compose-environment.yml`、`docker-compose-app.yml`
- 启停脚本：`docs/dev-ops/app/start.sh`、`stop.sh`
- 跟学笔记：`docs/studyDocs/`（装配节点、MCP、Skills、会话服务、前端对接等）

---

## 适用场景

- 把「模型 + 工具 + 工作流」沉淀成可复用的企业侧 Agent 工程模板
- 在 DDD 分层下扩展业务域、HTTP API、持久化与部署方式
- 对照课程进度做增量能力落地（装配 → 增强 → 会话 → UI）

---

## License

Apache License 2.0
