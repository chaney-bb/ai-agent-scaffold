package cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.factory;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.ToolMcpCreateService;
import cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.imp.LocalToolMcpCreateService;
import cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.imp.SSEToolMcpCreateService;
import cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.imp.StdioToolMcpCreateService;
import cn.chaney.ai.types.enums.ResponseCode;
import cn.chaney.ai.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author chaney
 * @description MCP 装配工厂：按配置选择 local / sse / stdio 策略（后续可扩 streamableHttp）
 * @create 2026/8/7 17:29
 */
@Slf4j
@Service
public class DefaultMcpClientFactory {

    @Resource
    private LocalToolMcpCreateService localToolMcpCreateService;

    @Resource
    private SSEToolMcpCreateService sseToolMcpCreateService;

    @Resource
    private StdioToolMcpCreateService stdioToolMcpCreateService;

    /**
     * 优先级：local → sse → stdio；均未配置则抛 NOT_FOUND_METHOD
     */
    public ToolMcpCreateService getToolMcpCreateService(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        if (null != toolMcp.getLocal()) {
            return localToolMcpCreateService;
        }
        if (null != toolMcp.getSse()) {
            return sseToolMcpCreateService;
        }
        if (null != toolMcp.getStdio()) {
            return stdioToolMcpCreateService;
        }
        throw new AppException(ResponseCode.NOT_FOUND_METHOD.getCode(), ResponseCode.NOT_FOUND_METHOD.getInfo());
    }

}
