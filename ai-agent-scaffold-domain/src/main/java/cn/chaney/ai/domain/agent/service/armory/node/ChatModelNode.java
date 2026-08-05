package cn.chaney.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chaney
 * @description 装配对话模型：挂上 OpenAiApi + MCP 工具，写入上下文
 * @create 2026/8/5 20:35
 */
@Slf4j
@Service
public class ChatModelNode extends AbstractArmorySupport {

    @Resource
    private AgentNode agentNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - ChatModelNode");

        // 上一节点 AiApiNode 放入上下文的大模型 API 客户端
        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();

        // 每个 ToolMcp 配置对应连一个 MCP Server（同步客户端）
        List<McpSyncClient> mcpSyncClients = new ArrayList<>();
        List<AiAgentConfigTableVO.Module.ChatModel.ToolMcp> toolMcpList = chatModelConfig.getToolMcpList();
        for (AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp : toolMcpList) {
            mcpSyncClients.add(createMcpSyncClient(toolMcp));
        }

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel())
                        // Spring AI 桥：把 MCP Client 转成模型可调用的 toolCallbacks
                        .toolCallbacks(SyncMcpToolCallbackProvider.builder()
                                .mcpClients(mcpSyncClients).build()
                                .getToolCallbacks())
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

    /**
     * 按配置建 MCP 客户端：SSE（远程 HTTP）或 Stdio（本地进程）二选一
     */
    private McpSyncClient createMcpSyncClient(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {

        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();

        if (null != sseConfig) {
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

            return mcpSyncClient;
        }

        if (null != stdioConfig) {
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

            return mcpSyncClient;
        }

        throw new RuntimeException("tool mcp sse and stdio is null!");
    }
}
