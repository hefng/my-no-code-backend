package com.hefng.mynocodebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubOAuthConfig {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String frontendBaseUrl = "http://localhost:5173";
}
