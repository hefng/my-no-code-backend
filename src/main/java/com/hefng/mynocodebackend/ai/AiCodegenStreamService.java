package com.hefng.mynocodebackend.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface AiCodegenStreamService {

    /**
     * 根据用户输入生成 HTML 代码
     * @param userInput
     * @return
     */
    @SystemMessage(fromResource = "prompts/html-generator-system-prompt.txt")
    Flux<String> generateHtmlStream(String userInput);

    /**
     * 根据用户输入生成多文件代码(HTML +　CSS +　JS)
     * @param userInput
     * @return
     */
    @SystemMessage(fromResource = "prompts/multi-file-generator-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userInput);

}
