package com.hefng.mynocodebackend.ai;

import com.hefng.mynocodebackend.ai.tool.VueProjectFileSaveTool;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import dev.langchain4j.community.store.ememory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Vue 工程化项目代码生成服务工厂
 * <p>
 * 与 {@link AiCodeGeneratorServiceFactory} 的区别：
 * 1. 注入了 {@link VueProjectFileSaveTool}，让 AI 可以直接调用工具写文件
 * 2. 每次调用 getService() 都会创建新实例，因为 Tool 实例持有 appId 状态，不能复用
 * 3. 假设：深度推理模型通过 streamingChatModel bean 注入（需在 application.yml 中配置对应模型）
 *
 * @author hefng
 */
@Slf4j
@Component
public class VueProjectCodegenServiceFactory {

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 构建带文件保存工具的 Vue 工程化代码生成服务
     * <p>
     * 每次请求都新建实例，原因：
     * - VueProjectFileSaveTool 持有 appId 状态，不同应用不能共享同一个 Tool 实例
     * - AiServices.builder 构建的服务是有状态的（绑定了特定的 chatMemory 和 tools）
     *
     * @param appId 应用 id，用于隔离对话记忆和文件目录
     * @return 配置好的 VueProjectCodegenService 实例
     */
    public VueProjectCodegenService getService(Long appId) {
        // 构建独立的对话记忆，与 HTML 生成共用同一个 Redis store，但 id 不同
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();

        // 加载历史对话到内存，保证多轮对话上下文连贯
        chatHistoryService.loadChatHistoryToMemory(appId, memory, 20);

        // 每次新建 Tool 实例，绑定当前 appId，保证文件写入到正确的目录
        VueProjectFileSaveTool fileSaveTool = new VueProjectFileSaveTool(appId);

        return AiServices.builder(VueProjectCodegenService.class)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemory(memory)
                // 注册文件保存工具，AI 会在生成代码后自动调用
                .tools(fileSaveTool)
                .build();
    }
}
