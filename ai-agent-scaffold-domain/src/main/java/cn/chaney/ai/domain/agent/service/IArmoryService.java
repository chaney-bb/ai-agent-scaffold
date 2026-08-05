package cn.chaney.ai.domain.agent.service;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;

import java.util.List;

/**
 * @author chaney
 * @description 装配功能
 * @create 2026/8/5 18:05
 */
public interface IArmoryService {

    void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception;

}
