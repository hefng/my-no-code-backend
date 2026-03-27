package com.hefng.mynocodebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.mapper.ChatHistoryMapper;
import com.hefng.mynocodebackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hefng.mynocodebackend.model.entity.ChatHistory;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.enums.ChatMessageTypeEnum;
import com.hefng.mynocodebackend.model.vo.ChatHistoryVO;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import com.hefng.mynocodebackend.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hefng.mynocodebackend.model.table.ChatHistoryTableDef.CHAT_HISTORY;

/**
 * 对话历史服务实现
 *
 * @author https://github.com/hefng
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    private UserService userService;

    @Override
    public Long saveChatMessage(Long appId, Long userId, String messages, String chatMessageType) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户id不合法");
        ThrowUtils.throwIf(StringUtils.isBlank(messages), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(ChatMessageTypeEnum.getEnumByValue(chatMessageType) == null,
                ErrorCode.PARAMS_ERROR, "消息类型不合法");

        // 保存消息
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .messages(messages)
                .chatMessageType(chatMessageType)
                .build();
        boolean saved = this.save(chatHistory);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "保存对话消息失败");
        return chatHistory.getId();
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatMessageTypeEnum.USER.getValue().equals(history.getChatMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessages()));
                    loadedCount++;
                } else if (ChatMessageTypeEnum.AI.getValue().equals(history.getChatMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessages()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }


    @Override
    public List<ChatHistoryVO> listLatestChatHistory(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 按 createTime 降序取最新 10 条，再反转为升序返回给前端
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(CHAT_HISTORY.APP_ID.eq(appId))
                .and(CHAT_HISTORY.USER_ID.eq(loginUser.getId()))
                .orderBy(CHAT_HISTORY.CREATE_TIME.desc())
                .limit(10);

        List<ChatHistory> list = this.list(queryWrapper);
        java.util.Collections.reverse(list);
        return getChatHistoryVO(list);
    }

    @Override
    public List<ChatHistoryVO> listMoreChatHistory(ChatHistoryQueryRequest queryRequest, User loginUser) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = queryRequest.getAppId();
        java.time.LocalDateTime beforeTime = queryRequest.getBeforeTime();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(beforeTime == null, ErrorCode.PARAMS_ERROR, "beforeTime不能为空");

        int pageSize = queryRequest.getPageSize();
        if (pageSize <= 0 || pageSize > 50) {
            pageSize = 10;
        }

        // 加载比 beforeTime 更早的消息，降序取后反转为升序
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(CHAT_HISTORY.APP_ID.eq(appId))
                .and(CHAT_HISTORY.USER_ID.eq(loginUser.getId()))
                .and(CHAT_HISTORY.CREATE_TIME.lt(beforeTime))
                .orderBy(CHAT_HISTORY.CREATE_TIME.desc())
                .limit(pageSize);

        List<ChatHistory> list = this.list(queryWrapper);
        java.util.Collections.reverse(list);
        return getChatHistoryVO(list);
    }

    @Override
    public void removeByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(CHAT_HISTORY.APP_ID.eq(appId));
        this.remove(queryWrapper);
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long appId = queryRequest.getAppId();
        Long userId = queryRequest.getUserId();
        String chatMessageType = queryRequest.getChatMessageType();

        return QueryWrapper.create()
                .where(CHAT_HISTORY.APP_ID.eq(appId).when(appId != null))
                .and(CHAT_HISTORY.USER_ID.eq(userId).when(userId != null))
                .and(CHAT_HISTORY.CHAT_MESSAGE_TYPE.eq(chatMessageType).when(StringUtils.isNotBlank(chatMessageType)))
                .orderBy(CHAT_HISTORY.CREATE_TIME.desc());
    }

    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO vo = new ChatHistoryVO();
        BeanUtils.copyProperties(chatHistory, vo);

        Long userId = chatHistory.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            vo.setUser(userService.getUserVO(user));
        }
        return vo;
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVO(List<ChatHistory> chatHistoryList) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }

        // 批量查询用户信息，避免 N+1
        Set<Long> userIdSet = chatHistoryList.stream()
                .map(ChatHistory::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        return chatHistoryList.stream().map(chatHistory -> {
            ChatHistoryVO vo = new ChatHistoryVO();
            BeanUtils.copyProperties(chatHistory, vo);
            Long userId = chatHistory.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).getFirst();
            }
            vo.setUser(userService.getUserVO(user));
            return vo;
        }).collect(Collectors.toList());
    }
}
