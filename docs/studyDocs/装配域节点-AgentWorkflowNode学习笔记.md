# 装配域节点 AgentWorkflowNode 学习笔记

> 对应课程：第 2-8 节 · 装配域节点-AgentWorkflowNode  
> 工程提交：`feat: 第2-8节装配域节点 AgentWorkflowNode`  
> 对照学习项目分支：`2-8-armory-node-agent-work-flow`

---

> 学习笔记写法约定见工作台：`D:\javaproject\docs\学习笔记撰写约定.md`（结构：流程理解 + UML → 细聊 / 踩坑 / 拓展）

---

## 一、流程理解（总览）

前几节已把 `OpenAiApi` → `ChatModel` → 多个 `LlmAgent` 装进上下文。本节进入 **智能体工作流编排的流转骨架**：按 YAML `agent-workflows[].type` 决定进 Loop / Parallel / Sequential 哪条装配支路。  
**本节只做节点怎么跳，不做三种 Agent 的真正业务组装**（`doApply` 多为占位）。

1. **前置产物已在上下文**  
   `AgentNode` 已把配置里的多个 `LlmAgent` 按 `name` 写入 `DynamicContext.agentGroup`。工作流稍后会用 `sub-agents` 名字去取这些子 Agent（后续课时）。

2. **AgentWorkflowNode：校验配置并写入上下文**  
   入口：`AgentWorkflowNode.doApply`。从 `module.agentWorkflows` 取列表；为空则抛 `agentWorkflows is null`；否则 `dynamicContext.setAgentWorkflows(...)`，再 `router`。

3. **按首项 type 分发到三种子节点之一**  
   `AgentWorkflowNode.get()`：读列表第 0 项的 `type` → `AgentTypeEnum.formType` → 得到 Bean 名（如 `sequentialAgentNode`）→ `switch` 返回已注入的 `LoopAgentNode` / `ParallelAgentNode` / `SequentialAgentNode`。

4. **Loop / Parallel：只写好转发规则（骨架）**  
   - `doApply` 本节返回 `null`（尚未组装 ADK 的 `LoopAgent` / `ParallelAgent`）。  
   - `get()` 再看列表首项 type：Loop 可去 Parallel / Sequential；Parallel 可去 Loop / Sequential；用 `getBean` 取对方，避免循环依赖。

5. **Sequential：本节兜底终点**  
   `SequentialAgentNode.get()` 返回 `defaultStrategyHandler`，不参与 Loop↔Parallel 互跳。后续再在此组装 `SequentialAgent`。

6. **与整条装配链的关系（重要）**  
   学习项目 2-8 与我的项目一致：`AgentNode.get()` **暂未**接到 `AgentWorkflowNode`。节点类已齐，但从 `RootNode` 跑完整链时，目前仍在 `AgentNode` 处结束。要整链验证需后续接线。

一句话：**`AgentTypeEnum` 把 YAML 的 type 翻译成节点 Bean 名；`AgentWorkflowNode` 负责入口分发；Loop/Parallel/Sequential 先把「能怎么跳」写清楚，业务组装留给后面课时。**

### 时序图（UML）

```mermaid
sequenceDiagram
    autonumber
    participant AW as AgentWorkflowNode
    participant Ctx as DynamicContext
    participant Enum as AgentTypeEnum
    participant Loop as LoopAgentNode
    participant Para as ParallelAgentNode
    participant Seq as SequentialAgentNode

    Note over AW: 假定已从 AgentNode 接到本节点（2-8 官方暂未接线）
    AW->>AW: doApply 读 module.agentWorkflows
    AW->>Ctx: setAgentWorkflows(list)
    AW->>AW: router → get()
    AW->>Ctx: getAgentWorkflows().get(0).type
    AW->>Enum: formType(type)
    Enum-->>AW: node 名（如 sequentialAgentNode）

    alt type=loop
        AW->>Loop: 注入的 loopAgentNode
        Loop->>Loop: doApply 占位 return null
        Note over Loop: 若后续 doApply 调 router，get 可去 Parallel/Sequential
    else type=parallel
        AW->>Para: 注入的 parallelAgentNode
        Para->>Para: doApply 占位 return null
    else type=sequential
        AW->>Seq: 注入的 sequentialAgentNode
        Seq->>Seq: doApply 占位；get → default（链结束）
    end
```

**文本版（对照上面编号）：**

```text
AgentWorkflowNode
  ① doApply：校验 agentWorkflows → 写入 DynamicContext
  ② get：首项 type → AgentTypeEnum → Loop / Parallel / Sequential
LoopAgentNode / ParallelAgentNode
  ③ doApply：本节占位（return null）
  ④ get：可互跳或进 Sequential（getBean，防循环依赖）
SequentialAgentNode
  ⑤ get：defaultStrategyHandler，本节终点
说明：AgentNode → AgentWorkflowNode 的接线留待后续
```

---

## 二、学习内容与代码对应

### 2.1 改动地图（我的项目）

| 文件 | 作用 |
|------|------|
| `.../model/valobj/enums/AgentTypeEnum.java` | `loop/parallel/sequential` ↔ 节点 Bean 名 |
| `.../armory/factory/DefaultArmoryFactory.java` | `DynamicContext` 增加 `agentWorkflows` |
| `.../armory/AbstractArmorySupport.java` | 注入 `ApplicationContext`，提供 `getBean` |
| `.../armory/node/AgentWorkflowNode.java` | 工作流主流转入口 |
| `.../armory/node/workflow/LoopAgentNode.java` | Loop 支路 + 可转 Parallel/Sequential |
| `.../armory/node/workflow/ParallelAgentNode.java` | Parallel 支路 + 可转 Loop/Sequential |
| `.../armory/node/workflow/SequentialAgentNode.java` | 串行支路，本节作终点 |

包名 `cn.chaney.ai`；学习项目为 `cn.bugstack.ai`。`@Service("loopAgentNode")` 等具名 Bean 须与枚举里 `node` 字段一致。

### 2.2 AgentTypeEnum 怎么读

每个常量三元组：`(说明名, YAML type, Spring Bean 名)`。

| 枚举 | type（配置） | node（分发目标） |
|------|--------------|------------------|
| Loop | `loop` | `loopAgentNode` |
| Parallel | `parallel` | `parallelAgentNode` |
| Sequential | `sequential` | `sequentialAgentNode` |

`formType(String)`：忽略大小写遍历 `values()`（编译器生成的枚举静态方法），按 `type` 字段匹配。  
学习项目方法名是 `formType`（拼写如此），我的项目对齐了；语义上就是「from type」。

### 2.3 配置与 VO

`test-agent.yml` 示例：

```yaml
agent-workflows:
  - type: sequential
    name: CodePipelineAgent
    description: Executes a sequence of code writing, reviewing, and refactoring.
    sub-agents:
      - CodeWriterAgent
      - CodeReviewerAgent
      - CodeRefactorerAgent
```

对应 `AiAgentConfigTableVO.Module.AgentWorkflow`：`type` / `name` / `subAgents` / `description` / `maxIterations`。  
`sub-agents` 的名字应对得上前面 `agents` 里装配进 `agentGroup` 的 `name`。

### 2.4 为何主流转用注入、兄弟互跳用 getBean

```text
AgentWorkflowNode ──@Resource──→ Loop / Parallel / Sequential   （单向，可注入）
Loop ←──getBean──→ Parallel                                      （互跳，避免循环依赖）
```

若 Loop、Parallel 互相 `@Resource`，Spring 创建 Bean 时会成环（Boot 2.6+ 常直接启动失败）。`getBean` 是运行时再取，创建阶段无环。

### 2.5 与 Google ADK 三种工作流的关系

| ADK 运行时 | 本节装配节点 | 本节做到哪 |
|------------|--------------|------------|
| `LoopAgent` | `LoopAgentNode` | 仅流转骨架 |
| `ParallelAgent` | `ParallelAgentNode` | 仅流转骨架 |
| `SequentialAgent` | `SequentialAgentNode` | 仅流转骨架 |

装配节点负责「按配置选中并最终 new 出 ADK Agent」；ADK 负责运行时怎么执行子 Agent。二者同名不同层，不要混。

---

## 三、踩坑注意点

1. **不要以为 2-8 提交后整链已进 Workflow**  
   `AgentNode.get()` 仍是 `defaultStrategyHandler`，与学习项目一致。测分发逻辑需临时接线，或等后续课时。

2. **子节点 `doApply` 返回 `null` 时，其 `get()` 通常不会被走到**  
   规则树一般是 `doApply` 里 `router` 才会再调 `get`。本节 `get()` 是为后续「消费列表 + router」预留的转发逻辑。

3. **列表首项语义要心里有数**  
   当前骨架反复看 `agentWorkflows.get(0)`；真正多段组合时，后续课时通常会配合「消费/移除当前项」再跳下一 type，否则容易和「自己跳自己」搅在一起。

4. **Bean 名必须对上枚举**  
   `@Service("loopAgentNode")` 与 `AgentTypeEnum` 的 `node`、以及 `getBean("loopAgentNode")` 三者不一致会运行失败。

5. **只配 LlmAgent、不配任何 loop/parallel/sequential**  
   `AgentWorkflowNode` 会因空列表或非法 type 报错；课程也提示「裸 LlmAgent 作终点」要后续扩展。

---

## 四、拓展知识

### 4.1 实际项目里工作流只有这三种吗？

Google ADK **内置确定性工作流模板**主要就是 Sequential / Parallel / Loop，并可嵌套。不够用时继承 `BaseAgent` 自定义。  
落地常见做法：优先用这三种积木组合；复杂分支、审批、状态机再自建或换图编排（如 LangGraph）。

### 4.2 复杂作业在想什么

课件：若 `sequentialAgentNode` 还能再转到 `loop` / `sequential`，怎么改？  
思路方向：Sequential 的 `get()` 不再固定 `default`，按剩余配置 `getBean` 下一节点；同时要想清楚终止条件，避免环路跑飞。

### 4.3 自测清单

- [ ] `AgentTypeEnum.formType("sequential")` 能否得到 `sequentialAgentNode`  
- [ ] `DynamicContext` 是否有 `agentWorkflows` 字段  
- [ ] `AbstractArmorySupport#getBean` 是否可用  
- [ ] 三个 workflow 节点 Bean 名是否与枚举一致  
- [ ] `AgentWorkflowNode.get()` 三种 type 是否分发正确  
- [ ] 是否理解：本节官方未把 `AgentNode` 接到 `AgentWorkflowNode`  
- [ ] 是否理解：Loop↔Parallel 用 `getBean` 是为拆循环依赖  
