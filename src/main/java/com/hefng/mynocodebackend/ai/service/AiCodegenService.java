package com.hefng.mynocodebackend.ai.service;

import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;
import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 生成代码服务
 *
 * @author hefng
 */
public interface AiCodegenService {

    /**
     * 根据用户输入生成 HTML 代码
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompts/html-generator-system-prompt.txt")
    HTMLCodeResult generateHtml(@UserMessage String userMessage, @MemoryId Long memoryId);

    /**
     * 根据用户输入生成多文件代码(HTML +　CSS +　JS)
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompts/multi-file-generator-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(@UserMessage String userMessage);

    /**
     * 根据用户输入流式生成 HTML 代码（支持深度思考过程输出）
     * @param userMessage
     * @return TokenStream，通过 onPartialThinking/onPartialResponse 分别订阅思考过程和答案
     */
    @SystemMessage(fromResource = "prompts/html-generator-system-prompt.txt")
    TokenStream generateHtmlStream(@UserMessage String userMessage);

    /**
     * 根据用户输入流式生成多文件代码（支持深度思考过程输出）
     * @param userMessage
     * @return TokenStream，通过 onPartialThinking/onPartialResponse 分别订阅思考过程和答案
     */
    @SystemMessage(fromResource = "prompts/multi-file-generator-system-prompt.txt")
    TokenStream generateMultiFileCodeStream(@UserMessage String userMessage);


}
