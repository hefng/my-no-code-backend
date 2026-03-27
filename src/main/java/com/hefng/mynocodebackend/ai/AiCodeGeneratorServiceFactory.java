package com.hefng.mynocodebackend.ai;

import com.hefng.mynocodebackend.service.ChatHistoryService;
import dev.langchain4j.community.store.ememory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    public AiCodegenService getAiCodeGeneratorService(Long appId) {
        // 根据 id 构建独立的对话记忆
        MessageWindowChatMemory memory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 加载历史对话到内存
        chatHistoryService.loadChatHistoryToMemory(appId, memory, 20);
        return AiServices.builder(AiCodegenService.class)
                .chatMemory(memory)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }

    @Bean
    public AiCodegenService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

}
