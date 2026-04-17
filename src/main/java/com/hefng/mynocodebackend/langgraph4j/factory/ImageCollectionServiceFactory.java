package com.hefng.mynocodebackend.langgraph4j.factory;

import com.hefng.mynocodebackend.langgraph4j.service.AiImageCollectService;
import com.hefng.mynocodebackend.langgraph4j.tools.LogoGeneratorTool;
import com.hefng.mynocodebackend.langgraph4j.tools.MermaidDiagramTool;
import com.hefng.mynocodebackend.langgraph4j.tools.PexelsImageSearchTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private PexelsImageSearchTool pexelsImageSearchTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    /**
     * 创建图片收集 AI 服务
     */
    @Bean
    public AiImageCollectService createImageCollectionService() {
        return AiServices.builder(AiImageCollectService.class)
                .chatModel(chatModel)
                .tools(
                        pexelsImageSearchTool,
                        mermaidDiagramTool,
                        logoGeneratorTool
                )
                .build();
    }
}