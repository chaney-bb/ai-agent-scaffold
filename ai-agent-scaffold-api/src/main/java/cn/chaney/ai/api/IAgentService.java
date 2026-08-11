package cn.chaney.ai.api;

import cn.chaney.ai.api.dto.*;
import cn.chaney.ai.api.response.Response;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

/**
 * @author chaney
 * @description 智能体对外服务接口：列表、建会话、同步/流式对话
 * @create 2026/8/10 20:06
 */
public interface IAgentService {

    /** 查询已装配的智能体配置列表 */
    Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList();

    /** 按 agentId + userId 创建会话，返回 sessionId */
    Response<CreateSessionResponseDTO> createSession(CreateSessionRequestDTO requestDTO);

    /** 同步对话；sessionId 为空时内部先建会话 */
    Response<ChatResponseDTO> chat(ChatRequestDTO requestDTO);

    /** 流式对话，通过 ResponseBodyEmitter 分片写出 */
    ResponseBodyEmitter chatStream(ChatRequestDTO requestDTO);
}
