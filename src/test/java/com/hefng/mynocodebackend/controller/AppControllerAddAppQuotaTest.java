package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.ai.service.AiCodeGenTypeRoutingService;
import com.hefng.mynocodebackend.common.BaseResponse;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.model.dto.app.AppAddRequest;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.UserService;
import com.hefng.mynocodebackend.utils.RedisCacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppControllerAddAppQuotaTest {

    private AppController appController;

    @Mock
    private AppService appService;

    @Mock
    private UserService userService;

    @Mock
    private CosClientConfig cosClientConfig;

    @Mock
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Mock
    private RedisCacheUtil redisCacheUtil;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        appController = new AppController();
        ReflectionTestUtils.setField(appController, "appService", appService);
        ReflectionTestUtils.setField(appController, "userService", userService);
        ReflectionTestUtils.setField(appController, "cosClientConfig", cosClientConfig);
        ReflectionTestUtils.setField(appController, "aiCodeGenTypeRoutingService", aiCodeGenTypeRoutingService);
        ReflectionTestUtils.setField(appController, "redisCacheUtil", redisCacheUtil);
        request = new MockHttpServletRequest();
    }

    private AppAddRequest createAddRequest(String initPrompt) {
        AppAddRequest req = new AppAddRequest();
        req.setInitPrompt(initPrompt);
        return req;
    }

    private User createLoginUser(Long id, int appMaxCount, int appUsedCount) {
        User user = new User();
        user.setId(id);
        user.setAppMaxCount(appMaxCount);
        user.setAppUsedCount(appUsedCount);
        return user;
    }

    @Test
    void shouldCreateAppWhenQuotaAvailable() {
        User loginUser = createLoginUser(1L, 3, 1);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(cosClientConfig.getHost()).thenReturn("https://cos.example.com");
        when(aiCodeGenTypeRoutingService.routeCodeGenType(any())).thenReturn(null);
        when(appService.save(any(App.class))).thenAnswer(invocation -> {
            App app = invocation.getArgument(0);
            app.setId(100L);
            return true;
        });
        when(userService.incrementAppUsedCountIfWithinLimit(1L)).thenReturn(true);

        BaseResponse<Long> response = appController.addApp(createAddRequest("测试提示词"), request);

        assertEquals(100L, response.getData());
        verify(userService).incrementAppUsedCountIfWithinLimit(1L);
    }

    @Test
    void shouldRejectAppCreationWhenQuotaExhausted() {
        User loginUser = createLoginUser(1L, 3, 3);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(cosClientConfig.getHost()).thenReturn("https://cos.example.com");
        when(aiCodeGenTypeRoutingService.routeCodeGenType(any())).thenReturn(null);
        when(appService.save(any(App.class))).thenAnswer(invocation -> {
            App app = invocation.getArgument(0);
            app.setId(100L);
            return true;
        });
        when(userService.incrementAppUsedCountIfWithinLimit(1L)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> appController.addApp(createAddRequest("测试提示词"), request)
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("应用创建次数已用尽", exception.getMessage());
        verify(appService).removeById(100L);
    }

    @Test
    void shouldSkipQuotaCheckWhenMaxCountIsMinusOne() {
        User loginUser = createLoginUser(1L, -1, 10);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(cosClientConfig.getHost()).thenReturn("https://cos.example.com");
        when(aiCodeGenTypeRoutingService.routeCodeGenType(any())).thenReturn(null);
        when(appService.save(any(App.class))).thenAnswer(invocation -> {
            App app = invocation.getArgument(0);
            app.setId(200L);
            return true;
        });

        BaseResponse<Long> response = appController.addApp(createAddRequest("测试提示词"), request);

        assertEquals(200L, response.getData());
        verify(userService).incrementAppUsedCount(1L);
        verify(userService, never()).incrementAppUsedCountIfWithinLimit(anyLong());
    }

    @Test
    void shouldRejectWhenInitPromptIsBlank() {
        assertThrows(
                BusinessException.class,
                () -> appController.addApp(createAddRequest(""), request)
        );
    }

    @Test
    void shouldRejectWhenAppAddRequestIsNull() {
        assertThrows(
                BusinessException.class,
                () -> appController.addApp(null, request)
        );
    }
}
