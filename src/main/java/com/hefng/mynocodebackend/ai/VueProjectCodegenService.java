package com.hefng.mynocodebackend.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Vue 工程化项目代码生成 AI 服务接口
 * <p>
 * 与 {@link AiCodegenService} 的区别：
 * 1. 使用深度推理模型（如 deepseek-reasoner），会产生 thinking 过程
 * 2. 通过 @Tool 注解的 {@link com.hefng.mynocodebackend.ai.tool.VueProjectFileSaveTool} 直接写文件，
 *    不再依赖后端解析代码块
 * 3. 返回 TokenStream 而非 Flux<String>，以便通过 onPartialThinking / onPartialResponse
 *    回调分别获取思考过程和最终答案，避免依赖 <think> 标签解析
 * <p>
 * 注意：不使用 @MemoryId，对话记忆通过 AiServices.builder().chatMemory(memory) 在工厂中绑定。
 * 若同时使用 @MemoryId 和 .chatMemory()，LangChain4j 会将 memoryId 解析为 null 导致 NPE。
 *
 * @author hefng
 */
interface VueProjectCodegenService {

    /**
     * 流式生成 Vue 工程化项目
     *
     * @param userMessage 用户需求描述
     * @return TokenStream，通过 onPartialThinking/onPartialResponse 分别订阅思考过程和答案
     */
    @SystemMessage(fromResource = "prompts/vue-project-generator-system-prompt.txt")
    TokenStream generateVueProjectStream(@UserMessage String userMessage);
}
