package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import com.hefng.mynocodebackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryControllerPermissionTest {

    private ChatHistoryController chatHistoryController;

    @Mock
    private ChatHistoryService chatHistoryService;

    @Mock
    private UserService userService;

    @Mock
    private AppService appService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        chatHistoryController = new ChatHistoryController();
        ReflectionTestUtils.setField(chatHistoryController, "chatHistoryService", chatHistoryService);
        ReflectionTestUtils.setField(chatHistoryController, "userService", userService);
        ReflectionTestUtils.setField(chatHistoryController, "appService", appService);

        request = new MockHttpServletRequest();
    }

    @Test
    void shouldAllowFeaturedAppHistoryForOtherUser() {
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(userService.isAdmin(loginUser)).thenReturn(false);

        App app = new App();
        app.setId(200L);
        app.setAppOwnerId(2L);
        app.setPriority(99);

        when(appService.getById(200L)).thenReturn(app);
        when(chatHistoryService.listLatestChatHistory(any(), any())).thenReturn(Collections.emptyList());

        var response = chatHistoryController.listLatestChatHistory(200L, request);

        assertEquals(20000, response.getCode());
        assertEquals(Collections.emptyList(), response.getData());
    }

    @Test
    void shouldRejectPrivateAppHistoryForOtherUser() {
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(userService.isAdmin(loginUser)).thenReturn(false);

        App app = new App();
        app.setId(201L);
        app.setAppOwnerId(2L);
        app.setPriority(0);

        when(appService.getById(201L)).thenReturn(app);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatHistoryController.listLatestChatHistory(201L, request)
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertEquals("无权查看当前应用", exception.getMessage());
    }
}
