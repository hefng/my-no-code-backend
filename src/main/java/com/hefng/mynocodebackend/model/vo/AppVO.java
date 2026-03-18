package com.hefng.mynocodebackend.model.vo;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 应用视图
 *
 * @author https://github.com/hefng
 */
@Data
public class AppVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用描述
     */
    private String appDesc;

    /**
     * 应用封面
     */
    private String appCover;

    /**
     * 代码生成类型
     */
    private String codegenType;

    /**
     * 部署key
     */
    private String deployedKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 应用所有者id
     */
    private Long appOwnerId;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 应用所有者信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;
}
