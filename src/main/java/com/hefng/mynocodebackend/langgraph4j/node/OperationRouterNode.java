package com.hefng.mynocodebackend.langgraph4j.node;

import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 操作类型路由节点
 */
public class OperationRouterNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            context.setCurrentStep("operation_router");
            return WorkflowContext.saveContext(context);
        });
    }
}