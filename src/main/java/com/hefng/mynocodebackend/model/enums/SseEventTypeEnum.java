package com.hefng.mynocodebackend.model.enums;

/**
 * SSE 事件类型枚举
 * <p>
 * 前端通过 event 字段区分不同类型的流式事件：
 * - thought：深度推理模型的思考过程（逐段输出，供前端实时展示"思考中..."）
 * - answer：最终答案内容（逐段输出，供前端渲染代码或文字）
 * - done：流结束信号（前端收到后关闭 EventSource 连接）
 *
 * @author hefng
 */
public enum SseEventTypeEnum {

    THOUGHT("thought"),
    ANSWER("answer"),
    DONE("done");

    private final String value;

    SseEventTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
