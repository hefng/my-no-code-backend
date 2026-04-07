package com.hefng.mynocodebackend.ai;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * AI代码生成类型智能路由服务
 * 使用结构化输出直接返回枚举类型
 *
 * @author hefng
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     *
     * @param userPrompt 用户输入的需求描述
     * @return 推荐的代码生成类型
     */
    @SystemMessage(fromResource = "prompts/codegen-routing-system-prompt.txt")
    CodegenTypeEnum routeCodeGenType(String userPrompt);
}