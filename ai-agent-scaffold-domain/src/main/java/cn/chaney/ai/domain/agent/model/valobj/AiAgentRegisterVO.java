package cn.chaney.ai.domain.agent.model.valobj;

import com.google.adk.runner.InMemoryRunner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author chaney
 * @description 装配完成后注册到 Spring 的结果：元信息 + 可对话的 InMemoryRunner
 * @create 2026/8/5 18:08
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentRegisterVO {
    /** 应用名（传给 InMemoryRunner） */
    private String appName;

    /** 智能体 ID（同时作为 Spring Bean 名） */
    private String agentId;

    /** 智能体名称 */
    private String agentName;

    /** 智能体描述 */
    private String agentDesc;

    /** 会话运行器（内存 Session，非持久化 Agent 定义） */
    private InMemoryRunner runner;
}
