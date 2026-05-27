package com.hefng.mynocodebackend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 管理员增加用户应用创建次数请求
 */
@Data
public class AdminAddAppQuotaRequest implements Serializable {

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 增加的次数
     */
    private Integer addCount;

    private static final long serialVersionUID = 1L;
}
