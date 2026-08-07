package cn.chaney.ai.domain.agent.service.armory.matter.plugin;

import com.google.adk.agents.InvocationContext;
import com.google.adk.plugins.BasePlugin;
import com.google.genai.types.Content;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author chaney
 * @description
 * @create 2026/8/7 19:31
 */

@Slf4j
@Service("myTestPlugin")
public class MyTestPlugin extends BasePlugin {
    public MyTestPlugin() {
        super("myTestPlugin");
    }

    public MyTestPlugin(String name) {
        super(name);
    }

    @Override
    public Maybe<Content> onUserMessageCallback(InvocationContext invocationContext, Content userMessage) {

        log.info("用户输入信息:{}", userMessage.text());

        return super.onUserMessageCallback(invocationContext, userMessage);
    }
}
