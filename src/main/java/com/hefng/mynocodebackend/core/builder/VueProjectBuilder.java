package com.hefng.mynocodebackend.core.builder;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Vue 工程化项目构建器
 * <p>
 * 在 AI 生成完 Vue 项目源码后，异步执行 npm i + npm run build，
 * 将源码打包为可部署的静态文件（dist 目录）。
 * <p>
 * 使用虚拟线程（Thread.ofVirtual）执行耗时的 npm 命令，避免阻塞平台线程。
 *
 * @author hefng
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 异步构建 Vue 项目（npm i + npm run build）
     * <p>
     * 构建完成后，dist 目录位于：{CODEGEN_DIR}/vue_project_{appId}/dist/
     *
     * @param appId 应用 id，用于定位项目目录
     * @return CompletableFuture，构建成功时 complete，失败时 completeExceptionally
     */
    public CompletableFuture<Void> buildAsync(Long appId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // 使用虚拟线程执行耗时的 npm 命令，不占用平台线程
        Thread.ofVirtual()
                .name("vue-build-" + appId)
                .start(() -> {
                    try {
                        doBuild(appId);
                        future.complete(null);
                    } catch (Exception e) {
                        log.error("[VueBuilder] 构建失败, appId={}", appId, e);
                        future.completeExceptionally(e);
                    }
                });

        return future;
    }

    /**
     * 同步执行构建流程：npm i → npm run build
     *
     * @param appId 应用 id
     * @throws Exception 任意步骤失败时抛出
     */
    private void doBuild(Long appId) throws Exception {
        String projectDir = buildProjectDir(appId);

        // 校验项目目录是否存在
        File dir = new File(projectDir);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalStateException("Vue 项目目录不存在: " + projectDir);
        }

        log.info("[VueBuilder] 开始构建 Vue 项目, appId={}, dir={}", appId, projectDir);

        // 第一步：npm i（安装依赖）
        runCommand(projectDir, "npm i");

        // 第二步：npm run build（打包）
        runCommand(projectDir, "npm run build");

        log.info("[VueBuilder] Vue 项目构建完成, appId={}, distDir={}/dist", appId, projectDir);
    }

    /**
     * 在指定目录下执行 shell 命令，并将输出打印到日志
     * <p>
     * 兼容 Windows（cmd /c）和 Unix（sh -c）环境。
     *
     * @param workDir 工作目录
     * @param command 要执行的命令
     * @throws Exception 命令执行失败或退出码非 0 时抛出
     */
    private void runCommand(String workDir, String command) throws Exception {
        log.info("[VueBuilder] 执行命令: {} (workDir={})", command, workDir);

        // 根据操作系统选择 shell
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder pb = isWindows
                ? new ProcessBuilder("cmd", "/c", command)
                : new ProcessBuilder("sh", "-c", command);

        pb.directory(new File(workDir));
        // 将 stderr 合并到 stdout，统一读取
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 读取命令输出，避免缓冲区满导致进程阻塞
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[VueBuilder][{}] {}", command, line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("命令执行失败，退出码=" + exitCode + "，命令=" + command);
        }
    }

    /**
     * 构建 Vue 项目的根目录路径
     * 格式：{CODEGEN_DIR}/vue_project_{appId}
     */
    private String buildProjectDir(Long appId) {
        return AppConstant.CODEGEN_DIR + File.separator
                + CodegenTypeEnum.VUE_PROJECT.getType() + "_" + appId;
    }
}
