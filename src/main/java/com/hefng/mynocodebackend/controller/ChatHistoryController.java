package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.annotation.AuthCheck;
import com.hefng.mynocodebackend.common.BaseResponse;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.common.ResultUtils;
import com.hefng.mynocodebackend.constant.UserConstant;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hefng.mynocodebackend.model.dto.chathistory.ChatHistorySaveRequest;
import com.hefng.mynocodebackend.model.entity.ChatHistory;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.enums.ChatMessageTypeEnum;
import com.hefng.mynocodebackend.model.vo.ChatHistoryVO;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import com.hefng.mynocodebackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话历史控制层
 *
 * @author https://github.com/hefng
 */
@Slf4j
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    // region 对话历史保存（内部调用）

    /**
     * 保存一条对话消息（用户消息或 AI 消息）
     * 通常由 AppController 的对话接口在内部调用，也可由前端直接调用保存用户消息
     *
     * @param saveRequest 保存请求
     * @param request     HTTP 请求
     * @return 保存的记录 id
     */
    @PostMapping("/save")
    public BaseResponse<Long> saveChatMessage(@RequestBody ChatHistorySaveRequest saveRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(saveRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = saveRequest.getAppId();
        String messages = saveRequest.getMessages();
        String chatMessageType = saveRequest.getChatMessageType();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(ChatMessageTypeEnum.getEnumByValue(chatMessageType) == null,
                ErrorCode.PARAMS_ERROR, "消息类型不合法");

        User loginUser = userService.getLoginUser(request);
        Long id = chatHistoryService.saveChatMessage(appId, loginUser.getId(), messages, chatMessageType);
        return ResultUtils.success(id);
    }

    // endregion

    // region 对话历史查询（应用创建者 / 管理员）

    /**
     * 进入应用页面时加载最新 10 条对话历史
     * 仅应用创建者和管理员可见
     *
     * @param appId   应用id
     * @param request HTTP 请求
     * @return 最新 10 条消息（按时间升序）
     */
    @GetMapping("/list/latest")
    public BaseResponse<List<ChatHistoryVO>> listLatestChatHistory(@RequestParam Long appId,
                                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        User loginUser = userService.getLoginUser(request);

        // 校验权限：仅应用创建者或管理员可查看
        checkChatHistoryPermission(appId, loginUser);

        List<ChatHistoryVO> list = chatHistoryService.listLatestChatHistory(appId, loginUser);
        return ResultUtils.success(list);
    }

    /**
     * 向前加载更多历史消息（类似聊天软件的上拉加载）
     * 仅应用创建者和管理员可见
     *
     * @param queryRequest 查询请求（需包含 appId 和 beforeId）
     * @param request      HTTP 请求
     * @return 更早的消息列表（按时间升序）
     */
    @PostMapping("/list/more")
    public BaseResponse<List<ChatHistoryVO>> listMoreChatHistory(@RequestBody ChatHistoryQueryRequest queryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = queryRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(queryRequest.getBeforeTime() == null, ErrorCode.PARAMS_ERROR, "beforeTime不能为空");

        User loginUser = userService.getLoginUser(request);
        checkChatHistoryPermission(appId, loginUser);

        List<ChatHistoryVO> list = chatHistoryService.listMoreChatHistory(queryRequest, loginUser);
        return ResultUtils.success(list);
    }

    // endregion

    // region 管理员接口

    /**
     * 分页查询所有应用的对话历史（管理员）
     * 按时间降序排序，便于内容监管
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryByPage(@RequestBody ChatHistoryQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();

        Page<ChatHistory> page = chatHistoryService.page(
                new Page<>(current, size),
                chatHistoryService.getQueryWrapper(queryRequest));

        Page<ChatHistoryVO> voPage = new Page<>(current, size, page.getTotalRow());
        voPage.setRecords(chatHistoryService.getChatHistoryVO(page.getRecords()));
        return ResultUtils.success(voPage);
    }

    // endregion

    // region 私有方法

    /**
     * 校验对话历史查看权限：仅应用创建者或管理员可查看
     */
    private void checkChatHistoryPermission(Long appId, User loginUser) {
        // 管理员直接放行
        if (userService.isAdmin(loginUser)) {
            return;
        }
        // 校验应用是否存在，且当前用户是否为应用创建者
        com.hefng.mynocodebackend.model.entity.App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getAppOwnerId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限查看该应用的对话历史");
    }

    // endregion
}
