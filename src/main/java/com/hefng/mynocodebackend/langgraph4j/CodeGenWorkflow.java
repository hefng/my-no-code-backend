package com.hefng.mynocodebackend.langgraph4j;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.langgraph4j.entity.QualityResult;
import com.hefng.mynocodebackend.langgraph4j.node.CodeGeneratorNode;
import com.hefng.mynocodebackend.langgraph4j.node.CodeQualityCheckerNode;
import com.hefng.mynocodebackend.langgraph4j.node.ImageCollectorNode;
import com.hefng.mynocodebackend.langgraph4j.node.ProjectBuilderNode;
import com.hefng.mynocodebackend.langgraph4j.node.PromptEnhancerNode;
import com.hefng.mynocodebackend.langgraph4j.node.SmartRouterNode;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

@Slf4j
public class CodeGenWorkflow {

    static final String BUILD_ROUTE = "need_build";
    static final String SKIP_ROUTE = "skip_build";
    static final String RETRY_ROUTE = "retry_codegen";
    static final String RETRY_EXHAUSTED_ROUTE = "retry_exhausted";

    /**
     * 创建完整工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", SmartRouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_checker", CodeQualityCheckerNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_checker")
                    .addConditionalEdges("code_quality_checker", buildRouter(), Map.of(
                            BUILD_ROUTE, "project_builder",
                            SKIP_ROUTE, END,
                            RETRY_ROUTE, "code_generator",
                            RETRY_EXHAUSTED_ROUTE, END
                    ))
                    .addEdge("project_builder", END)
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    private AsyncEdgeAction<MessagesState<String>> buildRouter() {
        return state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            return CompletableFuture.completedFuture(resolveCodeQualityRoute(context));
        };
    }

    String resolveCodeQualityRoute(WorkflowContext context) {
        if (context == null) {
            return SKIP_ROUTE;
        }
        context.initCodeQualityRetryConfigIfAbsent();

        int retryCount = context.getCodeQualityRetryCount();
        int maxRetries = context.getCodeQualityMaxRetries();
        boolean exhausted = Boolean.TRUE.equals(context.getCodeQualityRetryExhausted()) || retryCount >= maxRetries;

        QualityResult qualityResult = context.getQualityResult();
        boolean qualityPassed = qualityResult != null && Boolean.TRUE.equals(qualityResult.getIsValid());

        if (qualityPassed) {
            CodegenTypeEnum generationType = context.getGenerationType();
            return generationType == CodegenTypeEnum.VUE_PROJECT ? BUILD_ROUTE : SKIP_ROUTE;
        }

        if (exhausted) {
            return RETRY_EXHAUSTED_ROUTE;
        }
        return RETRY_ROUTE;
    }

    /**
     * 执行工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("init")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前上下文: {}", currentContext);
            }
            stepCounter++;
        }

        log.info("代码生成工作流执行完成");
        return finalContext;
    }
}
