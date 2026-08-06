package cn.chaney.ai.domain.agent.service.armory.node.workflow;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.model.valobj.enums.AgentTypeEnum;
import cn.chaney.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LoopAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaney
 * @description Loop 装配节点：组装 LoopAgent，可再跳 Parallel / Sequential
 * @create 2026/8/6 14:35
 */
@Slf4j
@Service("loopAgentNode")
public class LoopAgentNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - LoopAgentNode");

        // 消费列表首项配置
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();
        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = agentWorkflows.remove(0);

        // 按 subAgents 名取已装配的子 Agent（须已在 agentGroup 中）
        List<BaseAgent> subAgents = dynamicContext.queryAgentList(agentWorkflow.getSubAgents());

        LoopAgent loopAgent =
                LoopAgent.builder()
                        .name(agentWorkflow.getName())
                        .description(agentWorkflow.getDescription())
                        .subAgents(subAgents)
                        .maxIterations(agentWorkflow.getMaxIterations())
                        .build();

        // 写回上下文，供后续工作流按 name 引用
        dynamicContext.getAgentGroup().put(agentWorkflow.getName(), loopAgent);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();
        if (null == agentWorkflows || agentWorkflows.isEmpty()) {
            return defaultStrategyHandler;
        }

        // 按列表首项 type 决定下一跳；可去 Parallel / Sequential，不回自己
        String type = agentWorkflows.get(0).getType();
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.formType(type);
        if (null == agentTypeEnum) {
            throw new RuntimeException("agentWorkflow type is error!");
        }

        // getBean：运行时取 Bean，避免与 ParallelAgentNode 循环依赖
        return switch (agentTypeEnum.getNode()) {
            case "parallelAgentNode" -> getBean("parallelAgentNode");
            case "sequentialAgentNode" -> getBean("sequentialAgentNode");
            default -> defaultStrategyHandler;
        };
    }
}
