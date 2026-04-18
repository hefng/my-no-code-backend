package com.hefng.mynocodebackend.langgraph4j.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 质检结果实体类
 * 包含是否通过质检、错误列表和改进建议等信息
 * 用于在工作流中传递质检结果，帮助后续节点进行决策和处理
 * @author hefng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityResult implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 是否通过质检
     */
    private Boolean isValid;
    
    /**
     * 错误列表
     */
    private List<String> errors;
    
    /**
     * 改进建议
     */
    private List<String> suggestions;
}