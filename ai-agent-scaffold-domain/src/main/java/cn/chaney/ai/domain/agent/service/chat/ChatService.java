package cn.chaney.ai.domain.agent.service.chat;

import cn.chaney.ai.domain.agent.model.entity.ChatCommandEntity;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.chaney.ai.domain.agent.model.valobj.SessionMeta;
import cn.chaney.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import cn.chaney.ai.domain.agent.service.IChatService;
import cn.chaney.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.chaney.ai.types.common.Constants;
import cn.chaney.ai.types.enums.ResponseCode;
import cn.chaney.ai.types.exception.AppException;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 会话服务：按 agentId 取已装配 Runner，新建/索引 Session 并处理消息（同步 / 流式 / 多模态）。
 *
 * @author chaney
 * @create 2026/8/5 18:20
 */
@Service
public class ChatService implements IChatService {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    /**
     * 会话目录：key = agentId + userId，value = 该用户在该智能体下的多个 SessionMeta。
     * 对话正文仍在 ADK InMemory SessionService，此处只做索引。
     */
    private final Map<String, List<SessionMeta>> userSessions = new ConcurrentHashMap<>();

    /** 从装配配置表收集已配置的智能体，供调用方选取 agentId */
    @Override
    public List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList() {
        Map<String, AiAgentConfigTableVO> tables = aiAgentAutoConfigProperties.getTables();

        List<AiAgentConfigTableVO.Agent> agentList = new ArrayList<>();
        if (null != tables) {
            for (AiAgentConfigTableVO vo : tables.values()) {
                if (null != vo.getAgent()) {
                    agentList.add(vo.getAgent());
                }
            }
        }

        return agentList;
    }

    /**
     * 为指定用户与智能体新建一个 ADK Session；每次调用都新建并写入会话目录，返回新的 sessionId。
     */
    @Override
    public String createSession(String agentId, String userId) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        // 每次新建 Session，并追加到该用户在该智能体下的会话目录
        Session session = runner.sessionService().createSession(appName, userId).blockingGet();
        SessionMeta meta = SessionMeta.builder()
                .agentId(agentId)
                .userId(userId)
                .sessionId(session.id())
                .createdAt(System.currentTimeMillis())
                .build();

        String key = sessionKey(agentId, userId);
        userSessions.compute(key, (k, list) -> {
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
            }
            list.add(meta);
            return list;
        });
        return session.id();
    }

    /** 查询某用户在某智能体下的会话目录（仅元数据，不含消息正文） */
    @Override
    public List<SessionMeta> listSessions(String agentId, String userId) {
        List<SessionMeta> sessions = userSessions.get(sessionKey(agentId, userId));
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(sessions);
    }

    /** 拼接会话目录缓存 key：agentId + 分隔符 + userId */
    private static String sessionKey(String agentId, String userId) {
        return agentId + Constants.SPLIT + userId;
    }

    /** 未指定 sessionId：先新建会话，再同步发送纯文本并收集回复 */
    @Override
    public List<String> handleMessage(String agentId, String userId, String message) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        String sessionId = createSession(agentId, userId);
        return handleMessage(agentId, userId, sessionId, message);
    }

    /** 在指定 sessionId 下同步发送纯文本，阻塞收集本轮全部 Event 文本后返回 */
    @Override
    public List<String> handleMessage(String agentId, String userId, String sessionId, String message) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        Content userMsg = Content.fromParts(Part.fromText(message));
        // 阻塞收集本轮全部 Event 文本（含中间过程，不只 finalResponse）
        Flowable<Event> events = runner.runAsync(userId, sessionId, userMsg);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));
        return outputs;
    }

    /** 在指定 sessionId 下流式发送纯文本，返回 Event 流供调用方订阅 */
    @Override
    public Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        Content userMsg = Content.fromParts(Part.fromText(message));
        return runner.runAsync(userId, sessionId, userMsg);
    }

    /** 按命令对象发送多模态内容（文本 / 文件 URI / 内联字节），同步收集回复 */
    @Override
    public List<String> handleMessage(ChatCommandEntity chatCommandEntity) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(chatCommandEntity.getAgentId());

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        // 将命令对象中的多模态载体转为 ADK Part，再组装为一条 user Content
        List<Part> parts = new ArrayList<>();

        List<ChatCommandEntity.Content.Text> texts = chatCommandEntity.getTexts();
        if (null != texts && !texts.isEmpty()) {
            for (ChatCommandEntity.Content.Text text : texts) {
                parts.add(Part.fromText(text.getMessage()));
            }
        }

        List<ChatCommandEntity.Content.File> files = chatCommandEntity.getFiles();
        if (null != files && !files.isEmpty()) {
            for (ChatCommandEntity.Content.File file : files) {
                parts.add(Part.fromUri(file.getFileUri(), file.getMimeType()));
            }
        }

        List<ChatCommandEntity.Content.InlineData> inlineDatas = chatCommandEntity.getInlineDatas();
        if (null != inlineDatas && !inlineDatas.isEmpty()) {
            for (ChatCommandEntity.Content.InlineData inlineData : inlineDatas) {
                parts.add(Part.fromBytes(inlineData.getBytes(), inlineData.getMimeType()));
            }
        }

        Content content = Content.builder().role("user").parts(parts).build();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        Flowable<Event> events = runner.runAsync(chatCommandEntity.getUserId(), chatCommandEntity.getSessionId(), content);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));
        return outputs;
    }
}
