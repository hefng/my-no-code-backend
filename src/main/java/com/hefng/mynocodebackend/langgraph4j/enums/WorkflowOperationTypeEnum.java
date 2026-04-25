package com.hefng.mynocodebackend.langgraph4j.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 工作流操作类型
 */
@Getter
public enum WorkflowOperationTypeEnum {

    CREATE("create"),
    MODIFY("modify");

    private final String value;

    WorkflowOperationTypeEnum(String value) {
        this.value = value;
    }

    public static WorkflowOperationTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
