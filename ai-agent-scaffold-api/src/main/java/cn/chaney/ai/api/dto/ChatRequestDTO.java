package cn.chaney.ai.api.dto;

import lombok.Data;

/**
 * @author chaney
 * @description 对话请求：路由信息 + 用户消息
 * @create 2026/8/10 20:07
 */
@Data
public class ChatRequestDTO {

    /** 智能体 ID */
    private String agentId;

    /** 用户 ID */
    private String userId;

    /** 会话 ID，可空（空则 chat 接口内自动创建） */
    private String sessionId;

    /** 用户输入文本 */
    private String message;
}
