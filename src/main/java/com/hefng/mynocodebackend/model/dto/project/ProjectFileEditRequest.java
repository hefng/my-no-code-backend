package com.hefng.mynocodebackend.model.dto.project;

import lombok.Data;

import java.io.Serializable;

/**
 * 直接编辑项目文件请求（不经过 AI，用于可视化编辑器直接修改）
 */
@Data
public class ProjectFileEditRequest implements Serializable {

    /** 应用 id */
    private Long appId;

    /** 相对于项目根目录的文件路径，如 src/index.html */
    private String relativePath;

    /** 需要被替换的原始内容（精确匹配） */
    private String oldContent;

    /** 替换后的新内容 */
    private String newContent;

    private static final long serialVersionUID = 1L;
}
