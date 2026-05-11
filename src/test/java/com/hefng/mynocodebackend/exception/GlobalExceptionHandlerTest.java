package com.hefng.mynocodebackend.exception;

import com.hefng.mynocodebackend.common.BaseResponse;
import com.hefng.mynocodebackend.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 全局异常处理器测试，验证限流异常会被转换为统一的接口响应格式。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    /**
     * 验证限流触发后的业务异常会被全局异常处理器包装成统一响应结构。
     */
    void shouldReturnUnifiedResponseForRateLimitBusinessException() {
        BusinessException exception =
                new BusinessException(ErrorCode.OPERATION_ERROR, "请求过于频繁，请稍后再试");

        BaseResponse<?> response = globalExceptionHandler.businessExceptionHandler(exception);

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), response.getCode());
        assertNull(response.getData());
        assertEquals("请求过于频繁，请稍后再试", response.getMessage());
    }
}
