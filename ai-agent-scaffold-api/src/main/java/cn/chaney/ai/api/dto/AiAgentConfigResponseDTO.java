package cn.chaney.ai.api.dto;

import lombok.Data;

/**
 * @author chaney
 * @description 智能体配置列表项（对外）
 * @create 2026/8/10 20:07
 */
@Data
public class AiAgentConfigResponseDTO {

    /** 智能体 ID */
    private String agentId;

    /** 智能体名称 */
    private String agentName;

    /** 智能体描述 */
    private String agentDesc;

}
