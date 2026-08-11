package cn.chaney.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.ToolMcpCreateService;
import cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.factory.DefaultMcpClientFactory;
import cn.chaney.ai.domain.agent.service.armory.matter.skills.ToolSkillsCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chaney
 * @description 装配对话模型：挂上 OpenAiApi + MCP/本地工具 + Skills，写入上下文
 * @create 2026/8/5 20:35
 */
@Slf4j
@Service
public class ChatModelNode extends AbstractArmorySupport {

    @Resource
    private AgentNode agentNode;

    /** 按 yml 中 sse / stdio / local 选择对应策略，统一产出 ToolCallback */
    @Resource
    private DefaultMcpClientFactory mcpClientFactory;

    @Resource
    private ToolSkillsCreateService toolSkillsCreateService;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - ChatModelNode");

        // 上一节点 AiApiNode 放入上下文的大模型 API 客户端
        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();
        List<AiAgentConfigTableVO.Module.ChatModel.ToolMcp> toolMcpList = chatModelConfig.getToolMcpList();
        List<AiAgentConfigTableVO.Module.ChatModel.ToolSkills> toolSkillsList = chatModelConfig.getToolSkillsList();


        // 每条 ToolMcp 配置 → 工厂选策略 → 汇总为 ToolCallback 列表挂到 ChatModel
        List<ToolCallback> toolCallbackList = new ArrayList<>();
        for (AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp : toolMcpList) {
            ToolMcpCreateService toolMcpCreateService = mcpClientFactory.getToolMcpCreateService(toolMcp);
            ToolCallback[] toolCallbacks = toolMcpCreateService.buildToolCallback(toolMcp);
            toolCallbackList.addAll(List.of(toolCallbacks));
        }

        //构建skills服务
        if(null !=toolSkillsList && !toolSkillsList.isEmpty()){
            for (AiAgentConfigTableVO.Module.ChatModel.ToolSkills toolSkills : toolSkillsList) {
                ToolCallback[] toolCallbacks=toolSkillsCreateService.buildToolCallback(toolSkills);
                toolCallbackList.addAll(List.of(toolCallbacks));
            }
        }

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel())
                        .toolCallbacks(toolCallbackList)
                        .build())
                .build();

        dynamicContext.setChatModel(chatModel);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        // ChatModel 装配完 → 路由到 AgentNode 继续建 LlmAgent
        return agentNode;
    }
}
