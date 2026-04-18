package com.hefng.mynocodebackend.langgraph4j.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hefng.mynocodebackend.langgraph4j.entity.QualityResult;
import com.hefng.mynocodebackend.langgraph4j.service.CodeQualityCheckService;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Code quality check node.
 */
@Slf4j
public class CodeQualityCheckerNode {

    private static final String RETRY_EXHAUSTED_MESSAGE = "Code quality retries exhausted, workflow terminated";

    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx"
    );

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            context.initCodeQualityRetryConfigIfAbsent();
            log.info("[CodeQualityCheckerNode] Start quality check, step={}, retryCount={}, maxRetries={}",
                    context.getCurrentStep(), context.getCodeQualityRetryCount(), context.getCodeQualityMaxRetries());

            QualityResult qualityResult;
            try {
                String codeContent = readCodeAndConcatenate(context.getGeneratedCodeDir());
                if (StrUtil.isBlank(codeContent)) {
                    log.warn("[CodeQualityCheckerNode] Empty code content, skip quality check");
                    qualityResult = QualityResult.builder()
                            .isValid(false)
                            .errors(List.of("Code content is empty, please verify generation output"))
                            .suggestions(List.of("No suggestion available"))
                            .build();
                } else {
                    log.info("[CodeQualityCheckerNode] Code content length: {}", codeContent.length());
                    CodeQualityCheckService qualityCheckService = SpringContextUtil.getBean(CodeQualityCheckService.class);
                    qualityResult = qualityCheckService.checkCodeQuality(codeContent);
                }
            } catch (Exception e) {
                log.error("[CodeQualityCheckerNode] Quality check failed: {}", e.getMessage(), e);
                qualityResult = QualityResult.builder()
                        .isValid(false)
                        .errors(List.of("Code quality check failed: " + e.getMessage()))
                        .suggestions(List.of("Retry or verify generated code"))
                        .build();
            }

            updateRetryState(context, qualityResult);

            context.setCurrentStep("code_quality_checker");
            context.setQualityResult(qualityResult);
            return WorkflowContext.saveContext(context);
        });
    }

    static void updateRetryState(WorkflowContext context, QualityResult qualityResult) {
        context.initCodeQualityRetryConfigIfAbsent();

        int retryCount = context.getCodeQualityRetryCount();
        int maxRetries = context.getCodeQualityMaxRetries();
        boolean isValid = qualityResult != null && Boolean.TRUE.equals(qualityResult.getIsValid());

        if (isValid) {
            context.setCodeQualityRetryExhausted(false);
            log.info("[CodeQualityCheckerNode] Quality check passed, retryCount={}, maxRetries={}, exhausted={}",
                    retryCount, maxRetries, false);
            return;
        }

        retryCount++;
        boolean exhausted = retryCount >= maxRetries;
        context.setCodeQualityRetryCount(retryCount);
        context.setCodeQualityRetryExhausted(exhausted);

        if (exhausted) {
            context.setErrorMessage(RETRY_EXHAUSTED_MESSAGE);
            List<String> originalErrors = qualityResult == null || qualityResult.getErrors() == null
                    ? Collections.emptyList()
                    : qualityResult.getErrors();
            List<String> updatedErrors = new ArrayList<>(originalErrors);
            if (!updatedErrors.contains(RETRY_EXHAUSTED_MESSAGE)) {
                updatedErrors.add(RETRY_EXHAUSTED_MESSAGE);
            }
            if (qualityResult != null) {
                qualityResult.setErrors(updatedErrors);
            }
        }

        log.info("[CodeQualityCheckerNode] Quality check failed, retryCount={}, maxRetries={}, exhausted={}",
                retryCount, maxRetries, exhausted);
    }

    private static String readCodeAndConcatenate(String generatedCodeDir) {
        if (StrUtil.isBlank(generatedCodeDir)) {
            return "";
        }
        File directory = new File(generatedCodeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("Generated code directory not found or not a directory: {}", generatedCodeDir);
            return "";
        }
        StringBuilder codeContent = new StringBuilder();
        codeContent.append("# Project file tree and source code\n\n");
        FileUtil.walkFiles(directory, file -> {
            if (shouldSkipFile(file, directory)) {
                return;
            }
            if (isCodeFile(file)) {
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeContent.append("## File: ").append(relativePath).append("\n\n");
                String fileContent = FileUtil.readUtf8String(file);
                codeContent.append(fileContent).append("\n\n");
            }
        });
        return codeContent.toString();
    }

    private static boolean shouldSkipFile(File file, File rootDir) {
        String relativePath = FileUtil.subPath(rootDir.getAbsolutePath(), file.getAbsolutePath());
        if (file.getName().startsWith(".")) {
            return true;
        }
        return relativePath.contains("node_modules" + File.separator) ||
                relativePath.contains("dist" + File.separator) ||
                relativePath.contains("target" + File.separator) ||
                relativePath.contains(".git" + File.separator);
    }

    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
