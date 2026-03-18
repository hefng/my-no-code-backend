package com.hefng.mynocodebackend.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用部署请求
 *
 * @author https://github.com/hefng
 */
@Data
public class AppDeployedRequest implements Serializable {

    /**
     * 应用名称
     */
    private Long appId;

    private static final long serialVersionUID = 1L;
}
