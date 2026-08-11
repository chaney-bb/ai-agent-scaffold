package cn.chaney.ai.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 会话目录元数据：索引用户在某智能体下的多个 session，对话内容仍由 ADK SessionService 按 sessionId 持有。
 *
 * @author chaney
 * @create 2026/8/11
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionMeta {

    private String agentId;

    private String userId;

    private String sessionId;

    /** 创建时间戳（毫秒） */
    private long createdAt;
}
