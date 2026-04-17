package com.hefng.mynocodebackend.langgraph4j.tools;

import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import com.hefng.mynocodebackend.langgraph4j.entity.enums.ImageCategoryEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class LogoGeneratorToolTest {

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Test
    void generateLogo() throws Exception {
        String prompt = "一个简洁的科技风格 Logo，蓝色主色调，包含代码符号，适合程序员博客网站";
        ImageResource resource = logoGeneratorTool.generateLogo(prompt);
        assertNotNull(resource);
        assertFalse(resource.getUrl().isBlank(), "返回的 URL 不应为空");
        assertTrue(resource.getUrl().startsWith("http"), "URL 应以 http 开头");
        assertEquals(ImageCategoryEnum.LOGO, resource.getCategory());
        log.info("Logo 资源: category={}, description={}, url={}", resource.getCategory(), resource.getDescription(), resource.getUrl());
    }

    @Test
    void generateLogoWithEnglishPrompt() throws Exception {
        String prompt = "minimalist tech logo, blue and white color, clean design, suitable for a developer portfolio website";
        ImageResource resource = logoGeneratorTool.generateLogo(prompt);
        assertNotNull(resource);
        assertEquals(ImageCategoryEnum.LOGO, resource.getCategory());
        log.info("Logo 资源 (英文提示词): category={}, url={}", resource.getCategory(), resource.getUrl());
    }
}
