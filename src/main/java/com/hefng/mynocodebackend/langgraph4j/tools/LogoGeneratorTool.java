package com.hefng.mynocodebackend.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import com.hefng.mynocodebackend.langgraph4j.entity.enums.ImageCategoryEnum;
import com.hefng.mynocodebackend.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Logo 生成工具 - 调用阿里云千问文生图 API 生成 Logo，上传到 COS 并返回访问 URL
 */
@Slf4j
@Component
public class LogoGeneratorTool {

    private static final String LOCAL_OUTPUT_DIR = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "pic";
    private static final String COS_KEY_PREFIX = "logos/";

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.model-name}")
    private String modelName;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 根据提示词生成 Logo 图片，上传到 COS 并返回访问 URL
     *
     * @param prompt Logo 描述提示词（支持中英文）
     * @return 图片的 COS 访问 URL
     */
    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public ImageResource generateLogo(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String prompt) throws ApiException, NoApiKeyException {
        log.info("[LogoGeneratorTool] 开始生成 Logo，模型: {}, 提示词: {}", modelName, prompt);

        // 1. 调用 DashScope 文生图 API（同步调用，SDK 内部自动轮询）
        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey(apiKey)
                .model(modelName)
                .prompt(prompt)
                .n(1)
                .size("1024*1024")
                .build();

        ImageSynthesisResult result = new ImageSynthesis().call(param);

        List<?> results = result.getOutput().getResults();
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("[LogoGeneratorTool] 图片生成失败，results 为空");
        }

        String imageUrl = (String) ((Map<?, ?>) results.get(0)).get("url");
        log.info("[LogoGeneratorTool] 文生图成功，临时 URL: {}", imageUrl);

        // 2. 下载图片到本地临时目录
        FileUtil.mkdir(LOCAL_OUTPUT_DIR);
        String fileId = UUID.randomUUID().toString();
        String localPath = LOCAL_OUTPUT_DIR + File.separator + fileId + ".png";

        try {
            HttpUtil.downloadFile(imageUrl, new File(localPath));
            log.info("[LogoGeneratorTool] 图片下载到本地: {}", localPath);

            // 3. 上传到 COS
            String cosKey = COS_KEY_PREFIX + fileId + ".png";
            cosManager.putPictureObject(cosKey, new File(localPath));
            String cosUrl = cosClientConfig.getHost() + "/" + cosKey;

            log.info("[LogoGeneratorTool] Logo 上传 COS 成功，URL: {}", cosUrl);
            return ImageResource.builder()
                    .category(ImageCategoryEnum.LOGO)
                    .description(prompt)
                    .url(cosUrl)
                    .build();

        } finally {
            // 清理本地临时文件
            FileUtil.del(localPath);
        }
    }
}
