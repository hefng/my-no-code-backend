package com.hefng.mynocodebackend.model.dto.chathistory;

import com.hefng.mynocodebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 对话历史查询请求
 *
 * @author https://github.com/hefng
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 用户id（管理员查询用）
     */
    private Long userId;

    /**
     * 消息类型（user/ai）
     */
    private String chatMessageType;

    /**
     * 向前加载时，以此时间为游标，加载比它更早的消息
     */
    private java.time.LocalDateTime beforeTime;

    private static final long serialVersionUID = 1L;
}
