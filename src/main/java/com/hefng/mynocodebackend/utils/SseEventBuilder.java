package com.hefng.mynocodebackend.utils;

import cn.hutool.json.JSONUtil;
import com.hefng.mynocodebackend.model.enums.SseEventTypeEnum;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SseEventBuilder {

    private SseEventBuilder() {
    }

    public static String build(SseEventTypeEnum eventType, Object data) {
        return build(eventType.getValue(), data);
    }

    public static String build(String event, Object data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event);
        payload.put("d", data);
        return JSONUtil.toJsonStr(payload);
    }
}
