package com.hefng.mynocodebackend.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.simple-chat-model")
@Data
public class SimpleChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Boolean logRequests;

    private Boolean logResponses;

    /**
     * 推理流式模型，开启 returnThinking，专用于 VUE_PROJECT 代码生成
     */
    @Bean(name = "simpleChatModel")
    @Scope("prototype")
    public ChatModel simpleChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxCompletionTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

}
