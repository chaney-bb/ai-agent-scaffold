package cn.chaney.ai.domain.agent.service;

import cn.chaney.ai.domain.agent.model.entity.ChatCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;

import java.util.List;

/**
 * 会话服务接口：智能体列表、建会话、同步/流式消息、多模态命令。
 *
 * @author chaney
 * @create 2026/8/5 18:05
 */
public interface IChatService {

    /** 查询已配置的智能体列表（供调用方选取 agentId） */
    List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList();

    /** 按 agentId + userId 创建或复用 Session，返回 sessionId */
    String createSession(String agentId, String userId);

    /** 无 sessionId：内部建会话后同步处理纯文本 */
    List<String> handleMessage(String agentId, String userId, String message);

    /** 指定 sessionId 同步处理纯文本 */
    List<String> handleMessage(String agentId, String userId, String sessionId, String message);

    /** 流式返回 ADK Event，由调用方自行订阅 */
    Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message);

    /** 多模态命令对象入参（文本 / 文件 URI / 内联字节） */
    List<String> handleMessage(ChatCommandEntity chatCommandEntity);

}
