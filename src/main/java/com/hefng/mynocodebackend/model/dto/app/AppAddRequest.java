package com.hefng.mynocodebackend.model.dto.app;

import java.io.Serializable;
import lombok.Data;

/**
 * 应用创建请求
 *
 * @author https://github.com/hefng
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用描述
     */
    private String appDesc;

    /**
     * 应用封面
     */
    private String appCover;

    /**
     * 代码生成类型
     */
    private String codegenType;

    private static final long serialVersionUID = 1L;
}
