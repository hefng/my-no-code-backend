package com.hefng.mynocodebackend.ai;

import cn.hutool.core.lang.Assert;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;
import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;
import com.hefng.mynocodebackend.core.CodeFileSaver;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
class AiCodegenServiceTest {

    @Resource
    private AiCodegenService aiCodegenService;

    @Resource
    private AiCodegenServiceFaced aiCodegenServiceFaced;

    @Test
    void generateHTML() {
        HTMLCodeResult htmlCodeResult = aiCodegenService.generateHtml("请帮我生成一个简单的个人博客首页, 不超过50行代码", 1L);
        System.out.println(htmlCodeResult);
    }

    @Test
    void testChatMemory() {
        HTMLCodeResult result = aiCodegenService.generateHtml("做个程序员鱼皮的工具网站，总代码量不超过 20 行", 1L);
        Assertions.assertNotNull(result);
        result = aiCodegenService.generateHtml("不要生成网站，告诉我你刚刚做了什么？", 1L);
        Assertions.assertNotNull(result);
        result = aiCodegenService.generateHtml("做个程序员鱼皮的工具网站，总代码量不超过 20 行", 2L);
        Assertions.assertNotNull(result);
        result = aiCodegenService.generateHtml("不要生成网站，告诉我你刚刚做了什么？", 2L);
        Assertions.assertNotNull(result);
    }


    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCodeResult = aiCodegenService.generateMultiFileCode("写一个helloWorld, 网页上只展示[helloWorld], 但是要有css和js文件, css文件设置字体为红色, js文件设置点击helloWorld弹出alert");
        CodeFileSaver.saveMultiFileCodeToFile(multiFileCodeResult.getHtmlCode(), multiFileCodeResult.getCssCode(), multiFileCodeResult.getJsCode());
    }

    @Test
    void generateHtmlStream() {
        Flux<String> codeStream = aiCodegenServiceFaced.generateAndSaveCodeWithStream("请帮我生成一个简单的个人博客首页, 不超过50行代码", CodegenTypeEnum.HTML, 1L);
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        String HtmlResult = String.join("", result);
        System.out.println(HtmlResult);
    }

    @Test
    void generateMultiFileCodeStream() {
    }
}