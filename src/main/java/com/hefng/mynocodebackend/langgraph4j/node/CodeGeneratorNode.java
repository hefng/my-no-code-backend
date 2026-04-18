package com.hefng.mynocodebackend.langgraph4j.node;

import com.hefng.mynocodebackend.ai.AiCodegenServiceFaced;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.langgraph4j.entity.QualityResult;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成节点 - 根据路由结果调用对应的代码生成服务
 */
@Slf4j
public class CodeGeneratorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            CodegenTypeEnum generationType = context.getGenerationType();
            String userMessage = buildUserMessage(context);
            log.info("[CodeGeneratorNode] 开始生成代码，类型: {}", generationType);

            AiCodegenServiceFaced codeGeneratorFacade = SpringContextUtil.getBean(AiCodegenServiceFaced.class);

            // 先使用固定 appId，后续可替换为真实业务 appId
            Long appId = 0L;
            Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeWithStream(userMessage, generationType, appId);
            codeStream.blockLast(Duration.ofMinutes(10));

            String generatedCodeDir = String.format("%s%s_%s", AppConstant.CODEGEN_DIR, generationType.getType(), appId);
            context.setGeneratedCodeDir(generatedCodeDir);
            // 默认构建产物目录就是生成目录，进入 project_builder 后会被 dist 覆盖
            context.setBuildResultDir(generatedCodeDir);
            context.setAppId(appId);
            context.setCurrentStep("code_generator");

            log.info("[CodeGeneratorNode] 代码生成完成，目录: {}", generatedCodeDir);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 构建用户提示信息，包含原始提示词、增强后的提示词、图片资源、错误信息（如果有）等
     * @param context
     * @return
     */
    private static String buildUserMessage(WorkflowContext context) {
        QualityResult qualityResult = context.getQualityResult();
        String enhancedPrompt = context.getEnhancedPrompt();
        if (checkFailed(qualityResult)) {
            log.warn("[CodeGeneratorNode] 质量检查未通过，错误信息: {}, 建议: {}",
                    qualityResult.getErrors(), qualityResult.getSuggestions());
            return buildFailedMessage(qualityResult);
        }
        return enhancedPrompt;
    }

    /**
     * 构建质量检查未通过时的提示信息，包含错误信息和建议
     * @param qualityResult
     * @return
     */
    private static String buildFailedMessage(QualityResult qualityResult) {
        StringBuilder message = new StringBuilder();
        message.append("## 代码质量检查未通过\n\n");
        message.append("### 错误信息\n");
        qualityResult.getErrors().forEach(error -> message.append("- ").append(error).append("\n"));
        message.append("\n");
        message.append("### 修复建议\n");
        if (qualityResult.getSuggestions() == null || qualityResult.getSuggestions().isEmpty()) {
            message.append("- 请根据错误信息修复后重新生成代码。\n");
        } else {
            qualityResult.getSuggestions().forEach(suggestion -> message.append("- ").append(suggestion).append("\n"));
        }
        return message.toString();
    }

    private static boolean checkFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }
}
