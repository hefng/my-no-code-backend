package com.hefng.mynocodebackend.model.dto.app;

import java.io.Serializable;
import com.hefng.mynocodebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用查询请求
 *
 * @author https://github.com/hefng
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

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
     * 代码生成类型
     */
    private String codegenType;

    /**
     * 应用所有者id
     */
    private Long appOwnerId;

    /**
     * 优先级
     */
    private Integer priority;

    private static final long serialVersionUID = 1L;
}
