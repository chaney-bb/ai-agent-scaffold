package cn.chaney.ai.domain.agent.service.armory.factory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.service.armory.node.RootNode;
import com.google.adk.agents.BaseAgent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chaney
 * @description
 * @create 2026/8/5 18:07
 */
@Service
public class DefaultArmoryFactory {
    @Resource
    private RootNode rootNode;

    public StrategyHandler<ArmoryCommandEntity, DynamicContext, AiAgentRegisterVO> armoryStrategyHandler() {
        return rootNode;
    }

    /**
     * 定义一个上下文对象，用于各个节点串联的时候，写入数据和使用数据
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        /**
         * LLM API
         */
        private OpenAiApi openAiApi;

        /**
         * LLM ChatModel（ChatModelNode 写入）
         */
        private ChatModel chatModel;

        /**
         * 已装配的智能体：key = Agent.name，value = LlmAgent / 工作流 Agent（基类 BaseAgent）
         * RunnerNode 按 runner.agent-name 从此 Map 取入口 Agent
         */
        private Map<String, BaseAgent> agentGroup = new HashMap<>();

        /**
         * 当前待装配的 workflow 配置项（由 AgentWorkflowNode 按步骤写入）
         */
        private AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow;

        /**
         * 已推进的 workflow 步骤下标（装完一项 +1；用于从配置列表取下一项）
         */
        private AtomicInteger currentStepIndex = new AtomicInteger(0);

        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }

        /**
         * 按 subAgents 名称从 agentGroup 取已装配实例；找不到则跳过（不自动创建，依赖配置拓扑序）
         */
        public List<BaseAgent> queryAgentList(List<String> agentNames) {
            if (agentNames == null || agentNames.isEmpty() || agentGroup == null) {
                return Collections.emptyList();
            }

            List<BaseAgent> agents = new ArrayList<>();
            for (String name : agentNames) {
                BaseAgent agent = agentGroup.get(name);
                if (agent != null) {
                    agents.add(agent);
                }
            }
            return agents;
        }

        /** 推进到下一个 workflow 步骤 */
        public void addCurrentStepIndex() {
            currentStepIndex.incrementAndGet();
        }

        public int getCurrentStepIndex() {
            return currentStepIndex.get();
        }

    }
}
