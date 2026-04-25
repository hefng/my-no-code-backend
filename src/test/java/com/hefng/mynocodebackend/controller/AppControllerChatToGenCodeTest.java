package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.model.dto.app.ChatToGenCodeRequest;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppControllerChatToGenCodeTest {

    private AppController appController;

    @Mock
    private AppService appService;

    @Mock
    private UserService userService;

    @Mock
    private CosClientConfig cosClientConfig;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        appController = new AppController();
        ReflectionTestUtils.setField(appController, "appService", appService);
        ReflectionTestUtils.setField(appController, "userService", userService);
        ReflectionTestUtils.setField(appController, "cosClientConfig", cosClientConfig);

        request = new MockHttpServletRequest();
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
    }

    @Test
    void shouldDelegateChatToService() {
        ChatToGenCodeRequest body = new ChatToGenCodeRequest();
        body.setAppId(100L);
        body.setUserMessage("hello");
        body.setIsAgent(true);

        when(appService.chatToGenCode(eq(100L), eq("hello"), eq(true), any(User.class)))
                .thenReturn(Flux.just(ServerSentEvent.<String>builder().event("done").data("").build()));

        appController.chatToGenCode(body, request).collectList().block();

        verify(appService).chatToGenCode(eq(100L), eq("hello"), eq(true), any(User.class));
    }
}
