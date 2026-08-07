package cn.chaney.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.model.valobj.enums.AgentTypeEnum;
import cn.chaney.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.chaney.ai.domain.agent.service.armory.node.workflow.LoopAgentNode;
import cn.chaney.ai.domain.agent.service.armory.node.workflow.ParallelAgentNode;
import cn.chaney.ai.domain.agent.service.armory.node.workflow.SequentialAgentNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chaney
 * @description 工作流主流转：有 workflow 则分发；无则直接短路到 RunnerNode（支持单体 Agent）
 * @create 2026/8/6 14:28
 */
@Slf4j
@Service
public class AgentWorkflowNode extends AbstractArmorySupport {

    // 单向依赖，可直接注入；兄弟节点互跳见 Loop/Parallel 的 getBean
    @Resource
    private LoopAgentNode loopAgentNode;
    @Resource
    private ParallelAgentNode parallelAgentNode;
    @Resource
    private SequentialAgentNode sequentialAgentNode;
    /** 未配置 agent-workflows 时直达 Runner */
    @Resource
    private RunnerNode runnerNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AgentWorkflowNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = aiAgentConfigTableVO.getModule().getAgentWorkflows();

        // 无 workflow：不写上下文，直接 router → get() 返回 runnerNode
        if (null == agentWorkflows || agentWorkflows.isEmpty()) {
            return router(requestParameter, dynamicContext);
        }

        // 供后续 Loop / Parallel / Sequential 节点读取
        dynamicContext.setAgentWorkflows(agentWorkflows);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();

        // 空列表：跳过 Loop/Parallel/Sequential，进 Runner（由 runner.agent-name 指定入口 Agent）
        if (null == agentWorkflows || agentWorkflows.isEmpty()) {
            return runnerNode;
        }

        // 取列表首项 type，决定进入哪种工作流装配节点
        String type = agentWorkflows.get(0).getType();
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.formType(type);

        if (null == agentTypeEnum) {
            throw new RuntimeException("agentWorkflow type is error!");
        }

        String node = agentTypeEnum.getNode();
        return switch (node) {
            case "loopAgentNode" -> loopAgentNode;
            case "parallelAgentNode" -> parallelAgentNode;
            case "sequentialAgentNode" -> sequentialAgentNode;
            default -> defaultStrategyHandler;
        };
    }
}
