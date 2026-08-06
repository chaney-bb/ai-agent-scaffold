package cn.chaney.ai.domain.agent.model.valobj.properties;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * @author chaney
 * @description 绑定 ai.agent.config.*（YAML 智能体配置表）
 * @create 2026/8/5 16:00
 */
@Data
@ConfigurationProperties(prefix = "ai.agent.config", ignoreInvalidFields = true)
public class AiAgentAutoConfigProperties {
    /** 是否启用（预留开关；当前 AutoConfig 仍会直接装配 tables） */
    private boolean enabled = false;

    /** key 随意（如 testAgent）；value 为一套完整智能体配置，含 agent-id */
    private Map<String, AiAgentConfigTableVO> tables;
}
