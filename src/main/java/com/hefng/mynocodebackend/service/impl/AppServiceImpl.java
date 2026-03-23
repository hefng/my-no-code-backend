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
    public Flux<String> chatToGenCode(Long appId, String userMessage, User loginUser) {
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
            // 为空代表第一次对话，使用应用的 initPrompt 作为用户输入的消息
            userMessage = initPrompt;
        }

        // 4. 调用 AI 生成代码 返回 AI 生成的代码流
        // todo 暂时默认使用 HTML 生成, 后续可以根据 app.getCodegenType() 来动态判断生成的代码类型
        return aiCodegenServiceFaced.generateAndSaveCodeWithStream(userMessage, CodegenTypeEnum.HTML, appId);
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

        // 7. 将源文件目录下的文件复制到部署目录
        String deployPath = AppConstant.DEPLOY_DIR + File.separator + deployedKey;
        try {
            FileUtil.copyContent(srcFile, new File(deployPath), true);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败，复制文件出错");
        }

        // 8. 更新应用的 deployedKey 和 deployedTime
        app.setDeployedKey(deployedKey);
        app.setDeployedTime(LocalDateTime.now());
        boolean success = updateById(app);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "部署失败，更新应用信息失败");

        // 9. 返回可访问的 URL 地址
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
