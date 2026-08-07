package cn.chaney.ai.domain.agent.service.armory.mcp.client.imp;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.service.armory.mcp.client.ToolMcpCreateService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;

/**
 * @author chaney
 * @description SSE 策略：HttpClientSseClientTransport 连远程 MCP → SyncMcpToolCallbackProvider
 * @create 2026/8/7 17:28
 */
@Slf4j
@Service
public class SSEToolMcpCreateService implements ToolMcpCreateService {
    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();

        String originalBaseUri = sseConfig.getBaseUri();
        String baseUri = originalBaseUri;
        String sseEndpoint = sseConfig.getSseEndpoint();

        // Transport 要 baseUri + sseEndpoint 两段；配置若只给整链 URL 则在此拆开
        if (StringUtils.isBlank(sseEndpoint)) {
            URL url = new URL(originalBaseUri);

            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();

            String baseUrl = port == -1 ? protocol + "://" + host : protocol + "://" + host + ":" + port;

            int index = originalBaseUri.indexOf(baseUrl);
            if (index != -1) {
                sseEndpoint = originalBaseUri.substring(index + baseUrl.length());
            }

            baseUri = baseUrl;
        }

        sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

        // MCP 传输层：HTTP + SSE 连远程 Server
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder(baseUri)
                .sseEndpoint(sseEndpoint)
                .build();

        // McpClient.sync：基于传输层构建同步客户端
        McpSyncClient mcpSyncClient = McpClient
                .sync(sseClientTransport)
                .requestTimeout(Duration.ofMillis(sseConfig.getRequestTimeout())).build();
        // MCP 协议握手（版本/能力协商），通过后才能列工具、调工具
        McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

        log.info("tool sse mcp initialize {}", initialize);

        return SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpSyncClient).build()
                .getToolCallbacks();
    }
}
