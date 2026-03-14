package com.hefng.mynocodebackend.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.exception.BusinessException;

import java.io.File;

/**
 * 代码文件保存器模板类, 定义了保存代码文件的基本流程和接口
 *
 * @author hefng
 */
public abstract class CodeFileSaverTemplate<T> {

    // 默认保存路径, 项目根目录下的 tmp 文件夹 D:\develop\workspace\my-no-code-backend\tmp\
    protected static final String DEFAULT_SAVE_PATH = System.getProperty("user.dir") + "\\tmp\\";

    /**
     * 保存代码文件的模板方法, 定义了保存代码文件的基本流程
     * @param result 代码生成结果对象, 包含需要保存的代码内容
     * @return 保存后的文件对象
     */
    public File saveCodeFile(T result) {
        // 校验
        validResult(result);
        // 获取唯一路径
        String uniquePath = buildUniqueFilePath();
        // 写入文件 返回文件对象
        return writeToFile(uniquePath, result);
    }

    /**
     * 保存代码到文件, 由子类实现具体的保存逻辑, 如保存单文件或多文件等
     * @param uniquePath 文件夹路径
     * @param content 代码文件内容
     */
    protected abstract File writeToFile(String uniquePath, T content);

    /**
     * 获取代码类型, 由子类实现返回具体的代码类型字符串, 如 "html", "multi-file" 等
     */
    protected abstract String getType();

    /**
     * 校验代码生成结果对象是否合法
     */
    private void validResult(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成结果不能为空");
        }
    }

    /**
     * 构建唯一的文件路径
     * @return
     */
    private String buildUniqueFilePath() {
        String uniqueFileName = getType() + "_" + IdUtil.getSnowflakeNextIdStr();
        String uniqueFilePath = DEFAULT_SAVE_PATH + "codegen" + File.separator + uniqueFileName;
        FileUtil.mkdir(uniqueFilePath);
        return uniqueFilePath;
    }

}
