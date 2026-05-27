package com.hefng.mynocodebackend.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.config.GitHubOAuthConfig;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.vo.LoginUserVO;
import com.hefng.mynocodebackend.service.GitHubOAuthService;
import com.hefng.mynocodebackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.hefng.mynocodebackend.constant.UserConstant.USER_LOGIN_STATE;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthServiceImpl implements GitHubOAuthService {

    private static final String SALT = "yupi";
    private static final String STATE_KEY = "github_oauth_state";

    private final GitHubOAuthConfig gitHubOAuthConfig;
    private final UserService userService;

    @Override
    public String getAuthorizationUrl(HttpServletRequest request) {
        String state = IdUtil.simpleUUID();
        request.getSession().setAttribute(STATE_KEY, state);

        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + gitHubOAuthConfig.getClientId()
                + "&redirect_uri=" + URLEncoder.encode(gitHubOAuthConfig.getRedirectUri(), StandardCharsets.UTF_8)
                + "&state=" + state
                + "&scope=read:user";
        log.info("Generated GitHub OAuth authorize URL with state: {}", state);
        return url;
    }

    @Override
    public LoginUserVO handleCallback(String code, String state, HttpServletRequest request) {
        // 1. 校验 state
        String storedState = (String) request.getSession().getAttribute(STATE_KEY);
        if (storedState == null || !storedState.equals(state)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法请求，state 校验失败");
        }
        request.getSession().removeAttribute(STATE_KEY);

        // 2. 用 code 换 access_token
        String accessToken;
        try {
            String tokenUrl = "https://github.com/login/oauth/access_token";
            JSONObject tokenBody = new JSONObject();
            tokenBody.set("client_id", gitHubOAuthConfig.getClientId());
            tokenBody.set("client_secret", gitHubOAuthConfig.getClientSecret());
            tokenBody.set("code", code);
            tokenBody.set("redirect_uri", gitHubOAuthConfig.getRedirectUri());

            String tokenResponse = HttpUtil.createPost(tokenUrl)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(tokenBody.toString())
                    .execute()
                    .body();

            JSONObject tokenJson = JSONUtil.parseObj(tokenResponse);
            if (tokenJson.containsKey("error")) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "GitHub 授权失败: " + tokenJson.getStr("error_description", tokenJson.getStr("error")));
            }
            accessToken = tokenJson.getStr("access_token");
            if (accessToken == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取 GitHub access_token 失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub code 换 token 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "连接 GitHub 失败，请稍后重试");
        }

        // 3. 获取 GitHub 用户信息
        JSONObject githubUser;
        try {
            String userResponse = HttpUtil.createGet("https://api.github.com/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .execute()
                    .body();

            githubUser = JSONUtil.parseObj(userResponse);
            if (githubUser.containsKey("error") || githubUser.getLong("id") == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取 GitHub 用户信息失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取 GitHub 用户信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "连接 GitHub 失败，请稍后重试");
        }

        Long githubId = githubUser.getLong("id");
        String githubLogin = githubUser.getStr("login");
        String githubName = githubUser.getStr("name", githubLogin);
        String githubAvatarUrl = githubUser.getStr("avatar_url");

        // 4. 查找或创建本地用户
        User user = userService.getByGithubId(githubId);
        if (user == null) {
            user = new User();
            user.setUserAccount("github_" + githubId);
            user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + IdUtil.simpleUUID()).getBytes()));
            user.setUsername(githubName != null ? githubName : githubLogin);
            user.setUserAvatar(githubAvatarUrl);
            user.setGithubId(githubId);
            user.setUserRole("user");
            user.setAppMaxCount(3);
            user.setAppUsedCount(0);

            boolean saved = userService.save(user);
            if (!saved) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "自动注册失败，数据库错误");
            }
            log.info("GitHub OAuth 自动注册新用户: githubId={}, username={}", githubId, user.getUsername());
        }

        // 5. 记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        return userService.getLoginUserVO(user);
    }
}
