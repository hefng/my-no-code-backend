package com.hefng.mynocodebackend.ai;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;
import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.core.parser.CodeParserExecutor;
import com.hefng.mynocodebackend.core.saver.CodeFileSaverExecutor;
import com.hefng.mynocodebackend.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * ai 代码生成服务门面类
 *
 * @author hefng
 */
@Service
@Slf4j
public class AiCodegenServiceFaced {

    @Resource
    private AiCodegenService aiCodegenService;

    /**
     * 根据用户输入的需求生成代码并保存到文件
     * @param userMessage 用户输入的需求描述
     * @param codegenTypeEnum 代码类型，例如 "html" 或 "multi"（表示生成多文件代码）
     * @return
     */
    public File generateAndSaveCode(String userMessage, CodegenTypeEnum codegenTypeEnum, Long appId) {
        if (codegenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        return switch (codegenTypeEnum) {
            case HTML -> {
                HTMLCodeResult htmlCodeResult = aiCodegenService.generateHtml(userMessage);
                yield CodeFileSaverExecutor.saveCodeFile(htmlCodeResult, CodegenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodegenService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.saveCodeFile(multiFileCodeResult, CodegenTypeEnum.MULTI_FILE, appId);
            }
        };
    }

    /**
     * 根据用户输入的需求生成代码并保存到文件(流式输出)
     * @param userMessage 用户输入的需求描述
     * @param codegenTypeEnum 代码类型，例如 "html" 或 "multi"（表示生成多文件代码）
     * @return
     */
    public Flux<String> generateAndSaveCodeWithStream(String userMessage, CodegenTypeEnum codegenTypeEnum, Long appId) {
        if (codegenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        return switch (codegenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodegenService.generateHtmlStream(userMessage);
                yield processCodeStream(result, CodegenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodegenService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(result, CodegenTypeEnum.MULTI_FILE, appId);
            }
        };
    }

    /**
     * 处理流式输出的代码生成结果，解析并保存到文件
     *
     * @param result 流式输出的代码生成结果
     * @param codegenTypeEnum 代码类型，例如 "html" 或 "multi_file"（表示生成多文件代码）
     * @return
     */
    public Flux<String> processCodeStream(Flux<String> result, CodegenTypeEnum codegenTypeEnum, Long appId) {
        // 由于是流式输出，我们需要在流完成时对接收到的代码进行解析和保存，因此我们使用 doOnNext 和 doOnComplete 来处理流中的数据
        StringBuilder multiFileCodeBuilder = new StringBuilder();
        return result.doOnNext(multiFileCodeBuilder::append).doOnComplete(() -> {
            try {
                // 解析 multiFileCodeBuilder 中的代码，提取 HTML、CSS、JS 代码
                String multiFileCode = multiFileCodeBuilder.toString();
                CodeParserExecutor.parseCode(multiFileCode, codegenTypeEnum);
                // 保存代码到文件
                File file = CodeFileSaverExecutor.saveCodeFile(multiFileCode, codegenTypeEnum, appId);
                log.info("多文件代码生成并保存完成，保存路径: {}", file.getAbsolutePath());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "多文件代码生成失败: " + e.getMessage());
            }
        });
    }
}
