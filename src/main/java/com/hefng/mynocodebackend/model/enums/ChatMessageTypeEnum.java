package com.hefng.mynocodebackend.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话消息类型枚举
 *
 * @author https://github.com/hefng
 */
public enum ChatMessageTypeEnum {

    USER("用户消息", "user"),
    AI("AI消息", "ai");

    private final String text;
    private final String value;

    ChatMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static ChatMessageTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ChatMessageTypeEnum anEnum : ChatMessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
