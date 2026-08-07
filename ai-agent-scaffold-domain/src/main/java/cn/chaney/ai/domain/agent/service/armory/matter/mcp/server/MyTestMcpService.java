package cn.chaney.ai.domain.agent.service.armory.matter.mcp.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * @author chaney
 * @description 本地工具样例：@Tool 方法经 MethodToolCallbackProvider 暴露给 ChatModel
 * @create 2026/8/7 17:30
 */
@Slf4j
@Service
public class MyTestMcpService {

    /** 一个 @Tool = 一个可被模型调用的 tool；多方法即可多 tool */
    @Tool(description = "小写字母转换为大写字母")
    public XxxResponse toUpperCase(XxxRequest request) {
        XxxResponse xxxResponse = new XxxResponse();
        xxxResponse.setContent(request.getWord().toUpperCase());
        return xxxResponse;
    }

    /** 入参 schema：字段注解供模型理解如何填参 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class XxxRequest {
        @JsonProperty(required = true, value = "word")
        @JsonPropertyDescription("英文单词，字符串，字母。例如: good,xiaofuge")
        private String word;
    }

    /** 出参 schema：工具执行结果结构 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class XxxResponse {
        @JsonProperty(required = true, value = "content")
        @JsonPropertyDescription("单词转换结果")
        private String content;
    }
}
