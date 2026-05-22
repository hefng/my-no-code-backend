package com.hefng.mynocodebackend.ai.factory;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hefng.mynocodebackend.ai.guardrail.PromptSafetyInputGuardrail;
import com.hefng.mynocodebackend.ai.service.VueProjectCodegenService;
import com.hefng.mynocodebackend.ai.tool.BaseProjectTool;
import com.hefng.mynocodebackend.ai.tool.ToolManager;
import com.hefng.mynocodebackend.model.dto.sse.ToolStreamEvent;
import com.hefng.mynocodebackend.model.enums.SseEventTypeEnum;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import com.hefng.mynocodebackend.utils.SseEventBuilder;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.community.store.ememory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

/**
 * Vue 工程化项目代码生成服务工厂。
 * <p>
 * 负责把 LangChain4j 的工具调用过程包装成 SSE 事件流。
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
     * 构建带文件保存工具的 Vue 工程化代码生成服务。
     */
    public VueProjectCodegenService getService(Sinks.Many<String> sink) {
        return AiServices.builder(VueProjectCodegenService.class)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> {
                    MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .chatMemoryStore(redisChatMemoryStore)
                            .maxMessages(50)
                            .build();
                    chatHistoryService.loadChatHistoryToMemory((Long) memoryId, memory, 20);
                    return memory;
                })
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .tools(toolManager.getAllTools())
                .beforeToolExecution(before -> sink.tryEmitNext(SseEventBuilder.build(SseEventTypeEnum.TOOL,
                        buildToolStreamEvent("executing", before.request(), null))))
                .afterToolExecution(after -> sink.tryEmitNext(SseEventBuilder.build(SseEventTypeEnum.TOOL,
                        buildToolStreamEvent("completed", after.request(), after.result()))))
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }

    /**
     * 构建工具流式事件。
     * <p>
     * completed 阶段会把工具真实执行结果写入 toolResult，再交给各工具自己的
     * generateToolExecutedResult 方法生成前端展示文案。
     */
    private ToolStreamEvent buildToolStreamEvent(String phase, ToolExecutionRequest request, String result) {
        BaseProjectTool tool = toolManager.getToolByName(request.name());
        JSONObject arguments = parseArguments(request.arguments());
        if (StrUtil.isNotBlank(result)) {
            arguments.put("toolResult", result);
        }

        String message;
        if (tool == null) {
            message = StringUtils.defaultIfBlank(result, "工具执行完成");
        } else if ("executing".equals(phase)) {
            message = tool.generateToolRequestResponse();
        } else {
            message = tool.generateToolExecutedResult(arguments);
        }

        return new ToolStreamEvent(
                phase,
                request.id(),
                request.name(),
                message,
                request.arguments(),
                result
        );
    }

    private JSONObject parseArguments(String rawArguments) {
        if (StringUtils.isBlank(rawArguments)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(rawArguments);
        } catch (Exception e) {
            log.warn("解析工具参数失败，arguments={}", rawArguments, e);
            return new JSONObject();
        }
    }
}
