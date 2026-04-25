package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.model.dto.app.ChatToGenCodeRequest;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.enums.ChatMessageTypeEnum;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import com.hefng.mynocodebackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppControllerChatHistoryTest {

    private AppController appController;

    @Mock
    private AppService appService;

    @Mock
    private UserService userService;

    @Mock
    private ChatHistoryService chatHistoryService;

    @Mock
    private CosClientConfig cosClientConfig;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        appController = new AppController();
        ReflectionTestUtils.setField(appController, "appService", appService);
        ReflectionTestUtils.setField(appController, "userService", userService);
        ReflectionTestUtils.setField(appController, "chatHistoryService", chatHistoryService);
        ReflectionTestUtils.setField(appController, "cosClientConfig", cosClientConfig);

        request = new MockHttpServletRequest();
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
    }

    @Test
    void shouldPersistUserAndAiMessagesOnSuccess() {
        ChatToGenCodeRequest body = new ChatToGenCodeRequest();
        body.setAppId(100L);
        body.setUserMessage("hello");
        body.setIsAgent(true);

        when(appService.chatToGenCode(eq(100L), eq("hello"), eq(true), any(User.class)))
                .thenReturn(Flux.just("{\"d\":\"A\"}", "{\"d\":\"B\"}"));

        appController.chatToGenCode(body, request).collectList().block();

        verify(chatHistoryService).saveChatMessage(100L, 1L, "hello", ChatMessageTypeEnum.USER.getValue());
        verify(chatHistoryService).saveChatMessage(100L, 1L, "AB", ChatMessageTypeEnum.AI.getValue());
    }

    @Test
    void shouldPersistErrorResultOnFailure() {
        ChatToGenCodeRequest body = new ChatToGenCodeRequest();
        body.setAppId(101L);
        body.setUserMessage("标题改为xxx");
        body.setIsAgent(true);

        when(appService.chatToGenCode(eq(101L), eq("标题改为xxx"), eq(true), any(User.class)))
                .thenReturn(Flux.concat(
                        Flux.just("{\"d\":\"partial\"}"),
                        Flux.error(new RuntimeException("boom"))));

        appController.chatToGenCode(body, request).collectList().block();

        verify(chatHistoryService).saveChatMessage(101L, 1L, "标题改为xxx", ChatMessageTypeEnum.USER.getValue());
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatHistoryService, atLeast(1))
                .saveChatMessage(eq(101L), eq(1L), messageCaptor.capture(), eq(ChatMessageTypeEnum.AI.getValue()));
        assertTrue(messageCaptor.getAllValues().stream().anyMatch(msg -> msg.contains("partial") && msg.contains("boom")));
    }
}
