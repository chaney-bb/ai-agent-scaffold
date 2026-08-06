package cn.chaney.ai.domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.service.IArmoryService;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chaney
 * @description 装配服务：按配置表逐个走规则树，注册可对话的 Agent
 * @create 2026/8/5 18:06
 */
@Slf4j
@Service
public class ArmoryService implements IArmoryService {
    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Override
    public void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception {
        // tables 可有多项 → 多个主入口（各自 agentId / Runner），彼此默认隔离
        for (AiAgentConfigTableVO table : tables) {
            StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> handler = defaultArmoryFactory.armoryStrategyHandler();
            // 每次 new DynamicContext，从 RootNode 跑完整装配链
            handler.apply(
                    ArmoryCommandEntity.builder()
                            .aiAgentConfigTableVO(table)
                            .build(),
                    new DefaultArmoryFactory.DynamicContext());
        }
    }
}
