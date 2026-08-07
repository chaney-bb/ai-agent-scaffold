package cn.chaney.ai.domain.agent.service.armory.mcp.client.imp;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.service.armory.mcp.client.ToolMcpCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author chaney
 * @description 本地工具策略：按 bean 名取出 ToolCallbackProvider（进程内 @Tool，非 MCP 协议）
 * @create 2026/8/7 17:28
 */
@Slf4j
@Service
public class LocalToolMcpCreateService implements ToolMcpCreateService {

    @Resource
    protected ApplicationContext applicationContext;

    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.LocalParameters local = toolMcp.getLocal();
        // yml local.name = 容器中 ToolCallbackProvider 的 bean 名（如 myToolCallbackProvider）
        ToolCallbackProvider localToolCallbackProvider =
                (ToolCallbackProvider) applicationContext.getBean(local.getName());

        log.info("tool local mcp initialize {}", local.getName());

        return localToolCallbackProvider.getToolCallbacks();
    }
}
