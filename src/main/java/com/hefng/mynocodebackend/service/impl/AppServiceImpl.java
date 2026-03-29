package com.hefng.mynocodebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.hefng.mynocodebackend.ai.AiCodegenServiceFaced;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.constant.CommonConstant;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.mapper.AppMapper;
import com.hefng.mynocodebackend.model.dto.app.AppQueryRequest;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.vo.AppVO;
import com.hefng.mynocodebackend.model.vo.UserVO;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.ChatHistoryService;
import com.hefng.mynocodebackend.service.UserService;
import com.hefng.mynocodebackend.utils.SqlUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    public Flux<String> chatToGenCode(Long appId, String userMessage, String codegenType, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(StringUtils.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "用户消息不能为空");

        // 2. 获取应用信息
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getAppOwnerId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");

        // 3. 如果用户输入的消息为空，使用应用的 initPrompt 作为用户输入的消息
        String initPrompt = app.getInitPrompt();
        if (StrUtil.isBlank(userMessage)) {
            userMessage = initPrompt;
        }

        // 4. 若前端传入了 codegenType，校验合法性后覆盖应用原有类型并持久化
        // 这样下次进入应用时预览 URL 和生成逻辑都能保持一致，不会出现类型漂移
        if (StringUtils.isNotBlank(codegenType)) {
            CodegenTypeEnum requestedType = CodegenTypeEnum.getByType(codegenType);
            ThrowUtils.throwIf(requestedType == null, ErrorCode.PARAMS_ERROR,
                    "不支持的代码生成类型: " + codegenType + "，合法值为 html/multi-file/vue-project");
            // 仅在类型发生变化时才执行更新，避免无意义的 DB 写操作
            if (!codegenType.equals(app.getCodegenType())) {
                App updateApp = new App();
                updateApp.setId(appId);
                updateApp.setCodegenType(codegenType);
                updateById(updateApp);
                app.setCodegenType(codegenType);
            }
        }

        // 5. 根据最终确定的 codegenType 路由到对应的生成策略
        String finalCodegenType = app.getCodegenType();
        CodegenTypeEnum codegenTypeEnum = CodegenTypeEnum.getByType(finalCodegenType);
        if (codegenTypeEnum == null) {
            // 兜底：未知类型默认走 HTML 生成，避免因配置错误导致整个请求失败
            log.warn("未知的 codegenType={}，appId={}，降级为 HTML 生成", finalCodegenType, appId);
            codegenTypeEnum = CodegenTypeEnum.HTML;
        }
        return aiCodegenServiceFaced.generateAndSaveCodeWithStream(userMessage, codegenTypeEnum, appId);
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
        return AppConstant.CODE_DEPLOY_HOST + deployedKey;
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
}
