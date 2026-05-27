package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.common.BaseResponse;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.manager.CosManager;
import com.hefng.mynocodebackend.model.dto.user.AdminAddAppQuotaRequest;
import com.hefng.mynocodebackend.model.dto.user.UserUpdateRequest;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerQuotaTest {

    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private CosManager cosManager;

    @Mock
    private CosClientConfig cosClientConfig;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        userController = new UserController();
        ReflectionTestUtils.setField(userController, "userService", userService);
        ReflectionTestUtils.setField(userController, "cosManager", cosManager);
        ReflectionTestUtils.setField(userController, "cosClientConfig", cosClientConfig);
        ReflectionTestUtils.setField(userController, "stringRedisTemplate", stringRedisTemplate);
    }

    // ========== addAppQuota 测试 ==========

    @Test
    void shouldAddQuotaSuccessfully() {
        User user = new User();
        user.setId(1L);
        user.setAppMaxCount(3);
        user.setAppUsedCount(2);
        when(userService.getById(1L)).thenReturn(user);
        when(userService.updateById(any(User.class))).thenReturn(true);

        AdminAddAppQuotaRequest request = new AdminAddAppQuotaRequest();
        request.setUserId(1L);
        request.setAddCount(5);

        BaseResponse<Boolean> response = userController.addAppQuota(request);

        assertTrue(response.getData());
        assertEquals(8, user.getAppMaxCount());
        verify(userService).updateById(user);
    }

    @Test
    void shouldRejectWhenUserIdIsNull() {
        AdminAddAppQuotaRequest request = new AdminAddAppQuotaRequest();
        request.setAddCount(5);

        assertThrows(BusinessException.class, () -> userController.addAppQuota(request));
    }

    @Test
    void shouldRejectWhenAddCountIsZero() {
        AdminAddAppQuotaRequest request = new AdminAddAppQuotaRequest();
        request.setUserId(1L);
        request.setAddCount(0);

        assertThrows(BusinessException.class, () -> userController.addAppQuota(request));
    }

    @Test
    void shouldRejectWhenAddCountIsNegative() {
        AdminAddAppQuotaRequest request = new AdminAddAppQuotaRequest();
        request.setUserId(1L);
        request.setAddCount(-1);

        assertThrows(BusinessException.class, () -> userController.addAppQuota(request));
    }

    @Test
    void shouldRejectWhenUserNotFound() {
        when(userService.getById(999L)).thenReturn(null);

        AdminAddAppQuotaRequest request = new AdminAddAppQuotaRequest();
        request.setUserId(999L);
        request.setAddCount(5);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userController.addAppQuota(request)
        );
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldAddQuotaFromNullMaxCount() {
        User user = new User();
        user.setId(1L);
        user.setAppMaxCount(null);
        when(userService.getById(1L)).thenReturn(user);
        when(userService.updateById(any(User.class))).thenReturn(true);

        AdminAddAppQuotaRequest request = new AdminAddAppQuotaRequest();
        request.setUserId(1L);
        request.setAddCount(3);

        userController.addAppQuota(request);

        assertEquals(3, user.getAppMaxCount());
    }

    // ========== updateUser appMaxCount 校验测试 ==========

    @Test
    void shouldUpdateAppMaxCountSuccessfully() {
        User existing = new User();
        existing.setId(1L);
        existing.setAppUsedCount(2);
        existing.setAppMaxCount(3);
        when(userService.getById(1L)).thenReturn(existing);
        when(userService.updateById(any(User.class))).thenReturn(true);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(1L);
        request.setAppMaxCount(10);

        BaseResponse<Boolean> response = userController.updateUser(request, null);

        assertTrue(response.getData());
    }

    @Test
    void shouldRejectWhenAppMaxCountBelowUsedCount() {
        User existing = new User();
        existing.setId(1L);
        existing.setAppUsedCount(5);
        existing.setAppMaxCount(10);
        when(userService.getById(1L)).thenReturn(existing);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(1L);
        request.setAppMaxCount(3);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userController.updateUser(request, null)
        );
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("最大次数不能低于已使用次数", exception.getMessage());
    }

    @Test
    void shouldAllowMinusOneAsAppMaxCount() {
        User existing = new User();
        existing.setId(1L);
        existing.setAppUsedCount(5);
        existing.setAppMaxCount(10);
        when(userService.updateById(any(User.class))).thenReturn(true);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(1L);
        request.setAppMaxCount(-1);

        BaseResponse<Boolean> response = userController.updateUser(request, null);

        assertTrue(response.getData());
    }

    @Test
    void shouldAutoSetMinusOneWhenRoleBecomesAdmin() {
        when(userService.updateById(any(User.class))).thenReturn(true);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(1L);
        request.setUserRole("admin");

        userController.updateUser(request, null);

        verify(userService).updateById(argThat(u -> u.getAppMaxCount() == -1));
    }
}
