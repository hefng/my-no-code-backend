package com.hefng.mynocodebackend.model.dto.app;

import java.io.Serializable;
import lombok.Data;

/**
 * 应用更新请求（管理员）
 *
 * @author https://github.com/hefng
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String appCover;

    /**
     * 优先级
     */
    private Integer priority;

    private static final long serialVersionUID = 1L;
}
