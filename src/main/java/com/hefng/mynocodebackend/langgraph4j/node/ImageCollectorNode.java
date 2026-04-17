package com.hefng.mynocodebackend.langgraph4j.node;

import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import com.hefng.mynocodebackend.langgraph4j.entity.enums.ImageCategoryEnum;
import com.hefng.mynocodebackend.langgraph4j.service.AiImageCollectService;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import com.hefng.mynocodebackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Arrays;
import java.util.List;

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

            // 根据 originalPrompt 搜索并收集相关图片资源，填充 imageList 和 imageListStr

            String imageListStr = null;
            try {
                AiImageCollectService aiImageCollectService = SpringContextUtil.getBean(AiImageCollectService.class);
                imageListStr = aiImageCollectService.collectImages(context.getOriginalPrompt());
            } catch (Exception e) {
                log.error("[ImageCollectorNode] 图片收集失败，错误信息: {}", e.getMessage(), e);
            }

            // 更新状态
            context.setCurrentStep("image_collector");
            context.setImageListStr(imageListStr);
            return WorkflowContext.saveContext(context);
        });
    }
}
