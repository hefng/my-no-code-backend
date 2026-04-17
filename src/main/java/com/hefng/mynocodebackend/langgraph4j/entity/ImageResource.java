package com.hefng.mynocodebackend.langgraph4j.entity;

import com.hefng.mynocodebackend.langgraph4j.entity.enums.ImageCategoryEnum;
import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片资源对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResource implements Serializable {

    /**
     * 图片类别
     */
    @Description("图片类别，枚举值：CONTENT（内容图片）、LOGO（Logo图片）、ARCHITECTURE（架构图片）")
    private ImageCategoryEnum category;

    /**
     * 图片描述
     */
    @Description("图片的简短描述，说明图片内容或用途")
    private String description;

    /**
     * 图片地址
     */
    @Description("图片的完整访问 URL，必须以 http 或 https 开头")
    private String url;

    @Serial
    private static final long serialVersionUID = 1L;
}