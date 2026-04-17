package com.hefng.mynocodebackend.langgraph4j.tools;

import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import com.hefng.mynocodebackend.langgraph4j.entity.enums.ImageCategoryEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class PexelsImageSearchToolTest {

    @Resource
    private PexelsImageSearchTool pexelsImageSearchTool;

    @Test
    void searchImages() {
        List<ImageResource> resources = pexelsImageSearchTool.searchImages("nature");
        assertNotNull(resources);
        assertFalse(resources.isEmpty(), "图片列表不应为空");
        assertTrue(resources.size() <= 12, "图片数量不应超过 12 张");
        resources.forEach(r -> {
            assertNotNull(r.getUrl());
            assertTrue(r.getUrl().startsWith("http"), "图片 URL 应以 http 开头");
            assertEquals(ImageCategoryEnum.CONTENT, r.getCategory());
            log.info("图片资源: category={}, description={}, url={}", r.getCategory(), r.getDescription(), r.getUrl());
        });
        log.info("共获取到 {} 张图片", resources.size());
    }

    @Test
    void searchImagesWithChineseQuery() {
        List<ImageResource> resources = pexelsImageSearchTool.searchImages("博客 网站");
        assertNotNull(resources);
        log.info("中文关键词搜索结果数量: {}", resources.size());
        resources.forEach(r -> log.info("图片资源: {}", r));
    }
}
