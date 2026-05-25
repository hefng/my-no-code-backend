package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.config.GitHubOAuthConfig;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.model.vo.LoginUserVO;
import com.hefng.mynocodebackend.service.GitHubOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthController {

    private final GitHubOAuthService gitHubOAuthService;
    private final GitHubOAuthConfig gitHubOAuthConfig;

    /**
     * GitHub OAuth 授权入口，重定向用户到 GitHub 的授权页面
     * @param request
     * @param response
     * @throws IOException
     */
    @GetMapping("/user/oauth/github/authorize")
    public void authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = gitHubOAuthService.getAuthorizationUrl(request);
        response.sendRedirect(url);
    }

    /**
     * GitHub OAuth 回调处理，处理 GitHub 返回的授权结果
     * @param code GitHub 返回的授权码
     * @param state GitHub 返回的状态参数
     * @param request
     * @param response
     * @throws IOException
     */
    @GetMapping("/user/oauth/github/callback")
    public void callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String frontendBaseUrl = gitHubOAuthConfig.getFrontendBaseUrl();

        if (code == null) {
            response.sendRedirect(frontendBaseUrl + "/user/login?error="
                    + URLEncoder.encode("授权已取消", StandardCharsets.UTF_8));
            return;
        }

        try {
            LoginUserVO loginUserVO = gitHubOAuthService.handleCallback(code, state, request);
            log.info("GitHub OAuth 登录成功: userId={}, username={}", loginUserVO.getId(), loginUserVO.getUsername());
            response.sendRedirect(frontendBaseUrl + "/");
        } catch (BusinessException e) {
            log.warn("GitHub OAuth 回调处理失败: code={}, message={}", e.getCode(), e.getMessage());
            response.sendRedirect(frontendBaseUrl + "/user/login?error="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
