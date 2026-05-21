package com.hefng.mynocodebackend.model.dto.sse;

public record ToolStreamEvent(
        String phase,
        String toolCallId,
        String toolName,
        String message,
        String arguments,
        String result) {
}
