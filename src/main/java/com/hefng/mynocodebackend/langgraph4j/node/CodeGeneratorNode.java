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
 * 代码生成节点 - 根据路由结果调用对应的代码生成服务
 */
@Slf4j
public class CodeGeneratorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            CodegenTypeEnum generationType = context.getGenerationType();
            String userMessage = context.getEnhancedPrompt();
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
}
