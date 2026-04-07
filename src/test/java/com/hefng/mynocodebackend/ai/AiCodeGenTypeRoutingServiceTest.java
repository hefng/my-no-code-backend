package com.hefng.mynocodebackend.ai;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.service.AiCodeGenTypeRoutingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class AiCodeGenTypeRoutingServiceTest {

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Test
    void routeCodeGenType() {
        // HTML
        String htmlPrompt = "帮我做一个简单的个人主页，展示姓名和联系方式";
        CodegenTypeEnum htmlResult = aiCodeGenTypeRoutingService.routeCodeGenType(htmlPrompt);
        log.info("prompt: {}, result: {}", htmlPrompt, htmlResult);
        Assertions.assertEquals(CodegenTypeEnum.HTML, htmlResult);

        // MULTI_FILE
        String multiFilePrompt = "帮我做一个带有独立 CSS 和 JS 文件的计时器网页";
        CodegenTypeEnum multiFileResult = aiCodeGenTypeRoutingService.routeCodeGenType(multiFilePrompt);
        log.info("prompt: {}, result: {}", multiFilePrompt, multiFileResult);
        Assertions.assertEquals(CodegenTypeEnum.MULTI_FILE, multiFileResult);

        // VUE_PROJECT
        String vuePrompt = "帮我生成一个完整的 Vue 工程化项目，包含路由和组件";
        CodegenTypeEnum vueResult = aiCodeGenTypeRoutingService.routeCodeGenType(vuePrompt);
        log.info("prompt: {}, result: {}", vuePrompt, vueResult);
        Assertions.assertEquals(CodegenTypeEnum.VUE_PROJECT, vueResult);
    }
}
