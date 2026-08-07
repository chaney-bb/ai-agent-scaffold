package cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.imp;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.service.armory.matter.mcp.client.ToolMcpCreateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author chaney
 * @description Stdio 策略：拉起本地 MCP 子进程，经 stdin/stdout 通信后转为 ToolCallback
 * @create 2026/8/7 17:28
 */
@Slf4j
@Service
public class StdioToolMcpCreateService implements ToolMcpCreateService {
    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();


        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters serverParameters = stdioConfig.getServerParameters();

        // 本地 MCP Server 的启动参数：命令 / args / 环境变量
        ServerParameters stdioParams = ServerParameters.builder(serverParameters.getCommand())
                .args(serverParameters.getArgs())
                .env(serverParameters.getEnv())
                .build();

        // Stdio 传输：子进程 stdin/stdout 传 MCP 报文；JacksonMcpJsonMapper 负责 JSON 序列化
        McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(new ObjectMapper())))
                .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout())).build();

        McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

        log.info("tool stdio mcp initialize {}", initialize);

        return SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpSyncClient).build()
                .getToolCallbacks();
    }
}
