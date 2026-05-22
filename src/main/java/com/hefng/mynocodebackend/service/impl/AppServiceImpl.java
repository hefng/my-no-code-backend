package com.hefng.mynocodebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.hefng.mynocodebackend.ai.AiCodegenServiceFaced;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.service.AiCodeGenTypeRoutingService;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.constant.CommonConstant;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.langgraph4j.CodeGenWorkflowWithFlux;
import com.hefng.mynocodebackend.langgraph4j.enums.WorkflowOperationTypeEnum;
import com.hefng.mynocodebackend.manager.CosManager;
import com.hefng.mynocodebackend.mapper.AppMapper;
import com.hefng.mynocodebackend.model.dto.app.AppQueryRequest;
import com.hefng.mynocodebackend.model.dto.sse.ToolStreamEvent;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.enums.ChatMessageTypeEnum;
import com.hefng.mynocodebackend.model.enums.SseEventTypeEnum;
import com.hefng.mynocodebackend.model.vo.AppVO;
import com.hefng.mynocodebackend.model.vo.UserVO;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import com.hefng.mynocodebackend.service.UserService;
import com.hefng.mynocodebackend.utils.SqlUtils;
import com.hefng.mynocodebackend.utils.SseEventBuilder;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.hefng.mynocodebackend.model.table.AppTableDef.APP;

/**
 * 应用服务实现
 *
 * @author https://github.com/hefng
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private AiCodegenServiceFaced aiCodegenServiceFaced;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private CosManager cosManager;

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Value("${app.codegen.agent.enabled:false}")
    private boolean agentWorkflowEnabled;

    @Value("${app.codegen.agent.fallback-to-legacy:true}")
    private boolean agentWorkflowFallbackToLegacy;

    @Override
    public Flux<ServerSentEvent<String>> chatToGenCode(Long appId, String userMessage, Boolean isAgent, User loginUser) {
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NO_AUTH_ERROR, "用户未登录");
        return doChatToGenCode(appId, userMessage, isAgent, loginUser);
    }

    /**
     * 部署应用
     *
     * @param appId 应用id
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");

        // 2. 获取应用信息
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 校验用户是否能够部署该应用
        ThrowUtils.throwIf(!app.getAppOwnerId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");

        // 4. 校验应用是否已经部署
        String deployedKey = app.getDeployedKey();
        if (StrUtil.isBlank(deployedKey)) {
            deployedKey = IdUtil.getSnowflakeNextIdStr();
        }

        // 5. 构建源文件目录
        String codegenType = app.getCodegenType();
        String srcPath = AppConstant.CODEGEN_DIR + File.separator + codegenType + "_" + appId;

        // 6. 校验源文件目录是否存在
        File srcFile = new File(srcPath);
        ThrowUtils.throwIf(!srcFile.exists(), ErrorCode.NOT_FOUND_ERROR, "源文件不存在，无法部署");

        // 7. Vue 工程化项目：部署 dist 目录（npm run build 的产物）
        //    其他类型（html/multi_file）：直接部署源文件目录
        if (AppConstant.VUE_PROJECT_CODEGEN_TYPE.equals(codegenType)) {
            srcPath = srcPath + File.separator + "dist";
            srcFile = new File(srcPath);
            ThrowUtils.throwIf(!srcFile.exists(), ErrorCode.NOT_FOUND_ERROR,
                    "Vue 项目尚未构建完成（dist 目录不存在），请稍后再试");
        }

        // 8. 将源文件目录下的文件复制到部署目录
        String deployPath = AppConstant.DEPLOY_DIR + File.separator + deployedKey;
        try {
            File deployDir = new File(deployPath);
            if (AppConstant.VUE_PROJECT_CODEGEN_TYPE.equals(codegenType)) {
                // vue-project 只复制 dist/index.html 和 dist/assets
                File indexFile = new File(srcFile, "index.html");
                ThrowUtils.throwIf(!indexFile.exists(), ErrorCode.NOT_FOUND_ERROR, "dist/index.html 不存在，请先构建项目");
                FileUtil.copy(indexFile, new File(deployDir, "index.html"), true);
                File assetsDir = new File(srcFile, "assets");
                if (assetsDir.exists() && assetsDir.isDirectory()) {
                    FileUtil.copyContent(assetsDir, new File(deployDir, "assets"), true);
                }
            } else {
                FileUtil.copyContent(srcFile, deployDir, true);
            }
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败，复制文件出错");
        }

        // 9. 更新应用的 deployedKey 和 deployedTime
        app.setDeployedKey(deployedKey);
        app.setDeployedTime(LocalDateTime.now());
        boolean success = updateById(app);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "部署失败，更新应用信息失败");

        // 10. 返回可访问的 URL 地址
        String deployUrl = AppConstant.CODE_DEPLOY_HOST + deployedKey;

        // 11. 异步截图任务（虚拟线程），不阻塞部署接口响应
        final String finalDeployedKey = deployedKey;
        final Long finalAppId = appId;
        Thread.ofVirtual().name("screenshot-" + appId).start(() -> {
            try {
                asyncScreenshotAndUpdateCover(finalAppId, deployUrl, loginUser.getId());
            } catch (Exception e) {
                log.error("[Screenshot] 封面截图失败, appId={}", finalAppId, e);
            }
        });

        return deployUrl;
    }


    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtils.copyProperties(app, appVO);
        
        // 关联查询用户信息
        Long appOwnerId = app.getAppOwnerId();
        if (appOwnerId != null && appOwnerId > 0) {
            User user = userService.getById(appOwnerId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        
        return appVO;
    }

    @Override
    public List<AppVO> getAppVO(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        
        // 关联查询用户信息
        Set<Long> userIdSet = appList.stream()
                .map(App::getAppOwnerId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        
        // 填充信息
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtils.copyProperties(app, appVO);
            Long appOwnerId = app.getAppOwnerId();
            User user = null;
            if (userIdUserListMap.containsKey(appOwnerId)) {
                user = userIdUserListMap.get(appOwnerId).getFirst();
            }
            appVO.setUser(userService.getUserVO(user));
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String appDesc = appQueryRequest.getAppDesc();
        String codegenType = appQueryRequest.getCodegenType();
        Long appOwnerId = appQueryRequest.getAppOwnerId();
        Integer priority = appQueryRequest.getPriority();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(APP.ID.eq(id).when(id != null))
                .and(APP.APP_NAME.like(appName).when(StringUtils.isNotBlank(appName)))
                .and(APP.APP_DESC.like(appDesc).when(StringUtils.isNotBlank(appDesc)))
                .and(APP.CODEGEN_TYPE.eq(codegenType).when(StringUtils.isNotBlank(codegenType)))
                .and(APP.APP_OWNER_ID.eq(appOwnerId).when(appOwnerId != null))
                .and(APP.PRIORITY.eq(priority).when(priority != null));

        // 动态排序
        if (SqlUtils.validSortField(sortField)) {
            boolean asc = CommonConstant.SORT_ORDER_ASC.equals(sortOrder);
            queryWrapper.orderBy(sortField, asc);
        }

        return queryWrapper;
    }

    @Override
    public boolean deleteAppWithHistory(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        // 先关联删除对话历史
        chatHistoryService.removeByAppId(appId);
        // 再删除应用本身
        return this.removeById(appId);
    }

    private Flux<ServerSentEvent<String>> doChatToGenCode(Long appId, String userMessage, Boolean isAgent, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");

        // 2. 获取应用信息
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getAppOwnerId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");

        // 3. 根据原始用户输入判定操作类型：如果用户输入的消息与应用的 initPrompt 相同，则判定为 CREATE 操作；否则为 MODIFY 操作
        WorkflowOperationTypeEnum operationType = userMessage.equals(app.getInitPrompt())
                ? WorkflowOperationTypeEnum.CREATE
                : WorkflowOperationTypeEnum.MODIFY;

        // 4. 如果用户输入的消息为空，使用应用的 initPrompt 作为生成输入
        String initPrompt = app.getInitPrompt();
        if (StrUtil.isBlank(userMessage)) {
            userMessage = initPrompt;
        }
        ThrowUtils.throwIf(StringUtils.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        final String finalUserMessage = userMessage;
        chatHistoryService.saveChatMessage(appId, loginUser.getId(), finalUserMessage, ChatMessageTypeEnum.USER.getValue());
        CodegenTypeEnum codegenTypeEnum = resolveCodegenType(app);

        // 5. 传统路径（默认）
        Flux<String> rawFlux;
        if (!Boolean.TRUE.equals(isAgent)) {
            rawFlux = aiCodegenServiceFaced.generateAndSaveCodeWithStream(finalUserMessage, codegenTypeEnum, appId);
            return buildSseResponse(rawFlux, appId, loginUser.getId());
        }
        // 6. Agent 路径受配置开关控制，便于灰度和回滚
        if (!agentWorkflowEnabled) {
            log.info("Agent 工作流未启用，回退传统代码生成，appId={}", appId);
            rawFlux = aiCodegenServiceFaced.generateAndSaveCodeWithStream(finalUserMessage, codegenTypeEnum, appId);
            return buildSseResponse(rawFlux, appId, loginUser.getId());
        }
        log.info("Agent 工作流执行，appId={}, operationType={}, codegenType={}",
                appId, operationType, codegenTypeEnum.getType());

        rawFlux = new CodeGenWorkflowWithFlux()
                .executeWorkflowWithFlux(finalUserMessage, appId, operationType, codegenTypeEnum);
        if (!agentWorkflowFallbackToLegacy) {
            return buildSseResponse(rawFlux, appId, loginUser.getId());
        }
        Flux<String> resilientFlux = rawFlux.onErrorResume(error -> {
            log.error("Agent 工作流执行失败，回退传统代码生成，appId={}, operationType={}", appId, operationType, error);
            return aiCodegenServiceFaced.generateAndSaveCodeWithStream(finalUserMessage, codegenTypeEnum, appId);
        });
        return buildSseResponse(resilientFlux, appId, loginUser.getId());
    }

    private CodegenTypeEnum resolveCodegenType(App app) {
        String initPrompt = app.getInitPrompt();
        if (app.getCodegenType() == null) {
            CodegenTypeEnum routingResult = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
            String codegenType = routingResult != null ? routingResult.getType() : null;
            // 降级策略：如果 AI 推荐失败，则默认生成 HTML 代码
            app.setCodegenType(Objects.requireNonNullElse(codegenType, AppConstant.DEFAULT_CODEGEN_TYPE));
        }
        String finalCodegenType = app.getCodegenType();
        CodegenTypeEnum codegenTypeEnum = CodegenTypeEnum.getByType(finalCodegenType);
        if (codegenTypeEnum == null) {
            log.warn("未知的 codegenType={}，appId={}，降级为 HTML 生成", finalCodegenType, app.getId());
            return CodegenTypeEnum.HTML;
        }
        return codegenTypeEnum;
    }

    private Flux<ServerSentEvent<String>> buildSseResponse(Flux<String> rawFlux, Long appId, Long userId) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        log.info("chatToGenCode 请求开始, traceId={}, appId={}, userId={}", traceId, appId, userId);

        StringBuilder aiResponseBuilder = new StringBuilder();
        Flux<ServerSentEvent<String>> messageFlux = rawFlux
                .doOnNext(chunk -> appendAiResponse(aiResponseBuilder, chunk))
                .doOnComplete(() -> persistAiResponse(appId, userId, aiResponseBuilder.toString(), traceId))
                .doOnError(error -> persistAiErrorResponse(appId, userId, aiResponseBuilder.toString(), error, traceId))
                .onErrorResume(error -> Flux.just(
                        SseEventBuilder.build(
                                SseEventTypeEnum.WORKFLOW_ERROR,
                                StringUtils.defaultIfBlank(error.getMessage(), "对话失败")
                        )
                ))
                .doFinally(signalType -> log.info("chatToGenCode 请求结束, traceId={}, appId={}, signal={}",
                        traceId, appId, signalType.name()))
                .map(this::toServerSentEvent);

        Flux<ServerSentEvent<String>> doneFlux = Flux.just(
                ServerSentEvent.<String>builder()
                        .event(SseEventTypeEnum.DONE.getValue())
                        .data("")
                        .build()
        );
        return Flux.concat(messageFlux, doneFlux);
    }

    private void appendAiResponse(StringBuilder aiResponseBuilder, String chunk) {
        try {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(chunk);
            String event = json.getStr("event", "");
            if (SseEventTypeEnum.DONE.getValue().equals(event)) {
                return;
            }
            if (SseEventTypeEnum.TOOL.getValue().equals(event)) {
                appendCompactToolResponse(aiResponseBuilder, json);
                return;
            }
            if (SseEventTypeEnum.WORKFLOW_STEP.getValue().equals(event)) {
                cn.hutool.json.JSONObject stepPayload = json.getJSONObject("d");
                if (stepPayload != null) {
                    Integer step = stepPayload.getInt("step");
                    String title = stepPayload.getStr("title", "");
                    String status = stepPayload.getStr("status", "");
                    if (step != null && StringUtils.isNotBlank(title)) {
                        aiResponseBuilder.append("第")
                                .append(step)
                                .append("步：")
                                .append(title)
                                .append("... ");
                        if ("completed".equals(status)) {
                            aiResponseBuilder.append("已完成");
                        } else {
                            aiResponseBuilder.append(StringUtils.defaultIfBlank(status, "已完成"));
                        }
                        aiResponseBuilder.append("\n");
                    }
                }
                return;
            }
            String content = json.getStr("d", "");
            if (!content.isEmpty()) {
                aiResponseBuilder.append(content);
            }
        } catch (Exception ignored) {
            aiResponseBuilder.append(chunk);
        }
    }

    private void appendCompactToolResponse(StringBuilder aiResponseBuilder, cn.hutool.json.JSONObject json) {
        cn.hutool.json.JSONObject toolJson = json.getJSONObject("d");
        if (toolJson == null) {
            String rawToolJson = json.getStr("d", "");
            if (StringUtils.isBlank(rawToolJson)) {
                return;
            }
            try {
                toolJson = cn.hutool.json.JSONUtil.parseObj(rawToolJson);
            } catch (Exception e) {
                log.warn("解析工具事件失败，chunk={}", json, e);
                return;
            }
        }

        ToolStreamEvent toolStreamEvent = toolJson.toBean(ToolStreamEvent.class);
        if (toolStreamEvent == null) {
            return;
        }

        cn.hutool.json.JSONObject compact = new cn.hutool.json.JSONObject();
        compact.put("phase", toolStreamEvent.getPhase());
        compact.put("toolCallId", toolStreamEvent.getToolCallId());
        compact.put("toolName", toolStreamEvent.getToolName());
        compact.put("message", compactToolMessage(toolStreamEvent.getMessage()));
        aiResponseBuilder.append(cn.hutool.json.JSONUtil.toJsonStr(compact));
    }

    private String compactToolMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return "";
        }

        List<String> meaningfulLines = new ArrayList<>();
        for (String line : message.split("\\R")) {
            String trimmed = StringUtils.trimToEmpty(line);
            if (StringUtils.isBlank(trimmed) || trimmed.startsWith("```")) {
                continue;
            }
            meaningfulLines.add(trimmed);
            if (meaningfulLines.size() >= 2) {
                break;
            }
        }

        String summary = meaningfulLines.isEmpty()
                ? StringUtils.normalizeSpace(message)
                : String.join(" ", meaningfulLines);
        return StringUtils.abbreviate(summary, 160);
    }

    private void persistAiResponse(Long appId, Long userId, String aiResponse, String traceId) {
        if (StringUtils.isBlank(aiResponse)) {
            return;
        }
        try {
            chatHistoryService.saveChatMessage(appId, userId, aiResponse, ChatMessageTypeEnum.AI.getValue());
        } catch (Exception e) {
            log.error("保存 AI 回答到对话历史失败, traceId={}, appId={}", traceId, appId, e);
        }
    }

    private void persistAiErrorResponse(Long appId, Long userId, String partialResponse, Throwable error, String traceId) {
        String errorMessage = StringUtils.defaultIfBlank(error.getMessage(), "工作流执行失败");
        String persistMessage = StringUtils.isNotBlank(partialResponse)
                ? partialResponse + "\n[ERROR] " + errorMessage
                : "[ERROR] " + errorMessage;
        try {
            chatHistoryService.saveChatMessage(appId, userId, persistMessage, ChatMessageTypeEnum.AI.getValue());
        } catch (Exception e) {
            log.error("保存失败结果到对话历史失败, traceId={}, appId={}", traceId, appId, e);
        }
    }

    private ServerSentEvent<String> toServerSentEvent(String chunk) {
        String sseEvent = "message";
        try {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(chunk);
            String eventField = json.getStr("event");
            if (StringUtils.isNotBlank(eventField)) {
                sseEvent = eventField;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return ServerSentEvent.<String>builder()
                .event(sseEvent)
                .data(chunk)
                .build();
    }

    /**
     * 截图并上传封面，更新应用 appCover 字段
     *
     * @param appId   应用id
     * @param pageUrl 要截图的部署页面地址
     * @param userId  当前用户id，用于构建 COS 存储路径
     */
    private void asyncScreenshotAndUpdateCover(Long appId, String pageUrl, Long userId) {
        String coverName = appId + ".webp";
        String screenshotPath = AppConstant.DEFAULT_DIR + "screenshots" + File.separator + "pic" + File.separator + coverName;
        String cosKey = "covers/" + userId + "/" + coverName;
        String coverUrl = cosManager.screenshotAndUpload(pageUrl, screenshotPath, cosKey);
        // 上传到 cos 之后, 清理本地文件
        if (!FileUtil.exist(screenshotPath)) {
            log.warn("[Screenshot] 本地截图文件不存在, appId={}, path={}", appId, screenshotPath);
            return;
        }
        FileUtil.del(screenshotPath);

        App coverUpdate = new App();
        coverUpdate.setId(appId);
        coverUpdate.setAppCover(coverUrl);
        updateById(coverUpdate);

        log.info("[Screenshot] 封面截图完成, appId={}, coverUrl={}", appId, coverUrl);
    }
}
