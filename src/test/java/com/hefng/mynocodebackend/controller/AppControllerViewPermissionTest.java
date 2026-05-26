package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.ai.service.AiCodeGenTypeRoutingService;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.vo.AppVO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppControllerViewPermissionTest {

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

    @Test
    void shouldAllowPublicFeaturedAppToBeViewed() {
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(userService.isAdmin(loginUser)).thenReturn(false);

        App app = new App();
        app.setId(100L);
        app.setAppOwnerId(2L);
        app.setPriority(AppConstant.MAX_PRIORITY);

        AppVO appVO = new AppVO();
        appVO.setId(100L);
        appVO.setPriority(AppConstant.MAX_PRIORITY);

        when(appService.getById(100L)).thenReturn(app);
        when(appService.getAppVO(app)).thenReturn(appVO);

        AppVO result = appController.getAppVOById(createQueryRequest(100L), request).getData();

        assertEquals(100L, result.getId());
        assertEquals(AppConstant.MAX_PRIORITY, result.getPriority());
    }

    @Test
    void shouldRejectPrivateAppViewedByOtherUser() {
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(userService.isAdmin(loginUser)).thenReturn(false);

        App app = new App();
        app.setId(101L);
        app.setAppOwnerId(2L);
        app.setPriority(AppConstant.DEFAULT_PRIORITY);

        when(appService.getById(101L)).thenReturn(app);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> appController.getAppVOById(createQueryRequest(101L), request)
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertEquals("无权查看当前应用", exception.getMessage());
    }

    private com.hefng.mynocodebackend.model.dto.app.AppQueryRequest createQueryRequest(Long id) {
        com.hefng.mynocodebackend.model.dto.app.AppQueryRequest request = new com.hefng.mynocodebackend.model.dto.app.AppQueryRequest();
        request.setId(id);
        return request;
    }
}
