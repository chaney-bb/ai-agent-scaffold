package cn.chaney.ai.api.dto;

import lombok.Data;

/**
 * @author chaney
 * @description 同步对话响应
 * @create 2026/8/10 20:07
 */
@Data
public class ChatResponseDTO {

    /** 模型回复文本（多段用换行拼接） */
    private String content;
}
