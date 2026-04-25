package com.hefng.mynocodebackend.langgraph4j;

import cn.hutool.json.JSONUtil;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.langgraph4j.enums.WorkflowOperationTypeEnum;
import com.hefng.mynocodebackend.langgraph4j.entity.QualityResult;
import com.hefng.mynocodebackend.langgraph4j.node.*;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.model.enums.SseEventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

@Slf4j
public class CodeGenWorkflowWithFlux {

    private static final String ERROR_EVENT = "workflow_error";
    private static final Map<String, String> WORKFLOW_STEP_TITLES = Map.of(
            "image_collector", "收集图片",
            "prompt_enhancer", "增强提示词",
            "router", "分析生成策略",
            "code_generator", "生成代码",
            "code_quality_checker", "检查代码质量",
            "project_builder", "构建项目"
    );

    static final String BUILD_ROUTE = "need_build";
    static final String SKIP_ROUTE = "skip_build";
    static final String RETRY_ROUTE = "retry_codegen";
    static final String RETRY_EXHAUSTED_ROUTE = "retry_exhausted";
    static final String CREATE_ROUTE = "create";
    static final String MODIFY_ROUTE = "modify";

    /**
     * 创建完整工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        return createWorkflow(null);
    }

    public CompiledGraph<MessagesState<String>> createWorkflow(Consumer<String> buildProgressCallback) {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("operation_router", OperationRouterNode.create())
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", SmartRouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_checker", CodeQualityCheckerNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create(buildProgressCallback))
                    .addEdge(START, "operation_router")
                    .addConditionalEdges("operation_router", buildOperationRouter(), Map.of(
                            CREATE_ROUTE, "image_collector",
                            MODIFY_ROUTE, "router"
                    ))
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

    private AsyncEdgeAction<MessagesState<String>> buildOperationRouter() {
        return state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            return CompletableFuture.completedFuture(resolveOperationRoute(context));
        };
    }

    String resolveOperationRoute(WorkflowContext context) {
        if (context == null || context.getOperationType() == null) {
            return CREATE_ROUTE;
        }
        return context.getOperationType() == WorkflowOperationTypeEnum.MODIFY ? MODIFY_ROUTE : CREATE_ROUTE;
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
     * 执行工作流（流式）
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt,
                                                Long appId,
                                                WorkflowOperationTypeEnum operationType,
                                                CodegenTypeEnum generationType) {
        return Flux.<String>create(sink -> Thread.startVirtualThread(() -> {
            Consumer<String> buildProgressCallback = line -> {
                if (!sink.isCancelled()) {
                    sink.next(buildEventOutput(SseEventTypeEnum.BUILD_LOG.getValue(), line));
                }
            };
            CompiledGraph<MessagesState<String>> workflow = createWorkflow(buildProgressCallback);
            WorkflowContext initialContext = WorkflowContext.builder()
                    .appId(appId)
                    .originalPrompt(originalPrompt)
                    .operationType(operationType)
                    .generationType(generationType)
                    .currentStep("init")
                    .build();

            GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
            log.info("工作流图:\n{}", graph.content());
            log.info("开始执行代码生成工作流");

            try {
                int nodeCounter = 1;
                int workflowStepCounter = 1;
                for (NodeOutput<MessagesState<String>> step : workflow.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                    if (sink.isCancelled()) {
                        log.info("工作流流式输出已取消");
                        return;
                    }
                    WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                    log.info("--- 节点 {} 完成，节点：{} ---", nodeCounter, step.node());
                    if (currentContext != null) {
                        log.info("当前上下文: {}", currentContext);
                    }
                    String stepOutput = buildStepOutput(workflowStepCounter, step);
                    if (stepOutput != null) {
                        sink.next(stepOutput);
                        workflowStepCounter++;
                    }
                    nodeCounter++;
                }
                log.info("代码生成工作流执行完成");
                sink.next(buildEventOutput(SseEventTypeEnum.DONE.getValue(), ""));
                sink.complete();
            } catch (Exception e) {
                log.error("代码生成工作流执行失败", e);
                String errorMessage = e.getMessage() == null ? "工作流执行失败" : e.getMessage();
                sink.next(buildEventOutput(ERROR_EVENT, errorMessage));
                sink.next(buildEventOutput(SseEventTypeEnum.DONE.getValue(), ""));
                sink.complete();
            }
        })).subscribeOn(Schedulers.boundedElastic());
    }

    private String buildStepOutput(int stepCounter,
                                   NodeOutput<MessagesState<String>> step) {
        String title = WORKFLOW_STEP_TITLES.get(step.node());
        if (title == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("step", stepCounter);
        payload.put("node", step.node());
        payload.put("title", title);
        payload.put("status", "completed");
        return buildEventOutput(SseEventTypeEnum.WORKFLOW_STEP.getValue(), payload);
    }

    private String buildEventOutput(String event, Object data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event);
        payload.put("d", data);
        return JSONUtil.toJsonStr(payload);
    }
}
