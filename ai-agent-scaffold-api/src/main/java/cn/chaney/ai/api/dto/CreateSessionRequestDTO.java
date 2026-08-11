package cn.chaney.ai.api.dto;

import lombok.Data;

/**
 * @author chaney
 * @description 创建会话请求
 * @create 2026/8/10 20:07
 */
@Data
public class CreateSessionRequestDTO {

    /** 智能体 ID */
    private String agentId;

    /** 用户 ID */
    private String userId;
}
