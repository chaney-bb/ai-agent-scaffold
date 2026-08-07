package cn.chaney.ai.domain.agent.model.valobj;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author chaney
 * @description 智能体配置表值对象
 * @create 2026/8/5 15:59
 */
@Data
public class AiAgentConfigTableVO {
    /**
     * 应用名称
     */
    private String appName;

    /**
     * 智能体配置 智能体的id，name，描述
     */
    private Agent agent;

    /**
     * 智能体模块 里面包括智能体：
     * api调用信息、chatModel信息（MCP调用）、子agent信息、agent工作流类型信息
     */
    private Module module;

    @Data
    public static class Agent {

        /**
         * 智能体ID
         */
        private String agentId;

        /**
         * 智能体名称
         */
        private String agentName;

        /**
         * 智能体描述
         */
        private String agentDesc;

    }

    @Data
    public static class Module {

        private AiApi aiApi;

        private ChatModel chatModel;

        private List<Agent> agents;

        private List<AgentWorkflow> agentWorkflows;

        /** Runner 入口：指定装入 InMemoryRunner 的 agent 名称（单体或组合均可） */
        private Runner runner;

        @Data
        public static class AiApi {
            private String baseUrl;
            private String apiKey;
            private String completionsPath = "/v1/chat/completions";
            private String embeddingsPath = "/v1/embeddings";

        }

        @Data
        public static class ChatModel {

            private String model;
            private List<ToolMcp> toolMcpList;

            @Data
            public static class ToolMcp {

                private SSEServerParameters sse;

                private StdioServerParameters stdio;

                @Data
                public static class SSEServerParameters {
                    private String name;
                    private String baseUri;
                    private String sseEndpoint;
                    private Integer requestTimeout = 3000;
                }

                @Data
                public static class StdioServerParameters {
                    private String name;
                    private Integer requestTimeout = 3000;
                    private ServerParameters serverParameters;

                    @Data
                    public static class ServerParameters {
                        private String command;
                        private List<String> args;
                        private Map<String, String> env;

                    }
                }

            }
        }

        @Data
        public static class Agent {
            private String name;
            private String instruction;
            private String description;
            private String outputKey;

        }

        @Data
        public static class AgentWorkflow {
            /**
             * 类型；loop、parallel、sequential
             */
            private String type;
            private String name;
            private List<String> subAgents;
            private String description;
            private Integer maxIterations = 3;

        }

        @Data
        public static class Runner {
            /** 对应 agents / agent-workflows 中的 name，供 RunnerNode 从 agentGroup 按名取用 */
            private String agentName;
        }
    }


}
