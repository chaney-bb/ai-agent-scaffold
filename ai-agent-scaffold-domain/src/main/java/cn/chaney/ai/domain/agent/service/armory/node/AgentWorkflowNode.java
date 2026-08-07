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
 * @description 工作流星型分发中心：按步骤取出当前 workflow，分发到 Loop/Parallel/Sequential；装完一项后子节点再回到本节点
 * @create 2026/8/6 14:28
 */
@Slf4j
@Service
public class AgentWorkflowNode extends AbstractArmorySupport {

    /** 单向依赖，可直接注入；子节点装完后通过 getBean 回到本节点，避免互跳 */
    @Resource
    private LoopAgentNode loopAgentNode;
    @Resource
    private ParallelAgentNode parallelAgentNode;
    @Resource
    private SequentialAgentNode sequentialAgentNode;
    /** 无待处理 workflow 时直达 Runner */
    @Resource
    private RunnerNode runnerNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AgentWorkflowNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = aiAgentConfigTableVO.getModule().getAgentWorkflows();

        // 未配置 / 已全部装完：清空当前项，router → get() 进 RunnerNode
        if (null == agentWorkflows || agentWorkflows.isEmpty()
                || dynamicContext.getCurrentStepIndex() >= agentWorkflows.size()) {
            dynamicContext.setCurrentAgentWorkflow(null);
            return router(requestParameter, dynamicContext);
        }

        // 取出第 N 项写入上下文，供子节点只读装配
        dynamicContext.setCurrentAgentWorkflow(agentWorkflows.get(dynamicContext.getCurrentStepIndex()));
        dynamicContext.addCurrentStepIndex();

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow = dynamicContext.getCurrentAgentWorkflow();

        // null：没有下一项，进入 Runner（由 runner.agent-name 指定入口）
        if (null == currentAgentWorkflow) {
            return runnerNode;
        }

        // 按当前项 type 分发到对应装配节点
        String type = currentAgentWorkflow.getType();
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.formType(type);

        if (null == agentTypeEnum) {
            throw new RuntimeException("agentWorkflow type is error!");
        }

        String node = agentTypeEnum.getNode();
        return switch (node) {
            case "loopAgentNode" -> loopAgentNode;
            case "parallelAgentNode" -> parallelAgentNode;
            case "sequentialAgentNode" -> sequentialAgentNode;
            default -> runnerNode;
        };
    }
}
