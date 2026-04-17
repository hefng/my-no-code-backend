package com.hefng.mynocodebackend.langgraph4j.node;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.service.AiCodeGenTypeRoutingService;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 智能路由节点 - 根据增强后的提示词判断代码生成类型
 */
@Slf4j
public class SmartRouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String enhancedPrompt = context.getEnhancedPrompt();
            log.info("[SmartRouterNode] 开始智能路由，增强提示词: {}", enhancedPrompt);
            // enhancedPrompt，判断应使用哪种代码生成策略，设置 generationType
            CodegenTypeEnum generationType;
            try {
                // 获取AI路由服务
                AiCodeGenTypeRoutingService routingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);
                // 根据原始提示词进行智能路由
                generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getType(), generationType.getDescription());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodegenTypeEnum.HTML;
            }
            // 更新状态
            context.setCurrentStep("smart_router");
            context.setGenerationType(generationType);
            log.info("路由决策完成，选择类型: {}", generationType.getType());
            return WorkflowContext.saveContext(context);
        });
    }
}
