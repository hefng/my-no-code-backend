package com.hefng.mynocodebackend.model.dto.app;

import java.io.Serializable;
import lombok.Data;

/**
 * 应用更新请求（用户修改自己的应用）
 *
 * @author https://github.com/hefng
 */
@Data
public class AppUpdateMyRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}
