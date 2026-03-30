package com.hefng.mynocodebackend.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 模型配置
 * <p>
 * - streamingChatModel（由 langchain4j auto-config 自动创建）：普通流式模型，不开启深度思考，用于 HTML / MULTI_FILE 生成
 * - reasoningStreamingChatModel：推理模型，开启 returnThinking，专用于 VUE_PROJECT 生成
 */
@Configuration
public class ReasoningStreamingChatModelConfig {

    @Value("${langchain4j.open-ai.reasoning-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.reasoning-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.reasoning-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.reasoning-model.max-tokens}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai.reasoning-model.return-thinking}")
    private Boolean returnThinking;

    @Value("${langchain4j.open-ai.reasoning-model.temperature}")
    private Double temperature;

    @Value("${langchain4j.open-ai.reasoning-model.timeout}")
    private Long timeoutSeconds;

    /**
     * 推理流式模型，开启 returnThinking，专用于 VUE_PROJECT 代码生成
     */
    @Bean(name = "reasoningStreamingChatModel")
    public StreamingChatModel reasoningStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxCompletionTokens(maxTokens)
                .temperature(temperature)
                .returnThinking(returnThinking)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
