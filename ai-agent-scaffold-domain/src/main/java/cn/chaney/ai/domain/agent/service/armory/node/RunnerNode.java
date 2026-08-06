package cn.chaney.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.runner.InMemoryRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author chaney
 * @description 装配链终点：用 SequentialAgent 构建 InMemoryRunner，封装为 VO 注册到 Spring
 * @create 2026/8/6 16:44
 */
@Slf4j
@Service
public class RunnerNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - RunnerNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        // 应用名 + Agent 元信息（注册 Bean 名用 agentId）
        String appName = aiAgentConfigTableVO.getAppName();
        AiAgentConfigTableVO.Agent agent = aiAgentConfigTableVO.getAgent();
        String agentId = agent.getAgentId();
        String agentName = agent.getAgentName();
        String agentDesc = agent.getAgentDesc();

        // SequentialAgentNode 写入；InMemoryRunner 用内存 Session 跑该 Agent
        SequentialAgent sequentialAgent = dynamicContext.getSequentialAgent();
        InMemoryRunner runner = new InMemoryRunner(sequentialAgent, appName);

        AiAgentRegisterVO aiAgentRegisterVO = AiAgentRegisterVO.builder()
                .appName(appName)
                .agentId(agentId)
                .agentName(agentName)
                .agentDesc(agentDesc)
                .runner(runner)
                .build();

        // 按 agentId 挂到容器，后续对话从此取 runner
        registerBean(agentId, AiAgentRegisterVO.class, aiAgentRegisterVO);

        return aiAgentRegisterVO;
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        // 装配链在此结束
        return defaultStrategyHandler;
    }
}
