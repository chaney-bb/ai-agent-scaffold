package cn.chaney.ai.domain.agent.model.valobj.properties;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * @author chaney
 * @description
 * @create 2026/8/5 16:00
 */
@Data
@ConfigurationProperties(prefix = "ai.agent.config", ignoreInvalidFields = true)
public class AiAgentAutoConfigProperties {
    /**
     * 是否启用AI Agent自动装配
     */
    private boolean enabled = false;

    private Map<String, AiAgentConfigTableVO> tables;
}
