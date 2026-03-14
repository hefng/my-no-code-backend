package com.hefng.mynocodebackend.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import opennlp.tools.util.StringUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存工具类
 *
 * @author hefng
 */
@Deprecated
public class CodeFileSaver {

    // 默认保存路径, 项目根目录下的 tmp 文件夹 D:\develop\workspace\my-no-code-backend\tmp\
    private static final String DEFAULT_SAVE_PATH = System.getProperty("user.dir") + "\\tmp\\";

    /**
     * 保存 HTML 代码到文件
     * @param htmlCode
     * @return
     */
    public static File saveHtmlCodeToFile(String htmlCode) {
        if (StringUtil.isEmpty(htmlCode)) {
            return null;
        }
        String uniqueFilePath = buildUniqueFilePath(CodegenTypeEnum.HTML.getType());
        writeToFile(uniqueFilePath, htmlCode, "index.html");
        return new File(uniqueFilePath + File.separator + "index.html");
    }

    /**
     * 保存多文件代码到文件夹
     * @param htmlCode
     * @param cssCode
     * @param jsCode
     * @return
     */
    public static File saveMultiFileCodeToFile(String htmlCode, String cssCode, String jsCode) {
        String uniqueFilePath = buildUniqueFilePath(CodegenTypeEnum.MULTI_FILE.getType());
        if (!StringUtil.isEmpty(htmlCode)) {
            writeToFile(uniqueFilePath, htmlCode, "index.html");
        }
        if (!StringUtil.isEmpty(cssCode)) {
            writeToFile(uniqueFilePath, cssCode, "style.css");
        }
        if (!StringUtil.isEmpty(jsCode)) {
            writeToFile(uniqueFilePath, jsCode, "script.js");
        }
        return new File(uniqueFilePath);
    }

    /**
     * 构建唯一的文件路径
     * @param type
     * @return
     */
    private static String buildUniqueFilePath(String type) {
        String uniqueFileName = type + "_" + IdUtil.getSnowflakeNextIdStr();
        String uniqueFilePath = DEFAULT_SAVE_PATH + "codegen" + File.separator + uniqueFileName;
        FileUtil.mkdir(uniqueFilePath);
        return uniqueFilePath;
    }

    /**
     * 保存代码到文件
     * @param dirPath 文件夹路径
     * @param content 代码文件内容
     * @param fileName 文件名称
     */
    private static void writeToFile(String dirPath, String content, String fileName) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }

}
