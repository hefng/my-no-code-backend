package com.hefng.mynocodebackend.model.dto.chathistory;

import lombok.Data;

import java.io.Serializable;

/**
 * 保存对话历史请求
 *
 * @author https://github.com/hefng
 */
@Data
public class ChatHistorySaveRequest implements Serializable {

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 消息内容
     */
    private String messages;

    /**
     * 消息类型（user/ai）
     */
    private String chatMessageType;

    private static final long serialVersionUID = 1L;
}
