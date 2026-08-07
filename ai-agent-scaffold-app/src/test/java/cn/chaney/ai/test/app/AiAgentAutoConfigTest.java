package cn.chaney.ai.test.app;

import cn.chaney.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.alibaba.fastjson.JSON;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author chaney
 * @description 端到端验证：启动装配后从容器取 Runner 发起真实对话（对照学习项目 2-11）
 * @create 2026/8/6 17:02
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiAgentAutoConfigTest {

    @Resource
    private ApplicationContext applicationContext;

    @Test
    public void test_agent() throws InterruptedException {
        // Bean 名 = YAML agent-id；由 RunnerNode 注册
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100001", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // 内存会话；用户 id 可自定义
        Session session = runner.sessionService()
                .createSession(appName, "chaney")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("编写冒泡排序"));
        Flowable<Event> events = runner.runAsync("chaney", session.id(), userMsg);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", JSON.toJSONString(outputs));

        // 故意挂起，便于观察日志（非断言需要）
        new CountDownLatch(1).await();
    }
    /** 第 2-12 节：验证单体智能体 only-one-agent（agent-id=100003，无 agent-workflows） */
    @Test
    public void test_handlerMessage_03() {
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100003", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService()
                .createSession(appName, "xiaofuge")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("给我目前是27届秋招，想找一份ai应用开发，想知道目前招聘要求是怎么样的"));
        Flowable<Event> events = runner.runAsync("xiaofuge", session.id(), userMsg);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", JSON.toJSONString(outputs));
    }
}
