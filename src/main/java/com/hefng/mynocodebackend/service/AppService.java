package com.hefng.mynocodebackend.service;

import com.hefng.mynocodebackend.model.dto.app.AppQueryRequest;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用服务
 *
 * @author https://github.com/hefng
 */
public interface AppService extends IService<App> {

    /**
     * 调用 AI 生成代码
     *
     * @param appId 应用id
     * @param userMessage 用户输入的消息
     * @param loginUser 当前登录用户
     * @return AI 生成的代码流
     */
    Flux<String> chatToGenCode(Long appId, String userMessage, User loginUser);

    /**
     * 部署应用
     *
     * @param appId 应用id
     * @param loginUser 当前登录用户
     * @return
     */
    String deployApp(Long appId, User loginUser);

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

    /**
     * 删除应用并关联删除其所有对话历史
     *
     * @param appId 应用id
     * @return 是否成功
     */
    boolean deleteAppWithHistory(Long appId);
}
