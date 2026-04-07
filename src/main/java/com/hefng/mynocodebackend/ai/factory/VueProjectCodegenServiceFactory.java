package com.hefng.mynocodebackend.ai.factory;

import com.hefng.mynocodebackend.ai.service.VueProjectCodegenService;
import com.hefng.mynocodebackend.ai.tool.*;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import dev.langchain4j.community.store.ememory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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
 * 1. 注入了 {@link ProjectFileSaveTool}，让 AI 可以直接调用工具写文件
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

    @Resource
    private ToolManager toolManager;

    /**
     * 构建带文件保存工具的 Vue 工程化代码生成服务
     * <p>
     * appId 通过 @MemoryId 在调用 generateVueProjectStream 时传入，
     * chatMemoryProvider 会按需创建对应的对话记忆，@ToolMemoryId 也能正确获取 appId。
     *
     * @return 配置好的 VueProjectCodegenService 实例
     */
    public VueProjectCodegenService getService() {
        return AiServices.builder(VueProjectCodegenService.class)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> {
                    MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .chatMemoryStore(redisChatMemoryStore)
                            .maxMessages(50)
                            .build();
                    // 加载历史对话到内存，保证多轮对话上下文连贯
                    chatHistoryService.loadChatHistoryToMemory((Long) memoryId, memory, 20);
                    return memory;
                })
                // 注册所有工具，AI 可按需调用：保存、编辑、读取、删除文件，以及读取目录结构
                .tools(toolManager.getAllTools())
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }
}
