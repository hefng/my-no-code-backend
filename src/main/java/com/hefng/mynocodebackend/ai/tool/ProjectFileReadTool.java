package com.hefng.mynocodebackend.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Vue 项目文件读取工具
 * <p>
 * 允许 AI 在修改文件前先读取文件内容，了解现有代码结构，
 * 从而生成更精准的 oldContent 用于 {@link ProjectFileEditTool}。
 *
 * @author hefng
 */
@Slf4j
@Component
public class ProjectFileReadTool extends BaseProjectTool {

    /**
     * 读取 Vue 项目中指定文件的完整内容
     *
     * @param relativePath 相对于项目根目录的文件路径，如 "src/App.vue"
     * @param appId        应用 id（由框架自动注入）
     * @return 文件内容字符串；若文件不存在或读取失败则返回错误描述
     */
    @Tool("读取Vue项目中指定文件的内容。在修改文件前，建议先调用此工具获取文件现有内容，以便精准定位需要修改的代码片段。")
    public String readFile(
            @P("相对于项目根目录的文件路径，例如：src/App.vue、src/components/Header.vue") String relativePath,
            @ToolMemoryId Long appId) {

        if (StrUtil.isBlank(relativePath)) {
            return "失败：文件路径不能为空";
        }

        // 路径安全校验
        Path resolvedPath = resolveAndValidatePath(appId, relativePath);
        if (resolvedPath == null) {
            return "失败：非法文件路径，禁止访问项目目录之外的文件";
        }

        if (!resolvedPath.toFile().exists()) {
            return "失败：文件不存在 - " + relativePath;
        }

        if (resolvedPath.toFile().isDirectory()) {
            return "失败：目标路径是一个目录，请使用目录读取工具";
        }

        try {
            String content = FileUtil.readString(resolvedPath.toFile(), StandardCharsets.UTF_8);
            log.info("文件读取成功，appId={}, path={}", appId, resolvedPath);

            // 返回带路径标注的文件内容，方便 AI 理解上下文
            String suffix = FileUtil.getSuffix(relativePath);
            return String.format("文件路径：%s\n```%s\n%s\n```", relativePath, suffix, content);
        } catch (Exception e) {
            log.error("文件读取失败，appId={}, path={}, error={}", appId, relativePath, e.getMessage(), e);
            return "失败：读取文件时发生错误 - " + e.getMessage();
        }
    }
}
