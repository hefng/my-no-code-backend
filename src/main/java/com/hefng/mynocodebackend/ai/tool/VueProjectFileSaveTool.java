package com.hefng.mynocodebackend.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.constant.AppConstant;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

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
public class VueProjectFileSaveTool {

    /**
     * 当前应用 id，用于隔离不同应用的生成目录
     * 每次构建 AI 服务实例时注入，保证线程安全（每次请求新建实例）
     */
    private final Long appId;

    public VueProjectFileSaveTool(Long appId) {
        this.appId = appId;
    }

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
            @P("文件的完整内容") String content) {

        // 参数基础校验
        if (StrUtil.isBlank(relativePath) || content == null) {
            return "失败：文件路径或内容不能为空";
        }

        // 安全校验：防止路径穿越攻击（../../../etc/passwd 等）
        // 将路径规范化后检查是否仍在项目目录内
        String projectDir = buildProjectDir();
        Path resolvedPath = Paths.get(projectDir).resolve(relativePath).normalize();
        Path projectDirPath = Paths.get(projectDir).normalize();

        // 如果规范化后的路径不以项目目录开头，说明存在路径穿越，直接拒绝
        if (!resolvedPath.startsWith(projectDirPath)) {
            log.warn("检测到路径穿越攻击，appId={}, relativePath={}", appId, relativePath);
            return "失败：非法文件路径，禁止写入项目目录之外的文件";
        }

        try {
            // 确保父目录存在（如 src/components/ 不存在时自动创建）
            FileUtil.mkParentDirs(resolvedPath.toFile());
            FileUtil.writeString(content, resolvedPath.toFile(), StandardCharsets.UTF_8);
            log.info("Vue项目文件写入成功，appId={}, path={}", appId, resolvedPath);
            return "成功：文件已写入 " + relativePath;
        } catch (Exception e) {
            log.error("Vue项目文件写入失败，appId={}, path={}, error={}", appId, relativePath, e.getMessage(), e);
            return "失败：写入文件时发生错误 - " + e.getMessage();
        }
    }

    /**
     * 构建当前应用的项目根目录路径
     * 格式：{CODEGEN_DIR}/vue_project_{appId}
     */
    private String buildProjectDir() {
        String dirPath = AppConstant.CODEGEN_DIR + File.separator
                + CodegenTypeEnum.VUE_PROJECT.getType() + "_" + appId;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }
}
