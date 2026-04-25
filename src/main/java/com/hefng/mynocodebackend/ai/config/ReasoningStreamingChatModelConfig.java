package com.hefng.mynocodebackend.ai.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

/**
 * AI 模型配置
 * - reasoningStreamingChatModel：推理模型，开启 returnThinking
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Boolean returnThinking;

    private Double temperature;

    private Long timeout;

    /**
     * 推理流式模型，开启 returnThinking，专用于 VUE_PROJECT 代码生成
     */
    @Bean(name = "reasoningStreamingChatModel")
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxCompletionTokens(maxTokens)
                .temperature(temperature)
                .returnThinking(returnThinking)
                .timeout(Duration.ofSeconds(timeout))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
