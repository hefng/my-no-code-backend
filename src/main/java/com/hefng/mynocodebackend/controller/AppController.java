package com.hefng.mynocodebackend.controller;

import cn.hutool.json.JSONUtil;
import com.hefng.mynocodebackend.ai.AiCodeGenTypeRoutingService;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.annotation.AuthCheck;
import com.hefng.mynocodebackend.common.BaseResponse;
import com.hefng.mynocodebackend.common.DeleteRequest;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.common.ResultUtils;
import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.constant.UserConstant;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.model.dto.app.*;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.enums.ChatMessageTypeEnum;
import com.hefng.mynocodebackend.model.vo.AppVO;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import com.hefng.mynocodebackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;

import static com.hefng.mynocodebackend.model.table.AppTableDef.APP;

/**
 * 应用控制层
 *
 * @author https://github.com/hefng
 */
@Slf4j
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    /**
     * 调用 AI 生成代码（流式 SSE）
     * <p>
     * SSE 事件类型说明（前端通过 event 字段区分）：
     * - message：HTML/MULTI_FILE 生成的代码片段，data 格式为 {"d": "代码片段"}
     * - thought：Vue 工程化深度推理模型的思考过程片段，data 格式为 {"event":"thought","d":"内容"}
     * - answer：Vue 工程化最终答案片段，data 格式为 {"event":"answer","d":"内容"}
     * - done：流结束信号，前端收到后关闭 EventSource 连接
     *
     * @param appId       应用 id
     * @param userMessage 用户输入的消息
     * @param request     HTTP 请求
     * @return SSE 事件流
     */
    @GetMapping(value = "chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                      @RequestParam String userMessage,
                                                      HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(StringUtils.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "用户消息不能为空");

        User loginUser = userService.getLoginUser(request);

        // 在调用服务生成代码之前，先保存用户的输入消息到对话历史中
        chatHistoryService.saveChatMessage(appId, loginUser.getId(), userMessage, ChatMessageTypeEnum.USER.getValue());

        Flux<String> rawFlux = appService.chatToGenCode(appId, userMessage, loginUser);

        // 用于收集完整 AI 回答（思考过程 + 最终答案），流结束后一并保存到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();

        // 将原始流的每个 JSON chunk 转换为 SSE 事件
        // 核心逻辑：从 chunk 中解析 event 字段，决定 SSE event 名称
        // - Vue 工程化：chunk 格式为 {"event":"thought"|"answer","d":"..."} → SSE event = thought/answer
        // - HTML/MULTI_FILE：chunk 格式为 {"d":"..."} → SSE event = message（保持原有行为）
        Flux<ServerSentEvent<String>> messageFlux = rawFlux
                .doOnNext(chunk -> {
                    // 收集原始内容用于保存对话历史（只收集 d 字段的内容，过滤掉 event 元数据）
                    try {
                        cn.hutool.json.JSONObject json = JSONUtil.parseObj(chunk);
                        String d = json.getStr("d", "");
                        if (!d.isEmpty()) {
                            aiResponseBuilder.append(d);
                        }
                    } catch (Exception ignored) {
                        // 解析失败时直接追加原始内容，避免丢失数据
                        aiResponseBuilder.append(chunk);
                    }
                })
                .doOnComplete(() -> {
                    // 流结束后保存 AI 完整回答到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    if (StringUtils.isNotBlank(aiResponse)) {
                        try {
                            chatHistoryService.saveChatMessage(appId, loginUser.getId(), aiResponse, ChatMessageTypeEnum.AI.getValue());
                        } catch (Exception e) {
                            log.error("保存 AI 回答到对话历史失败, appId={}", appId, e);
                        }
                    }
                })
                .map(chunk -> {
                    // 解析 chunk 中的 event 字段，决定 SSE event 名称
                    // Vue 工程化的 chunk 带有 event 字段；HTML 的 chunk 只有 d 字段
                    String sseEvent = "message";
                    try {
                        cn.hutool.json.JSONObject json = JSONUtil.parseObj(chunk);
                        String eventField = json.getStr("event");
                        if (StringUtils.isNotBlank(eventField)) {
                            // thought 或 answer 事件，直接使用 event 字段值作为 SSE event 名称
                            sseEvent = eventField;
                        }
                    } catch (Exception ignored) {
                        // 解析失败时降级为 message 事件
                    }
                    return ServerSentEvent.<String>builder()
                            .event(sseEvent)
                            .data(chunk)
                            .build();
                });

        // done 事件：流结束后通知前端关闭连接
        Flux<ServerSentEvent<String>> doneFlux = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("done")
                        .data("")
                        .build()
        );

        return Flux.concat(messageFlux, doneFlux);
    }

    /**
     * 部署应用
     *
     * @param appDeployedRequest
     * @param request
     * @return
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployedApp(@RequestBody AppDeployedRequest appDeployedRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(appDeployedRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployedRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 部署应用
        String deployedUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployedUrl);
    }

    /**
     * 创建应用
     *
     * @param appAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        if (appAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String initPrompt = appAddRequest.getInitPrompt();
        if (StringUtils.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化提示不能为空");
        }
        
        User loginUser = userService.getLoginUser(request);

        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);
        app.setAppOwnerId(loginUser.getId());
        app.setAppName(initPrompt.substring(0, 4)); // 暂时使用初始化提示的前4个字符作为应用名称
        app.setPriority(AppConstant.DEFAULT_PRIORITY);
        // 构建默认的应用封面url
        String defaultCoverUrl = cosClientConfig.getHost() + "/covers/public/default_covers.png";
        app.setAppCover(defaultCoverUrl);
        // 调用 AI 代码生成类型路由服务，根据用户输入的初始化提示智能推荐代码生成类型
        CodegenTypeEnum codegenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        String codegenType = codegenTypeEnum != null ? codegenTypeEnum.getType() : null;
        // 降级策略：如果 AI 推荐失败，则默认生成 HTML 代码
        if (codegenType == null) {
            app.setCodegenType(AppConstant.DEFAULT_CODEGEN_TYPE);
        } else {
            app.setCodegenType(codegenType);
        }

        boolean result = false;
        try {
            result = appService.save(app);
        } catch (Exception e) {
            log.error("保存应用失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存应用失败: " + e.getMessage());
        }
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        
        return ResultUtils.success(app.getId());
    }

    /**
     * 根据 id 修改自己的应用（目前只支持修改应用名称）
     *
     * @param appUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyApp(@RequestBody AppUpdateMyRequest appUpdateMyRequest,
                                             HttpServletRequest request) {
        if (appUpdateMyRequest == null || appUpdateMyRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        User loginUser = userService.getLoginUser(request);
        Long appId = appUpdateMyRequest.getId();
        
        // 判断应用是否存在
        App oldApp = appService.getById(appId);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 仅本人可以修改
        if (!oldApp.getAppOwnerId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        
        App app = new App();
        app.setId(appId);
        app.setAppName(appUpdateMyRequest.getAppName());
        
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 删除自己的应用
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete/my")
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        User loginUser = userService.getLoginUser(request);
        Long appId = deleteRequest.getId();
        
        // 判断应用是否存在
        App oldApp = appService.getById(appId);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 仅本人可以删除
        if (!oldApp.getAppOwnerId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        
        boolean result = appService.deleteAppWithHistory(appId);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 查看应用详情
     *
     * @param appQueryRequest
     * @return
     */
    @PostMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = appQueryRequest.getId();
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表（支持根据名称查询，每页最多 20 个）
     *
     * @param appQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/my")
    public BaseResponse<Page<AppVO>> listMyAppByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                      HttpServletRequest request) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        User loginUser = userService.getLoginUser(request);
        
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        
        // 限制每页最多 20 个
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页最多 20 个");
        
        // 只查询自己的应用
        appQueryRequest.setAppOwnerId(loginUser.getId());
        
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        
        Page<AppVO> appVOPage = new Page<>(current, size, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVO(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选的应用列表（支持根据名称查询，每页最多 20 个）
     *
     * @param appQueryRequest
     * @return
     */
    @PostMapping("/list/page/featured")
    public BaseResponse<Page<AppVO>> listFeaturedAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        
        // 限制每页最多 20 个
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页最多 20 个");
        
        // 查询精选应用（优先级为99）
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        queryWrapper.and(APP.PRIORITY.eq(AppConstant.MAX_PRIORITY));

        Page<App> appPage = appService.page(new Page<>(current, size), queryWrapper);
        
        Page<AppVO> appVOPage = new Page<>(current, size, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVO(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        
        return ResultUtils.success(appVOPage);
    }

    /**
     * 根据 id 删除任意应用（管理员）
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        boolean result = appService.deleteAppWithHistory(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 更新任意应用（管理员）
     *
     * @param appUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        App app = new App();
        BeanUtils.copyProperties(appUpdateRequest, app);
        
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        
        return ResultUtils.success(true);
    }

    /**
     * 分页查询应用列表（管理员）
     *
     * @param appQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<App>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        
        return ResultUtils.success(appPage);
    }

    /**
     * 根据 id 查看应用详情（管理员）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> getAppById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        return ResultUtils.success(app);
    }
}
