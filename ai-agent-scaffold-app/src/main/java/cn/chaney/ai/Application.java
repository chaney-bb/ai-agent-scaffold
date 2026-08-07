package cn.chaney.ai;

import cn.chaney.ai.domain.agent.service.armory.mcp.server.MyTestMcpService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication 
@Configurable
public class Application {

    public static void main(String[] args){
        SpringApplication.run(Application.class);
    }

    /** bean 名供 yml local.name 引用；扫 MyTestMcpService 上 @Tool 成 ToolCallback */
    @Bean("myToolCallbackProvider")
    public ToolCallbackProvider testTools(MyTestMcpService testService) {
        return MethodToolCallbackProvider.builder().toolObjects(testService).build();
    }

}
