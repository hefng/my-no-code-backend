package com.hefng.mynocodebackend.service;

import com.hefng.mynocodebackend.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;

public interface GitHubOAuthService {

    String getAuthorizationUrl(HttpServletRequest request);

    LoginUserVO handleCallback(String code, String state, HttpServletRequest request);
}
