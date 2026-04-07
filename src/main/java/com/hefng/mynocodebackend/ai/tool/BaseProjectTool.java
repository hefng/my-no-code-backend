package com.hefng.mynocodebackend.ai.tool;

import cn.hutool.core.io.FileUtil;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AI 工具基类
 * <p>
 * 所有暴露给 AI 调用的工具类都应继承此类，并使用 @Component 注册为 Spring Bean。
 * 提供公共的路径构建与安全校验能力，避免各工具重复实现。
 *
 * @author hefng
 */
@Slf4j
public abstract class BaseProjectTool {

    /**
     * 构建当前应用的 Vue 项目根目录路径，目录不存在时自动创建
     *
     * @param appId 应用 id
     * @return 项目根目录绝对路径字符串
     */
    protected String buildProjectDir(Long appId) {
        String dirPath = AppConstant.CODEGEN_DIR + File.separator
                + CodegenTypeEnum.VUE_PROJECT.getType() + "_" + appId;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 将相对路径解析为项目内的绝对路径，并进行路径穿越安全校验
     * <p>
     * 若路径包含 "../" 等穿越片段，解析后会超出项目目录，此时返回 null 表示非法路径。
     *
     * @param appId        应用 id
     * @param relativePath 相对于项目根目录的文件路径，如 "src/App.vue"
     * @return 合法的绝对路径；若路径非法则返回 null
     */
    protected Path resolveAndValidatePath(Long appId, String relativePath) {
        String projectDir = buildProjectDir(appId);
        Path projectDirPath = Paths.get(projectDir).normalize();
        Path resolvedPath = projectDirPath.resolve(relativePath).normalize();

        // 规范化后的路径必须以项目目录开头，否则存在路径穿越风险
        if (!resolvedPath.startsWith(projectDirPath)) {
            log.warn("检测到路径穿越攻击，appId={}, relativePath={}", appId, relativePath);
            return null;
        }
        return resolvedPath;
    }
}
