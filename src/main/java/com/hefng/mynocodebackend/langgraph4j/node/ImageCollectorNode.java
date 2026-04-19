package com.hefng.mynocodebackend.langgraph4j.node;

import cn.hutool.core.date.StopWatch;
import cn.hutool.json.JSONUtil;
import com.hefng.mynocodebackend.langgraph4j.entity.ImageCollectionPlan;
import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import com.hefng.mynocodebackend.langgraph4j.service.ImageCollectionPlanService;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.langgraph4j.tools.LogoGeneratorTool;
import com.hefng.mynocodebackend.langgraph4j.tools.MermaidDiagramTool;
import com.hefng.mynocodebackend.langgraph4j.tools.PexelsImageSearchTool;
import com.hefng.mynocodebackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点 - 根据用户提示词收集相关图片资源
 */
@Slf4j
public class ImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("[ImageCollectorNode] 开始收集图片资源，原始提示词: {}", context.getOriginalPrompt());

            List<ImageResource> allImageResources = new ArrayList<>();
//            StopWatch stopWatch = StopWatch.create("ImageCollectorNode");
//            stopWatch.start();
            try {
                // 1. 先生成图片收集计划
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(context.getOriginalPrompt());
                if (plan == null) {
                    log.warn("[ImageCollectorNode] 图片收集计划为空，跳过图片收集");
                } else {
                    log.info("[ImageCollectorNode] 图片收集计划生成成功，contentTasks={}, diagramTasks={}, logoTasks={}",
                            safeSize(plan.getContentImageTasks()),
                            safeSize(plan.getDiagramTasks()),
                            safeSize(plan.getLogoTasks()));

                    // 2. 从计划中拆出任务，并发调用 tools
                    PexelsImageSearchTool pexelsImageSearchTool = SpringContextUtil.getBean(PexelsImageSearchTool.class);
                    MermaidDiagramTool mermaidDiagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    LogoGeneratorTool logoGeneratorTool = SpringContextUtil.getBean(LogoGeneratorTool.class);

                    List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();

                    for (ImageCollectionPlan.ContentImageTask task : safeList(plan.getContentImageTasks())) {
                        futures.add(CompletableFuture.supplyAsync(() -> collectContentImages(pexelsImageSearchTool, task)));
                    }
                    for (ImageCollectionPlan.DiagramTask task : safeList(plan.getDiagramTasks())) {
                        futures.add(CompletableFuture.supplyAsync(() -> collectDiagramImages(mermaidDiagramTool, task)));
                    }
                    for (ImageCollectionPlan.LogoTask task : safeList(plan.getLogoTasks())) {
                        futures.add(CompletableFuture.supplyAsync(() -> collectLogoImages(logoGeneratorTool, task)));
                    }

                    // 3. 聚合并发结果
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    allImageResources = futures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());
                    log.info("[ImageCollectorNode] 图片收集完成，共 {} 张", allImageResources.size());
                }
            } catch (Exception e) {
                log.error("[ImageCollectorNode] 图片收集失败，错误信息: {}", e.getMessage(), e);
            }
//            stopWatch.stop();
//            System.out.println(stopWatch.prettyPrint(TimeUnit.SECONDS));
            // 4. 更新状态并返回
            context.setCurrentStep("image_collector");
            context.setImageList(allImageResources);
            context.setImageListStr(JSONUtil.toJsonStr(allImageResources));
            return WorkflowContext.saveContext(context);
        });
    }

    private static List<ImageResource> collectContentImages(PexelsImageSearchTool pexelsImageSearchTool,
                                                            ImageCollectionPlan.ContentImageTask task) {
        if (task == null || task.getQuery() == null || task.getQuery().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return safeList(pexelsImageSearchTool.searchImages(task.getQuery()));
        } catch (Exception e) {
            log.warn("[ImageCollectorNode] 内容图片收集失败，query={}, error={}", task.getQuery(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<ImageResource> collectDiagramImages(MermaidDiagramTool mermaidDiagramTool,
                                                            ImageCollectionPlan.DiagramTask task) {
        if (task == null || task.getMermaidCode() == null || task.getMermaidCode().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return safeList(mermaidDiagramTool.generateDiagram(task.getMermaidCode(), task.getDescription()));
        } catch (Exception e) {
            log.warn("[ImageCollectorNode] 图表图片收集失败，description={}, error={}", task.getDescription(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<ImageResource> collectLogoImages(LogoGeneratorTool logoGeneratorTool,
                                                         ImageCollectionPlan.LogoTask task) {
        if (task == null || task.getDescription() == null || task.getDescription().isBlank()) {
            return Collections.emptyList();
        }
        try {
            ImageResource logo = logoGeneratorTool.generateLogo(task.getDescription());
            return logo == null ? Collections.emptyList() : Collections.singletonList(logo);
        } catch (Exception e) {
            log.warn("[ImageCollectorNode] Logo 图片收集失败，description={}, error={}", task.getDescription(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
