package com.hefng.mynocodebackend.langgraph4j.service;

import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 * AI 图片搜集服务接口
 * <p>
 * AI 会根据用户提示词，自动调用以下工具完成图片搜集：
 * - {@link com.hefng.mynocodebackend.langgraph4j.tools.PexelsImageSearchTool} 搜索内容图片
 * - {@link com.hefng.mynocodebackend.langgraph4j.tools.LogoGeneratorTool} 生成 Logo
 * - {@link com.hefng.mynocodebackend.langgraph4j.tools.MermaidDiagramTool} 生成架构图
 */
public interface AiImageCollectService {

    /**
     * 根据用户提示词，自动调用工具搜集所需图片资源
     *
     * @param userPrompt 用户对网站的描述，如"创建一个程序员博客网站"
     * @return 搜集到的图片资源列表
     */
    @SystemMessage(fromResource = "prompts/image-collection-system-prompt.txt")
    String collectImages(@UserMessage String userPrompt);
}
