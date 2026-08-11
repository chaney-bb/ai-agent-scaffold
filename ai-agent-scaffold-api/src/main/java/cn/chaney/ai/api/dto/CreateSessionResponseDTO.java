package cn.chaney.ai.api.dto;

import lombok.Data;

/**
 * @author chaney
 * @description 创建会话响应
 * @create 2026/8/10 20:08
 */
@Data
public class CreateSessionResponseDTO {

    /** 新建会话 ID，后续对话携带 */
    private String sessionId;
}
