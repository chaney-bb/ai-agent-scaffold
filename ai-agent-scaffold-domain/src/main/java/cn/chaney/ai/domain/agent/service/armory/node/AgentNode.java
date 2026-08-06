package cn.chaney.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.google.adk.agents.LlmAgent;
import com.google.adk.models.springai.SpringAI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chaney
 * @description 循环构建 LlmAgent，按 name 写入上下文 agentGroup
 * @create 2026/8/5 20:59
 */
@Slf4j
@Service
public class AgentNode extends AbstractArmorySupport {

    /** 下一跳：工作流编排（Loop / Parallel / Sequential） */
    @Resource
    private AgentWorkflowNode agentWorkflowNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AgentNode");

        // 上一节点 ChatModelNode 放入的对话模型（已挂好 MCP 工具）
        ChatModel chatModel = dynamicContext.getChatModel();

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        // 配置里可有多个 Agent，各自职责不同，后面按 name 取用
        List<AiAgentConfigTableVO.Module.Agent> agents = aiAgentConfigTableVO.getModule().getAgents();

        for (AiAgentConfigTableVO.Module.Agent agentConfig : agents) {
            // Google ADK 的 LlmAgent；model 通过 SpringAI 桥接 Spring AI 的 ChatModel
            LlmAgent llmAgent = LlmAgent.builder()
                    .name(agentConfig.getName())
                    .description(agentConfig.getDescription())
                    .model(new SpringAI(chatModel))
                    .instruction(agentConfig.getInstruction())   // 系统提示 / 行为说明
                    .outputKey(agentConfig.getOutputKey())       // 输出写入会话状态时的键名
                    .build();

            dynamicContext.getAgentGroup().put(agentConfig.getName(), llmAgent);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return agentWorkflowNode;
    }
}
