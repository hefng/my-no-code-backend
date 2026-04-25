package com.hefng.mynocodebackend.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话生成代码请求
 */
@Data
public class ChatToGenCodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * 是否启用 Agent 工作流，默认 false
     */
    private Boolean isAgent;
}
