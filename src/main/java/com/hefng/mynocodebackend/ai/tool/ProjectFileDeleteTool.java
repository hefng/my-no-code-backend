package com.hefng.mynocodebackend.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Vue 项目文件删除工具
 * <p>
 * 允许 AI 删除项目中不再需要的文件，例如重构时移除旧组件。
 * 安全限制：只能删除当前应用项目目录内的文件，禁止跨目录操作。
 *
 * @author hefng
 */
@Slf4j
@Component
public class ProjectFileDeleteTool extends BaseProjectTool {

    /**
     * 删除 Vue 项目中的指定文件
     *
     * @param relativePath 相对于项目根目录的文件路径，如 "src/components/OldComponent.vue"
     * @param appId        应用 id（由框架自动注入）
     * @return 操作结果描述
     */
    @Tool("删除Vue项目中的指定文件。relativePath 为相对于项目根目录的路径，例如 src/components/OldComponent.vue。")
    public String deleteFile(
            @P("相对于项目根目录的文件路径，例如：src/components/OldComponent.vue") String relativePath,
            @ToolMemoryId Long appId) {

        if (StrUtil.isBlank(relativePath)) {
            return "失败：文件路径不能为空";
        }

        // 路径安全校验，防止删除项目目录之外的文件
        Path resolvedPath = resolveAndValidatePath(appId, relativePath);
        if (resolvedPath == null) {
            return "失败：非法文件路径，禁止访问项目目录之外的文件";
        }

        if (!resolvedPath.toFile().exists()) {
            return "失败：文件不存在 - " + relativePath;
        }

        // 只允许删除文件，不允许删除目录（防止误删整个目录）
        if (resolvedPath.toFile().isDirectory()) {
            return "失败：目标路径是一个目录，本工具只支持删除文件";
        }

        try {
            FileUtil.del(resolvedPath.toFile());
            log.info("文件删除成功，appId={}, path={}", appId, resolvedPath);
            return "成功：文件已删除 - " + relativePath;
        } catch (Exception e) {
            log.error("文件删除失败，appId={}, path={}, error={}", appId, relativePath, e.getMessage(), e);
            return "失败：删除文件时发生错误 - " + e.getMessage();
        }
    }
}
