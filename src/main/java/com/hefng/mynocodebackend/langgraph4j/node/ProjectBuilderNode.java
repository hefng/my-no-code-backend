package com.hefng.mynocodebackend.langgraph4j.node;

import com.hefng.mynocodebackend.ai.AiCodegenServiceFaced;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.core.builder.VueProjectBuilder;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 构建项目节点 - 对生成的代码进行编译打包，输出可部署产物
 */
@Slf4j
public class ProjectBuilderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("[ProjectBuilderNode] 开始构建项目，代码目录: {}", context.getGeneratedCodeDir());

            // 获取生成代码目录和生成类型
            String generatedCodeDir = context.getGeneratedCodeDir();
            CodegenTypeEnum generationType = context.getGenerationType();
            String buildResultDir;
            try {
                if (generationType.getType().equals(CodegenTypeEnum.VUE_PROJECT.getType())) {
                    // 仅当是vue项目时，构建
                    Long appId = context.getAppId();
                    VueProjectBuilder builder = SpringContextUtil.getBean(VueProjectBuilder.class);
                    boolean success = builder.doBuild(appId);
                    if (!success) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败");
                    } else {
                        // 构建成功，返回 dist 目录路径
                        buildResultDir = generatedCodeDir + File.separator + "dist";
                        log.info("Vue 项目构建成功，dist 目录: {}", buildResultDir);
                    }
                } else {
                    // 不是 Vue 项目，暂不支持构建，直接返回生成代码目录
                    buildResultDir = generatedCodeDir;
                    log.info("非 Vue 项目，无需构建，直接使用生成代码目录: {}", buildResultDir);
                }
            } catch (Exception e) {
                // 异常时返回原路径
                log.error("项目构建失败，返回原代码目录: {}, 错误信息: {}", generatedCodeDir, e.getMessage(), e);
                buildResultDir = generatedCodeDir;
            }

            // 更新状态
            context.setCurrentStep("project_builder");
            context.setBuildResultDir(buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
