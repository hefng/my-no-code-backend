package com.hefng.mynocodebackend.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Vue 工程化项目文件保存工具
 * <p>
 * 通过 LangChain4j @Tool 注解暴露给 AI，由 AI 在生成代码后主动调用此工具写入文件，
 * 彻底替代"后端解析代码块"的方式，让文件保存的时机和内容完全由 AI 决策。
 * <p>
 * 每个 appId 对应一个独立的项目目录，路径格式：
 * {CODEGEN_DIR}/vue_project_{appId}/
 *
 * @author hefng
 */
@Slf4j
@Component
public class ProjectFileSaveTool extends BaseProjectTool {

    /**
     * 将单个文件内容写入 Vue 项目目录
     * <p>
     * AI 在生成每个文件后调用此工具，参数由 AI 自行决定文件路径和内容。
     * 安全校验：禁止路径穿越（../），防止写入项目目录之外的文件。
     *
     * @param relativePath 相对于项目根目录的文件路径，如 "src/App.vue"、"package.json"
     * @param content      文件内容
     * @return 操作结果描述，供 AI 判断是否成功
     */
    @Tool("将指定内容写入Vue项目的文件。relativePath为相对于项目根目录的路径（如src/App.vue），content为文件内容。每次只写一个文件。")
    public String saveFile(
            @P("相对于项目根目录的文件路径，例如：src/App.vue、package.json、vite.config.js") String relativePath,
            @P("文件的完整内容") String content,
            @ToolMemoryId Long appId) {

        // 参数基础校验
        if (StrUtil.isBlank(relativePath) || content == null) {
            return "失败：文件路径或内容不能为空";
        }

        // 安全校验：防止路径穿越攻击，使用基类统一校验逻辑
        Path resolvedPath = resolveAndValidatePath(appId, relativePath);
        if (resolvedPath == null) {
            return "失败：非法文件路径，禁止写入项目目录之外的文件";
        }

        try {
            // 确保父目录存在（如 src/components/ 不存在时自动创建）
            FileUtil.mkParentDirs(resolvedPath.toFile());
            FileUtil.writeString(content, resolvedPath.toFile(), StandardCharsets.UTF_8);

            log.info("Vue项目文件写入成功，appId={}, path={}", appId, resolvedPath);
            return "文件保存成功: " + relativePath;
        } catch (Exception e) {
            log.error("Vue项目文件写入失败，appId={}, path={}, error={}", appId, relativePath, e.getMessage(), e);
            return "失败：写入文件时发生错误 - " + e.getMessage();
        }
    }

    @Override
    protected String getToolName() {
        return "fileSaveTool";
    }

    @Override
    protected String getToolDescription() {
        return "文件保存";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String resolvedPath = arguments.getStr("resolvedPath");
        String content = arguments.getStr("content");
        // 文件扩展名
        String suffix = FileUtil.getSuffix(resolvedPath);
        return String.format("""
                    [文件保存] %s
                    ```%s
                    %s
                    ```
                    """, resolvedPath, suffix, content);
    }
}
