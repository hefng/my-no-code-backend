package com.hefng.mynocodebackend.langgraph4j.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import com.hefng.mynocodebackend.langgraph4j.entity.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片搜集工具 - 调用 Pexels API 搜索图片，每次固定返回 12 张图片 URL
 */
@Slf4j
@Component
public class PexelsImageSearchTool {

    private static final String PEXELS_SEARCH_URL = "https://api.pexels.com/v1/search";
    private static final int PER_PAGE = 12;

    @Value("${pexels.api-key}")
    private String apiKey;

    /**
     * 根据关键词搜索图片，返回图片 URL 列表（最多 12 张）
     *
     * @param query 搜索关键词
     * @return 图片 URL 列表
     */
    @Tool("搜索内容相关的图片，用于网站内容展示")
    public List<ImageResource> searchImages(@P("搜索关键词") String query) {
        log.info("[PexelsImageSearchTool] 搜索图片，关键词: {}", query);

        HttpResponse response = HttpRequest.get(PEXELS_SEARCH_URL)
                .header("Authorization", apiKey)
                .form("query", query)
                .form("per_page", PER_PAGE)
                .form("page", 1)
                .execute();

        if (!response.isOk()) {
            log.error("[PexelsImageSearchTool] 请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
            throw new RuntimeException("Pexels API 请求失败，状态码: " + response.getStatus());
        }

        return parseImageResources(response.body(), query);
    }

    private List<ImageResource> parseImageResources(String responseBody, String query) {
        List<ImageResource> resources = new ArrayList<>();
        JSONObject json = JSONUtil.parseObj(responseBody);
        JSONArray photos = json.getJSONArray("photos");

        if (photos == null || photos.isEmpty()) {
            log.warn("[PexelsImageSearchTool] 未找到任何图片");
            return resources;
        }

        for (int i = 0; i < photos.size(); i++) {
            JSONObject photo = photos.getJSONObject(i);
            JSONObject src = photo.getJSONObject("src");
            if (src != null) {
                String url = src.getStr("large2x");
                if (url == null) {
                    url = src.getStr("original");
                }
                if (url != null) {
                    resources.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description(query)
                            .url(url)
                            .build());
                }
            }
        }

        log.info("[PexelsImageSearchTool] 共获取到 {} 张图片", resources.size());
        return resources;
    }
}
