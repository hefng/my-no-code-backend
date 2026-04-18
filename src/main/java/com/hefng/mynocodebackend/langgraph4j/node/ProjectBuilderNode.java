package com.hefng.mynocodebackend.langgraph4j.node;

import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.core.builder.VueProjectBuilder;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 构建项目节点 - 对生成的代码进行编译打包，输出可部署产物
 */
@Slf4j
public class ProjectBuilderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String generatedCodeDir = context.getGeneratedCodeDir();
            String buildResultDir = generatedCodeDir;
            log.info("[ProjectBuilderNode] 开始构建项目，代码目录: {}", generatedCodeDir);

            try {
                // 此节点仅由条件边在 vue-project 类型下进入
                Long appId = context.getAppId();
                VueProjectBuilder builder = SpringContextUtil.getBean(VueProjectBuilder.class);
                boolean success = builder.doBuild(appId);
                if (!success) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败");
                }
                buildResultDir = generatedCodeDir + File.separator + "dist";
                log.info("[ProjectBuilderNode] Vue 项目构建成功，dist 目录: {}", buildResultDir);
            } catch (Exception e) {
                log.error("[ProjectBuilderNode] 项目构建失败，回退原目录: {}, 错误: {}", generatedCodeDir, e.getMessage(), e);
            }

            context.setCurrentStep("project_builder");
            context.setBuildResultDir(buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
