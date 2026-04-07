package com.hefng.mynocodebackend.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Vue 项目文件编辑工具
 * <p>
 * 与 {@link ProjectFileSaveTool} 的区别：
 * 本工具支持对文件中的指定代码片段进行精准替换，无需重写整个文件，
 * 适用于 AI 对已有文件进行局部修改的场景，大幅减少 token 消耗。
 *
 * @author hefng
 */
@Slf4j
@Component
public class ProjectFileEditTool extends BaseProjectTool {

    /**
     * 将文件中的指定旧代码替换为新代码
     * <p>
     * 使用字符串精确匹配，oldContent 必须与文件中的内容完全一致（包括空格和换行）。
     * 若文件中存在多处相同的 oldContent，则全部替换。
     *
     * @param relativePath 相对于项目根目录的文件路径，如 "src/App.vue"
     * @param oldContent   需要被替换的原始代码片段（必须与文件内容完全匹配）
     * @param newContent   替换后的新代码片段
     * @param appId        应用 id（由框架自动注入，无需 AI 传入）
     * @return 操作结果描述
     */
    @Tool("编辑Vue项目中指定文件的代码。将文件中的 oldContent 替换为 newContent，只需传入需要修改的代码片段，无需传入整个文件内容。")
    public String editFile(
            @P("相对于项目根目录的文件路径，例如：src/App.vue、src/components/Header.vue") String relativePath,
            @P("需要被替换的原始代码片段，必须与文件中的内容完全一致") String oldContent,
            @P("替换后的新代码片段") String newContent,
            @ToolMemoryId Long appId) {

        // 参数基础校验
        if (StrUtil.isBlank(relativePath) || oldContent == null || newContent == null) {
            return "失败：文件路径、原始内容和新内容均不能为空";
        }

        // 路径安全校验
        Path resolvedPath = resolveAndValidatePath(appId, relativePath);
        if (resolvedPath == null) {
            return "失败：非法文件路径，禁止访问项目目录之外的文件";
        }

        // 检查文件是否存在
        if (!resolvedPath.toFile().exists()) {
            return "失败：文件不存在 - " + relativePath;
        }

        try {
            String fileContent = FileUtil.readString(resolvedPath.toFile(), StandardCharsets.UTF_8);

            // 检查 oldContent 是否存在于文件中
            if (!fileContent.contains(oldContent)) {
                return "失败：在文件中未找到指定的原始代码片段，请确认内容是否与文件完全一致（包括空格和换行）";
            }

            // 执行替换（replace 会替换所有匹配项）
            String updatedContent = fileContent.replace(oldContent, newContent);
            FileUtil.writeString(updatedContent, resolvedPath.toFile(), StandardCharsets.UTF_8);

            log.info("文件编辑成功，appId={}, path={}", appId, resolvedPath);
            return "文件修改成功" + relativePath;
        } catch (Exception e) {
            log.error("文件编辑失败，appId={}, path={}, error={}", appId, relativePath, e.getMessage(), e);
            return "失败：编辑文件时发生错误 - " + e.getMessage();
        }
    }

    @Override
    protected String getToolName() {
        return "fileEditTool";
    }

    @Override
    protected String getToolDescription() {
        return "文件编辑";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativePath = arguments.getStr("relativePath");
        String oldContent = arguments.getStr("oldContent");
        String newContent = arguments.getStr("newContent");
        return String.format("""
                    [文件编辑] %s
                    编辑前:
                    ```%s
                    %s
                    ```
                    编辑后:
                    ```%s
                    %s
                    ```
                    """, relativePath, FileUtil.getSuffix(relativePath), oldContent, FileUtil.getSuffix(relativePath), newContent);
    }
}
