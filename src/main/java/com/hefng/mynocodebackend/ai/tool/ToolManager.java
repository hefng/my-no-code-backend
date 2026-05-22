package com.hefng.mynocodebackend.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具管理类
 * 用于集中管理所有暴露给 AI 调用的工具实例，提供统一的访问入口。
 *
 * @author hefng
 */
@Slf4j
@Component
public class ToolManager {

    @Resource
    private BaseProjectTool[] baseProjectTools;

    private final Map<String, BaseProjectTool> TOOL_MAP = new HashMap<>();

    /**
     * 初始化工具映射，将所有 BaseProjectTool 实例按类名存入 TOOL_MAP，方便 AI 根据工具名称调用。
     *
     */
    @PostConstruct
    public void initToolMap() {
        for (BaseProjectTool tool : baseProjectTools) {
            // 同时按工具方法名和类名注册，便于后续通过不同来源的名称查找工具。
            TOOL_MAP.put(tool.getToolName(), tool);
            TOOL_MAP.put(tool.getClass().getSimpleName(), tool);
            for (Method method : tool.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    TOOL_MAP.put(method.getName(), tool);
                }
            }
            log.info("注册工具:{} -> {}", tool.getToolName(), tool.getToolDescription());
        }
        log.info("工具注册完成, 注册工具 {} 个", TOOL_MAP.size());
    }

    /**
     * 根据工具名称获取工具实例
     *
     * @param toolName 工具名称，通常为工具类的简单类名，如 "VueProjectFileSaveTool"
     * @return 对应的 BaseProjectTool 实例；如果未找到则返回 null
     */
    public BaseProjectTool getToolByName(String toolName) {
        BaseProjectTool tool = TOOL_MAP.get(toolName);
        if (tool == null) {
            for (BaseProjectTool candidate : baseProjectTools) {
                if (candidate.getClass().getSimpleName().equals(toolName) || candidate.getToolName().equals(toolName)) {
                    return candidate;
                }
            }
        }
        if (tool == null) {
            log.warn("未找到工具实例，toolName={}", toolName);
        }
        return tool;
    }

    public BaseProjectTool[] getAllTools() {
        return baseProjectTools;
    }


}
