package cn.chaney.ai.domain.agent.service.armory.matter.mcp.client;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author chaney
 * @description MCP/工具装配策略接口：统一入参 ToolMcp，统一出参 ToolCallback[]
 *              现有实现：sse、stdio、local；后续可增 Streamable Http 策略
 * @create 2026/8/7 17:27
 */
public interface ToolMcpCreateService {

    /**
     * 按本条配置构建模型可调用的工具回调（可能含多个 tool）
     */
    ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception;
}
