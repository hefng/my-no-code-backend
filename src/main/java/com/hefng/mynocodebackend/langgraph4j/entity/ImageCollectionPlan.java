package com.hefng.mynocodebackend.langgraph4j.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 图片收集计划实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageCollectionPlan implements Serializable {

    /**
     * 内容图片任务列表
     */
    @Description("内容图片任务列表，每项包含用于搜索图片的关键词 query")
    private List<ContentImageTask> contentImageTasks;

    /**
     * 图表任务列表
     */
    @Description("图表任务列表，每项包含 mermaidCode 和图表用途描述 description")
    private List<DiagramTask> diagramTasks;

    /**
     * Logo 任务列表
     */
    @Description("Logo 任务列表，每项包含 Logo 设计描述 description")
    private List<LogoTask> logoTasks;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 内容图片任务
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentImageTask implements Serializable {

        /**
         * 搜索关键词
         */
        @Description("搜索关键词")
        private String query;

        @Serial
        private static final long serialVersionUID = 1L;
    }

    /**
     * 图表任务
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagramTask implements Serializable {

        /**
         * mermaid 图表代码
         */
        @Description("mermaid 图表代码")
        private String mermaidCode;

        /**
         * 图表用途描述
         */
        @Description("图表用途描述")
        private String description;

        @Serial
        private static final long serialVersionUID = 1L;
    }

    /**
     * Logo 任务
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogoTask implements Serializable {

        /**
         * Logo 设计描述
         */
        @Description("Logo设计描述，如名称、行业、风格等")
        private String description;

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
