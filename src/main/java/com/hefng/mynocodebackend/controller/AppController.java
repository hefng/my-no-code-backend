package com.hefng.mynocodebackend.controller;

import cn.hutool.core.util.RandomUtil;
import com.hefng.mynocodebackend.annotation.AuthCheck;
import com.hefng.mynocodebackend.common.BaseResponse;
import com.hefng.mynocodebackend.common.DeleteRequest;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.common.ResultUtils;
import com.hefng.mynocodebackend.constant.UserConstant;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.model.dto.app.AppAddRequest;
import com.hefng.mynocodebackend.model.dto.app.AppQueryRequest;
import com.hefng.mynocodebackend.model.dto.app.AppUpdateMyRequest;
import com.hefng.mynocodebackend.model.dto.app.AppUpdateRequest;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.vo.AppVO;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static com.hefng.mynocodebackend.model.table.AppTableDef.APP;

/**
 * 应用控制层
 *
 * @author https://github.com/hefng
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    // region 用户功能

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
        
        // 校验参数
        String appName = appAddRequest.getAppName();
        String codegenType = appAddRequest.getCodegenType();
        if (StringUtils.isAnyBlank(appName, codegenType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称和代码生成类型不能为空");
        }
        
        User loginUser = userService.getLoginUser(request);
        
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);
        app.setAppOwnerId(loginUser.getId());
        
        // 生成唯一的部署key
        String deployedKey = RandomUtil.randomString(16);
        app.setDeployedKey(deployedKey);
        app.setDeployedTime(LocalDateTime.now());
        
        boolean result = appService.save(app);
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
        
        boolean result = appService.removeById(appId);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 查看应用详情
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
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
        
        // 查询精选应用（优先级大于 0）
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        queryWrapper.and(APP.PRIORITY.gt(0));
        queryWrapper.orderBy(APP.PRIORITY, false); // 按优先级降序
        
        Page<App> appPage = appService.page(new Page<>(current, size), queryWrapper);
        
        Page<AppVO> appVOPage = new Page<>(current, size, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVO(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        
        return ResultUtils.success(appVOPage);
    }

    // endregion

    // region 管理员功能

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
        
        boolean result = appService.removeById(deleteRequest.getId());
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

    // endregion
}
