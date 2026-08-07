# 装配域节点 Loop / Parallel / Sequential 学习笔记

> 对应课程：第 2-9 节 · 装配域节点-Loop、Parallel、Sequential  
> 我的项目分支：`2-9-armory-node-agent-loop-parallel-sequential`  
> 对照学习项目分支：`2-9-armory-node-agent-loop-parallel-sequential`

---

> 学习笔记写法约定见工作台：`D:\javaproject\docs\学习笔记撰写约定.md`（结构：流程理解 + UML → 细聊 / 踩坑 / 拓展）

---

## 一、流程理解（总览）

上一节（2-8）已搭好工作流转发骨架：`AgentWorkflowNode` 按 `type` 分发到 Loop / Parallel / Sequential，但三个节点的 `doApply` 仍是占位。  
本节补齐 **真正装配**：从上下文取出子 Agent，构建 Google ADK 的 `LoopAgent` / `ParallelAgent` / `SequentialAgent`，写回 `agentGroup`；其中 Sequential 作为本期「总入口」再注册进 Spring。

1. **前置：叶子 Agent 已在 `agentGroup`**  
   `AgentNode` 已把各 `LlmAgent` 按 `name` put 进 `DynamicContext.agentGroup`。工作流用配置里的 `sub-agents` 名字去取。

2. **AgentWorkflowNode：写入待消费列表并分发**  
   `doApply`：`setAgentWorkflows(list)` → `router`。  
   `get`：看列表**首项** `type` → `AgentTypeEnum` → 进入对应装配节点。

3. **LoopAgentNode：消费一项，组装循环体**  
   入口：`LoopAgentNode.doApply`。  
   `remove(0)` 取出当前配置 → `queryAgentList(subAgents)` → `LoopAgent.builder()`（含 `maxIterations`）→ `agentGroup.put(name, loopAgent)` → `router` 看剩余列表继续跳。

4. **ParallelAgentNode：同上，组装并行体**  
   入口：`ParallelAgentNode.doApply`。  
   无 `maxIterations`；组装后同样 put 进 `agentGroup`，再 `router`。

5. **SequentialAgentNode：组装串行总入口 + 发布到 Spring**  
   入口：`SequentialAgentNode.doApply`。  
   组装 `SequentialAgent` → put `agentGroup` → **`registerBean(name, SequentialAgent.class, …)`**（对外可查找的根 Agent）→ `router`。  
   `get()` 仍返回 `defaultStrategyHandler`（本节终点；下一节再接 Runner）。

6. **列表如何继续跳**  
   每装完一项就 `remove(0)`，下一跳看**新的**首项 type：Loop↔Parallel 可互跳（`getBean` 防循环依赖），也可进 Sequential；列表空则结束。

一句话：**配置顺序决定装配顺序；`queryAgentList` 只查已存在的 `agentGroup`；Sequential 是本期发布出去的流水线根。**

### 时序图（UML）

```mermaid
sequenceDiagram
    autonumber
    participant AW as AgentWorkflowNode
    participant Ctx as DynamicContext
    participant Loop as LoopAgentNode
    participant Para as ParallelAgentNode
    participant Seq as SequentialAgentNode
    participant Spring as Spring 容器

    AW->>Ctx: setAgentWorkflows(list)
    AW->>AW: get() 按首项 type 分发

    alt 首项 type=loop
        AW->>Loop: 进入 LoopAgentNode
        Loop->>Ctx: remove(0) 取当前工作流配置
        Loop->>Ctx: queryAgentList(subAgents)
        Note over Ctx: 从 agentGroup 按名取已装配 Agent<br/>（LlmAgent 或其它工作流）
        Loop->>Loop: LoopAgent.builder() + maxIterations
        Loop->>Ctx: agentGroup.put(name, loopAgent)
        Loop->>Loop: router → 按剩余首项继续跳
    else 首项 type=parallel
        AW->>Para: 进入 ParallelAgentNode
        Para->>Ctx: remove(0) + queryAgentList
        Para->>Para: ParallelAgent.builder()
        Para->>Ctx: agentGroup.put(name, parallelAgent)
        Para->>Para: router → 继续跳
    else 首项 type=sequential
        AW->>Seq: 进入 SequentialAgentNode
        Seq->>Ctx: remove(0) + queryAgentList
        Seq->>Seq: SequentialAgent.builder()
        Seq->>Ctx: agentGroup.put(name, sequentialAgent)
        Seq->>Spring: registerBean(name, SequentialAgent)
        Seq->>Seq: get() → default（本节链结束）
    end
```

**文本版（对照上面编号）：**

```text
AgentWorkflowNode
  ① setAgentWorkflows → 按首项 type 分发
Loop / Parallel / Sequential（可多次互跳）
  ② remove(0) 消费一项配置
  ③ queryAgentList：按 subAgents 名从 agentGroup 取实例
  ④ builder 组装对应 ADK Agent → put 回 agentGroup
  ⑤ Sequential 额外 registerBean 到 Spring
  ⑥ router：看剩余列表首项 type，空则结束
```

---

## 二、学习内容与代码对应

### 2.1 改动地图（相对 2-8，对齐学习项目 2-9）

| 文件 | 本节增量 |
|------|----------|
| `DefaultArmoryFactory.DynamicContext` | 新增 `queryAgentList(List<String>)` |
| `AbstractArmorySupport` | 新增 `registerBean(...)`（动态注册 Spring Bean） |
| `LoopAgentNode#doApply` | 组装 `LoopAgent` 并 put |
| `ParallelAgentNode#doApply` | 组装 `ParallelAgent` 并 put |
| `SequentialAgentNode#doApply` | 组装 `SequentialAgent` + put + `registerBean` |

`get()` 转发逻辑沿用 2-8，本节不用重写。

### 2.2 `queryAgentList` 在干什么

```java
// 按配置名从 agentGroup 批量取 BaseAgent；找不到则跳过（不报错、不自动创建）
public List<BaseAgent> queryAgentList(List<String> agentNames) { ... }
```

子 Agent 来源通常有两类：

- 前面 `AgentNode` 装好的 `LlmAgent`
- 前面工作流节点已 put 进 `agentGroup` 的 Loop / Parallel（嵌套时）

### 2.3 为何只有 Sequential 注册 Spring

本期设计把 **Sequential 当作整条流水线的总入口**（与 `LoopAgentTest` 里 `InMemoryRunner` 接最外层 Sequential 同一思路）。

| 对象 | `agentGroup` | Spring `registerBean` |
|------|--------------|------------------------|
| Loop / Parallel | 是（给后续工作流当 subAgent） | 否 |
| Sequential | 是 | **是**（对外发布根 Agent） |

`@Service("sequentialAgentNode")` 注册的是**装配节点类**；`registerBean(工作流 name, …)` 注册的是**装配出来的 SequentialAgent 实例**，二者不要混。

### 2.4 与 `LoopAgentTest` 的对照（运行时结构）

测试里手写的结构，正是配置装配要表达的嵌套关系：

```text
Sequential(InitialWriter, Loop(Critic, Refiner))
```

- Loop：只负责「点评 ↔ 改稿」反复迭代  
- Sequential：负责「先写初稿一次，再进入 Loop」  
所以「最后是串行」= 串行是**外层编排**，不是 Loop 跑完变成串行。

---

## 三、踩坑注意点

1. **`agent-workflows` 必须按依赖拓扑排序（本节最大坑）**  
   `queryAgentList` **不会**像 Spring 那样「缺依赖就先去创建」。  
   - 正确：`parallel` 写在前 → `loop` 引用它 → 再 `sequential`  
   - 错误：`loop` 先于其引用的 `parallel` → 查不到 → 静默跳过 → 子节点残缺 / 像创建失败  
   约定：**被引用的先写，Sequential 放最后。**

2. **`queryAgentList` 静默跳过 null**  
   名字写错、顺序写反，往往不会立刻抛「找不到 Bean」，而是装出一个「少了子 Agent」的工作流，更难排查。联调时要核对 `sub-agents` 与 `agentGroup` 的 key。

3. **工作流 `name` 必须唯一**  
   put / `registerBean` 都用 `agentWorkflow.getName()`；重名会覆盖。

4. **`AgentNode` → `AgentWorkflowNode` 本节仍可能未接线**  
   与学习项目 2-9 一致：节点实现齐了，但从 Root 整链跑时，若 `AgentNode.get()` 仍是 `defaultStrategyHandler`，不会自动进工作流。整链验证需临时接线或等后续课时。

5. **类注释别留 2-8 的「占位」措辞**  
   若 Loop/Parallel 类上还写着「本节只做流转骨架」，容易误导；实现已完成后应改掉。

6. **课件里的 `setSequentialAgent` / `RunnerNode`**  
   HTML 可能提前出现；正式 2-9 代码以 put + `registerBean` 为准，Runner 属下一节。

---

## 四、拓展知识

### 4.1 和 Spring 依赖注入的差别

| | Spring Bean | 本节工作流装配 |
|--|-------------|----------------|
| 缺依赖 | 可先创建依赖再注入 | 跳过，装不全 |
| 顺序 | 可由依赖图推导 | **人手保证拓扑序** |

以后若要增强，方向可以是：装配前拓扑排序，或 `queryAgentList` 发现缺失时按 name 递归先装对应 `agentWorkflow`（类似 `getBean` 触发创建）。

### 4.2 Google ADK 工作流文档

复杂作业可对照：[Workflow Agents](https://google.github.io/adk-docs/agents/workflow-agents/)（Sequential / Parallel / Loop 语义与嵌套）。

### 4.3 自测清单

- [ ] `queryAgentList` 已加入 `DynamicContext`
- [ ] `registerBean` 已加入 `AbstractArmorySupport`
- [ ] Loop / Parallel / Sequential 的 `doApply` 均 `remove(0)` + builder + put + `router`
- [ ] 仅 Sequential 调用了 `registerBean`
- [ ] 理解：配置顺序 = 创建顺序；被引用方必须靠前
- [ ] 能讲清 `LoopAgentTest` 为何外层是 Sequential、内层是 Loop
- [ ] 能区分 `@Service("sequentialAgentNode")` 与 `registerBean(工作流名, …)`
