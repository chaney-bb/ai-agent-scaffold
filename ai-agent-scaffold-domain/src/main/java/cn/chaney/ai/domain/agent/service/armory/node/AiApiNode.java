package cn.chaney.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.chaney.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author chaney
 * @description 装配 OpenAiApi（baseUrl + completionsPath 由厂商兼容地址决定，勿重复叠 v1）
 * @create 2026/8/5 18:07
 */
@Slf4j
@Service
public class AiApiNode extends AbstractArmorySupport {

    @Resource
    private ChatModelNode chatModelNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AiApiNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.AiApi aiApiConfig = aiAgentConfigTableVO.getModule().getAiApi();

        // 空 path 会回退默认 v1/chat/completions；若 baseUrl 已含 /v1，应显式写 /chat/completions
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(aiApiConfig.getBaseUrl())
                .apiKey(aiApiConfig.getApiKey())
                .completionsPath(StringUtils.isNotBlank(aiApiConfig.getCompletionsPath()) ? aiApiConfig.getCompletionsPath() : "v1/chat/completions")
                .embeddingsPath(StringUtils.isNotBlank(aiApiConfig.getEmbeddingsPath()) ? aiApiConfig.getEmbeddingsPath() : "v1/embeddings")
                .build();

        dynamicContext.setOpenAiApi(openAiApi);
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return chatModelNode;
    }
}
