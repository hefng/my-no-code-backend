package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.model.dto.app.ChatToGenCodeRequest;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.ai.service.AiCodeGenTypeRoutingService;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.UserService;
import com.hefng.mynocodebackend.utils.RedisCacheUtil;
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
/**
 * AppController 的 chatToGenCode 接口测试，验证请求会正确校验登录用户并委派给应用服务。
 */
class AppControllerChatToGenCodeTest {

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
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
    }

    @Test
    /**
     * 验证 chatToGenCode 接口会将请求参数和当前登录用户正确透传给应用服务。
     */
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
