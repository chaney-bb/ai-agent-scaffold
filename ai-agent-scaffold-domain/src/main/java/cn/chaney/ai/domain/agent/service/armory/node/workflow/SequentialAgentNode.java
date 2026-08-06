package cn.chaney.ai.domain.agent.service.armory.node.workflow;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaney
 * @description Sequential 装配节点：组装串行总入口，并发布到 Spring；本期装配链终点
 * @create 2026/8/6 14:35
 */
@Slf4j
@Service("sequentialAgentNode")
public class SequentialAgentNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - SequentialAgentNode");

        // 消费列表首项配置
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();
        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = agentWorkflows.remove(0);

        // 按 subAgents 名取已装配的子 Agent（须已在 agentGroup 中）
        List<BaseAgent> subAgents = dynamicContext.queryAgentList(agentWorkflow.getSubAgents());

        SequentialAgent sequentialAgent =
                SequentialAgent.builder()
                        .name(agentWorkflow.getName())
                        .description(agentWorkflow.getDescription())
                        .subAgents(subAgents)
                        .build();

        // 写回上下文；同时作为流水线根 Agent 注册到 Spring，供后续查找/运行
        dynamicContext.getAgentGroup().put(agentWorkflow.getName(), sequentialAgent);
        registerBean(agentWorkflow.getName(), SequentialAgent.class, sequentialAgent);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        // 本期不继续互跳，装配链在此结束（Runner 等后续课时再接）
        return defaultStrategyHandler;
    }
}
