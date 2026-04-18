package com.hefng.mynocodebackend.langgraph4j;

import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;

@Slf4j
public class WorkFlowApp {

    public static void main(String[] args) throws GraphStateException {
        CompiledGraph<MessagesState<String>> workflow = new CodeGenWorkflow().createWorkflow();

        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt("创建一个鱼皮的个人博客网站")
                .currentStep("init")
                .build();

        log.info("初始输入: {}", initialContext.getOriginalPrompt());
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());

        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                log.info("当前上下文: {}", currentContext);
            }
            stepCounter++;
        }

        log.info("工作流执行完成");
    }
}
