package com.hefng.mynocodebackend.model.dto.sse;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.hefng.mynocodebackend.model.enums.SseEventTypeEnum;
import com.hefng.mynocodebackend.utils.SseEventBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 ToolStreamEvent 在当前项目的 JSON 转换链路中是否正常。
 * 重点是确认：
 * 1. Hutool 可以把普通 JavaBean 正常序列化为 JSON
 * 2. Hutool 可以把 JSON 正常反序列化回 record
 * 3. SSE 包装后，event 和 d 字段结构符合前端消费预期
 */
class ToolStreamEventJsonTest {

    @Test
    void shouldSerializeAndDeserializeBeanNormally() {
        ToolStreamEvent event = new ToolStreamEvent(
                "executing",
                "tool-call-1",
                "saveFile",
                "正在保存文件",
                "{\"relativePath\":\"src/App.vue\"}",
                null
        );

        // 普通 JavaBean 需要验证 getter/setter 这条链路能否正常工作。
        String json = JSONUtil.toJsonStr(event);
        JSONObject jsonObject = JSONUtil.parseObj(json);

        assertEquals("executing", jsonObject.getStr("phase"));
        assertEquals("tool-call-1", jsonObject.getStr("toolCallId"));
        assertEquals("saveFile", jsonObject.getStr("toolName"));
        assertEquals("正在保存文件", jsonObject.getStr("message"));
        assertEquals("{\"relativePath\":\"src/App.vue\"}", jsonObject.getStr("arguments"));
        assertNull(jsonObject.get("result"));

        ToolStreamEvent parsed = JSONUtil.toBean(json, ToolStreamEvent.class);
        assertNotNull(parsed);
        assertEquals(event, parsed);
    }

    @Test
    void shouldWrapBeanIntoSsePayloadCorrectly() {
        ToolStreamEvent event = new ToolStreamEvent(
                "completed",
                "tool-call-2",
                "readFile",
                "读取文件完成",
                "{\"relativePath\":\"src/main.js\"}",
                "读取成功"
        );

        String sseJson = SseEventBuilder.build(SseEventTypeEnum.TOOL, event);
        JSONObject jsonObject = JSONUtil.parseObj(sseJson);

        assertEquals("tool", jsonObject.getStr("event"));
        assertNotNull(jsonObject.getJSONObject("d"));
        assertEquals("completed", jsonObject.getJSONObject("d").getStr("phase"));
        assertEquals("readFile", jsonObject.getJSONObject("d").getStr("toolName"));
        assertEquals("读取文件完成", jsonObject.getJSONObject("d").getStr("message"));
        assertEquals("读取成功", jsonObject.getJSONObject("d").getStr("result"));
    }
}
