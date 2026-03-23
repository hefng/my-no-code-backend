package com.hefng.mynocodebackend.service;

import com.hefng.mynocodebackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hefng.mynocodebackend.model.entity.ChatHistory;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.vo.ChatHistoryVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 对话历史服务
 *
 * @author https://github.com/hefng
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存一条对话消息
     *
     * @param appId           应用id
     * @param userId          用户id
     * @param messages        消息内容
     * @param chatMessageType 消息类型（user/ai）
     * @return 保存的记录id
     */
    Long saveChatMessage(Long appId, Long userId, String messages, String chatMessageType);

    /**
     * 加载应用最新的一页对话历史（进入页面时调用）
     *
     * @param appId     应用id
     * @param loginUser 当前登录用户
     * @return 最新 10 条消息（按时间升序，便于前端直接渲染）
     */
    List<ChatHistoryVO> listLatestChatHistory(Long appId, User loginUser);

    /**
     * 向前加载更多历史消息（分页加载）
     *
     * @param queryRequest 查询请求（包含 appId、beforeTime、pageSize）
     * @param loginUser    当前登录用户
     * @return 更早的消息列表（按时间升序）
     */
    List<ChatHistoryVO> listMoreChatHistory(ChatHistoryQueryRequest queryRequest, User loginUser);

    /**
     * 删除某个应用的所有对话历史（关联删除，逻辑删除）
     *
     * @param appId 应用id
     */
    void removeByAppId(Long appId);

    /**
     * 获取查询条件（管理员用）
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest);

    /**
     * 将 ChatHistory 转换为 VO
     *
     * @param chatHistory 实体
     * @return VO
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory);

    /**
     * 批量转换为 VO
     *
     * @param chatHistoryList 实体列表
     * @return VO 列表
     */
    List<ChatHistoryVO> getChatHistoryVO(List<ChatHistory> chatHistoryList);
}
