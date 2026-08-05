package cn.chaney.ai.test.api.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * @author chaney
 * @description
 * @create 2026/8/5 13:57
 */
@Slf4j
public class SpringAiApiTest {
    public static void main(String[] args) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://apis.itedus.cn/")
                .apiKey(System.getenv().getOrDefault("AI_API_KEY", "your-api-key"))
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1")
                        .build())
                .build();

        String call = chatModel.call("hi 你好哇!");

        log.info("测试结果:{}", call);
    }
}
