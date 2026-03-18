package com.hefng.mynocodebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.constant.CommonConstant;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.mapper.AppMapper;
import com.hefng.mynocodebackend.model.dto.app.AppQueryRequest;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.vo.AppVO;
import com.hefng.mynocodebackend.model.vo.UserVO;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.UserService;
import com.hefng.mynocodebackend.utils.SqlUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

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
}
