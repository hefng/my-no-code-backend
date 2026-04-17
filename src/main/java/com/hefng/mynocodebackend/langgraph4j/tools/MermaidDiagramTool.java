package com.hefng.mynocodebackend.langgraph4j.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RuntimeUtil;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import com.hefng.mynocodebackend.langgraph4j.entity.enums.ImageCategoryEnum;
import com.hefng.mynocodebackend.manager.CosManager;
import com.mybatisflex.core.util.CollectionUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 架构图绘制工具 - 将 Mermaid 文本代码转换为图片并上传到 COS，返回可访问的 URL
 * 依赖：需要本地安装 Mermaid CLI
 * 安装命令：npm install -g @mermaid-js/mermaid-cli
 */
@Slf4j
@Component
public class MermaidDiagramTool {

    /** 本地临时图片保存目录（绝对路径，避免 CLI 工作目录不一致问题） */
    private static final String LOCAL_OUTPUT_DIR = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "pic";

    /** COS 存储路径前缀 */
    private static final String COS_KEY_PREFIX = "diagrams/";

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 将 Mermaid 文本代码转换为架构图，上传到 COS 并返回可访问的 URL
     *
     * @param mermaidCode Mermaid 格式的绘图代码
     * @return 图片的 COS 访问 URL
     */
    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateDiagram(@P("mermaid代码") String mermaidCode, @P("架构图描述") String description) {
        log.info("[MermaidDiagramTool] 开始生成架构图");

        // 确保输出目录存在
        FileUtil.mkdir(LOCAL_OUTPUT_DIR);

        String fileId = UUID.randomUUID().toString();
        String inputPath = LOCAL_OUTPUT_DIR + File.separator + fileId + ".mmd";
        String outputPath = LOCAL_OUTPUT_DIR + File.separator + fileId + ".png";

        try {
            // 1. 将 Mermaid 代码写入临时 .mmd 文件
            FileUtil.writeString(mermaidCode, inputPath, StandardCharsets.UTF_8);

            // 2. 调用 Mermaid CLI 转换为 PNG
            convertWithMermaidCli(inputPath, outputPath);

            // 3. 上传到 COS，返回访问 URL
            String cosKey = COS_KEY_PREFIX + fileId + ".png";
            cosManager.putPictureObject(cosKey, new File(outputPath));
            String imageUrl = cosClientConfig.getHost() + "/" + cosKey;

            log.info("[MermaidDiagramTool] 架构图生成并上传成功，URL: {}", imageUrl);
            return Collections.singletonList(ImageResource.builder()
                    .category(ImageCategoryEnum.ARCHITECTURE)
                    .description(description)
                    .url(imageUrl)
                    .build());

        } catch (Exception e) {
            log.error("[MermaidDiagramTool] 生成架构图失败", e);
        } finally {
            // 清理本地临时文件
            FileUtil.del(inputPath);
            FileUtil.del(outputPath);
        }
        return new ArrayList<>();
    }

    /**
     * 调用本地 Mermaid CLI（mmdc）执行转换
     * Windows 系统使用 mmdc.cmd，其他系统使用 mmdc
     */
    private void convertWithMermaidCli(String inputPath, String outputPath) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String mmdcCmd = os.contains("win") ? "mmdc.cmd" : "mmdc";

        String command = String.format("%s -i %s -o %s -b transparent", mmdcCmd, inputPath, outputPath);
        log.info("[MermaidDiagramTool] 执行命令: {}", command);

        String result = RuntimeUtil.execForStr(command);
        log.info("[MermaidDiagramTool] mmdc 执行结果: {}", result);

        // 检查输出文件是否生成
        if (!FileUtil.exist(outputPath)) {
            throw new RuntimeException("Mermaid CLI 执行失败，未生成输出文件。命令输出: " + result);
        }
    }
}
