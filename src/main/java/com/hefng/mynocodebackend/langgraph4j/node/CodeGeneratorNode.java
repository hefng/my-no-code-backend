package com.hefng.mynocodebackend.langgraph4j.node;

import com.hefng.mynocodebackend.ai.AiCodegenServiceFaced;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 生成代码节点 - 根据路由结果调用对应的代码生成服务
 */
@Slf4j
public class CodeGeneratorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("[CodeGeneratorNode] 开始生成代码，生成类型: {}", context.getGenerationType());

            // 使用增强提示词作为发给 AI 的用户消息
            String userMessage = context.getEnhancedPrompt();
            CodegenTypeEnum generationType = context.getGenerationType();
            // 获取 AI 代码生成外观服务
            AiCodegenServiceFaced codeGeneratorFacade = SpringContextUtil.getBean(AiCodegenServiceFaced.class);
            log.info("开始生成代码，类型: {} ({})", generationType.getType(), generationType.getDescription());
            // 先使用固定的 appId (后续再整合到业务中)
            Long appId = 0L;
            // 调用流式代码生成
            Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeWithStream(userMessage, generationType, appId);
            // 同步等待流式输出完成
            codeStream.blockLast(Duration.ofMinutes(10)); // 最多等待 10 分钟
            // 根据类型设置生成目录
            String generatedCodeDir = String.format("%s%s_%s", AppConstant.CODEGEN_DIR, generationType.getType(), appId);
            log.info("AI 代码生成完成，生成目录: {}", generatedCodeDir);
            context.setGeneratedCodeDir(generatedCodeDir);
            context.setAppId(appId);

            context.setCurrentStep("code_generator");
            log.info("代码生成完成，代码目录: {}", generatedCodeDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
