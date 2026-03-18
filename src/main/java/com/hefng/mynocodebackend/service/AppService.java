package com.hefng.mynocodebackend.service;

import com.hefng.mynocodebackend.model.dto.app.AppQueryRequest;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 应用服务
 *
 * @author https://github.com/hefng
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用视图对象
     *
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用视图对象列表
     *
     * @param appList
     * @return
     */
    List<AppVO> getAppVO(List<App> appList);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
