package com.hefng.mynocodebackend.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.io.Serializable;

/**
 * 多文件代码生成结果
 *
 * @author hefng
 */
@Description("多文件代码生成结果")
@Data
public class MultiFileCodeResult implements Serializable {

    /**
     * 生成的 HTML 代码
     */
    @Description("生成的 HTML 代码")
    private String htmlCode;

    /**
     * 生成的 CSS 代码
     */
    @Description("生成的 CSS 代码")
    private String cssCode;

    /**
     * 生成的 JavaScript 代码
     */
    @Description("生成的 JavaScript 代码")
    private String jsCode;

    /**
     * 生成的代码描述信息
     */
    @Description("生成的代码描述信息")
    private String description;

    private static final long serialVersionUID = 1L;
}
