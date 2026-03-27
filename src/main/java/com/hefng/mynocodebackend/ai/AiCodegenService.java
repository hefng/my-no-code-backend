package com.hefng.mynocodebackend.ai;

import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;
import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

/**
 * AI 生成代码服务
 *
 * @author hefng
 */
interface AiCodegenService {

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
     * 根据用户输入生成 HTML 代码
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompts/html-generator-system-prompt.txt")
    Flux<String> generateHtmlStream(@UserMessage String userMessage);

    /**
     * 根据用户输入生成多文件代码(HTML +　CSS +　JS)
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompts/multi-file-generator-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(@UserMessage String userMessage);


}
