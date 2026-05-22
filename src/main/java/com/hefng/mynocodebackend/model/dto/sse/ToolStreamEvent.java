package com.hefng.mynocodebackend.model.dto.sse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具流式事件载荷。
 * <p>
 * 这里改成普通 JavaBean，避免某些 JSON 库对 record 的反序列化兼容性问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolStreamEvent {

    private String phase;
    private String toolCallId;
    private String toolName;
    private String message;
    private String arguments;
    private String result;
}
