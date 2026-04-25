package com.hefng.mynocodebackend.service.impl;

import com.hefng.mynocodebackend.ai.AiCodegenServiceFaced;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.enums.ChatMessageTypeEnum;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppServiceImplChatToGenCodeTest {

    private AppServiceImpl appService;

    @Mock
    private AiCodegenServiceFaced aiCodegenServiceFaced;

    @Mock
    private ChatHistoryService chatHistoryService;

    private User loginUser;

    @BeforeEach
    void setUp() {
        appService = spy(new AppServiceImpl());
        ReflectionTestUtils.setField(appService, "aiCodegenServiceFaced", aiCodegenServiceFaced);
        ReflectionTestUtils.setField(appService, "chatHistoryService", chatHistoryService);
        ReflectionTestUtils.setField(appService, "agentWorkflowEnabled", false);
        ReflectionTestUtils.setField(appService, "agentWorkflowFallbackToLegacy", true);

        loginUser = new User();
        loginUser.setId(1L);
    }

    @Test
    void shouldPersistUserAndAiMessagesOnSuccess() {
        App app = new App();
        app.setId(100L);
        app.setAppOwnerId(1L);
        app.setInitPrompt("hello");
        app.setCodegenType("html");
        doReturn(app).when(appService).getById(100L);
        when(aiCodegenServiceFaced.generateAndSaveCodeWithStream(eq("hello"), eq(CodegenTypeEnum.HTML), eq(100L)))
                .thenReturn(Flux.just("{\"d\":\"A\"}", "{\"d\":\"B\"}"));

        java.util.List<ServerSentEvent<String>> events = appService.chatToGenCode(100L, "hello", false, loginUser)
                .collectList()
                .block();

        verify(chatHistoryService).saveChatMessage(100L, 1L, "hello", ChatMessageTypeEnum.USER.getValue());
        verify(chatHistoryService).saveChatMessage(100L, 1L, "AB", ChatMessageTypeEnum.AI.getValue());
        assertEquals("done", events.getLast().event());
    }

    @Test
    void shouldPersistErrorResultOnFailure() {
        App app = new App();
        app.setId(101L);
        app.setAppOwnerId(1L);
        app.setInitPrompt("标题改为xxx");
        app.setCodegenType("html");
        doReturn(app).when(appService).getById(101L);
        when(aiCodegenServiceFaced.generateAndSaveCodeWithStream(eq("标题改为xxx"), eq(CodegenTypeEnum.HTML), eq(101L)))
                .thenReturn(Flux.concat(
                        Flux.just("{\"d\":\"partial\"}"),
                        Flux.error(new RuntimeException("boom"))));

        java.util.List<ServerSentEvent<String>> events = appService.chatToGenCode(101L, "标题改为xxx", false, loginUser)
                .collectList()
                .block();

        verify(chatHistoryService).saveChatMessage(101L, 1L, "标题改为xxx", ChatMessageTypeEnum.USER.getValue());
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatHistoryService, atLeast(1))
                .saveChatMessage(eq(101L), eq(1L), messageCaptor.capture(), eq(ChatMessageTypeEnum.AI.getValue()));
        assertTrue(messageCaptor.getAllValues().stream().anyMatch(msg -> msg.contains("partial") && msg.contains("boom")));
        assertEquals("done", events.getLast().event());
    }
}
