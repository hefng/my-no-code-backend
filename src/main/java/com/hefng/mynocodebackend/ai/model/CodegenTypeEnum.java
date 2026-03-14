package com.hefng.mynocodebackend.ai.model;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 代码生成类型枚举
 *
 * @author hefng
 */
@Getter
public enum CodegenTypeEnum {


    HTML("html", "html代码生成"),
    MULTI_FILE("multi_file", "管理员用户");

    private final String type;
    private final String description;

    CodegenTypeEnum(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public static CodegenTypeEnum getByType(String type) {
        if (StrUtil.isBlank(type)) {
            return null;
        }
        for (CodegenTypeEnum codegenTypeEnum : values()) {
            if (codegenTypeEnum.getType().equals(type)) {
                return codegenTypeEnum;
            }
        }
        return null;
    }

}
