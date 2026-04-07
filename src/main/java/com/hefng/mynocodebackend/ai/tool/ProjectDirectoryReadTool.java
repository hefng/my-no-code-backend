package com.hefng.mynocodebackend.ai.tool;

import cn.hutool.core.io.FileUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Vue 项目目录读取工具
 * <p>
 * 允许 AI 查看项目的文件目录结构，了解当前项目包含哪些文件，
 * 从而在修改或删除文件前做出更准确的决策。
 *
 * @author hefng
 */
@Slf4j
@Component
public class ProjectDirectoryReadTool extends BaseProjectTool {

    /**
     * 读取 Vue 项目的目录结构
     * <p>
     * 支持读取项目根目录或任意子目录，返回该目录下所有文件和子目录的相对路径列表。
     *
     * @param relativePath 相对于项目根目录的目录路径；传入空字符串或 "." 表示读取项目根目录
     * @param appId        应用 id（由框架自动注入）
     * @return 目录树结构字符串
     */
    @Tool("读取Vue项目的目录结构，列出指定目录下的所有文件和子目录。relativePath 传入空字符串或 '.' 可读取项目根目录。")
    public String readDirectory(
            @P("相对于项目根目录的目录路径，传入空字符串或 '.' 读取根目录，传入 'src' 读取 src 目录") String relativePath,
            @ToolMemoryId Long appId) {

        String projectDir = buildProjectDir(appId);

        // 确定要读取的目标目录
        File targetDir;
        if (relativePath == null || relativePath.isBlank() || ".".equals(relativePath.trim())) {
            // 读取项目根目录
            targetDir = new File(projectDir);
        } else {
            // 路径安全校验，防止读取项目目录之外的内容
            Path resolvedPath = resolveAndValidatePath(appId, relativePath);
            if (resolvedPath == null) {
                return "失败：非法目录路径，禁止访问项目目录之外的内容";
            }
            targetDir = resolvedPath.toFile();
        }

        if (!targetDir.exists()) {
            return "失败：目录不存在 - " + (relativePath == null || relativePath.isBlank() ? "项目根目录" : relativePath);
        }

        if (!targetDir.isDirectory()) {
            return "失败：目标路径是一个文件，请使用文件读取工具";
        }

        try {
            // 递归获取目录下所有文件（包含子目录中的文件）
            List<File> files = FileUtil.loopFiles(targetDir);
            String projectDirStr = new File(projectDir).getAbsolutePath();

            StringBuilder sb = new StringBuilder();
            sb.append("项目目录结构（相对路径）：\n");

            if (files.isEmpty()) {
                sb.append("（目录为空）");
            } else {
                // 将绝对路径转换为相对于项目根目录的路径，便于 AI 理解
                files.stream()
                        .map(f -> f.getAbsolutePath().replace(projectDirStr, "").replace("\\", "/"))
                        .sorted()
                        .forEach(path -> sb.append(path).append("\n"));
            }

            log.info("目录读取成功，appId={}, targetDir={}", appId, targetDir.getAbsolutePath());
            return sb.toString();
        } catch (Exception e) {
            log.error("目录读取失败，appId={}, error={}", appId, e.getMessage(), e);
            return "失败：读取目录时发生错误 - " + e.getMessage();
        }
    }
}
