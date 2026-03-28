package com.hefng.mynocodebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史视图
 *
 * @author https://github.com/hefng
 */
@Data
public class ChatHistoryVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 消息内容
     */
    private String messages;

    /**
     * 消息类型（user/ai）
     */
    private String chatMessageType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 发送者信息（脱敏）
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;
}
