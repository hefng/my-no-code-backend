package com.hefng.mynocodebackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 应用生成根目录（用于浏览）
    private static final String PREVIEW_ROOT_DIR = System.getProperty("user.dir") + "/tmp/codegen";

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{codeGenType}_{appId}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String codeGenType,
            @PathVariable Long appId,
            HttpServletRequest request) {
        try {
            Path rootPath = Paths.get(PREVIEW_ROOT_DIR).toAbsolutePath().normalize();
            Path appDirPath = rootPath.resolve(codeGenType + "_" + appId).normalize();

            // 1) 防止越权访问（目录穿越）
            if (!appDirPath.startsWith(rootPath) || !Files.exists(appDirPath) || !Files.isDirectory(appDirPath)) {
                return ResponseEntity.notFound().build();
            }

            // 2) 解析通配符路径（/** 之后的部分）
            // request.getRequestURI 形如: /api/static/html_392477938542854144/assets/app.js
            String requestUri = request.getRequestURI();
            String mappingPrefix = request.getContextPath() + "/api/static/" + codeGenType + "_" + appId;
            String relativePath = requestUri.startsWith(mappingPrefix)
                    ? requestUri.substring(mappingPrefix.length())
                    : "";

            // 去掉开头 /
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            // 3) 空路径或目录路径 -> index.html
            Path targetPath;
            if (relativePath.isEmpty()) {
                targetPath = appDirPath.resolve("index.html").normalize();
            } else {
                targetPath = appDirPath.resolve(relativePath).normalize();

                // 再次防止越权访问
                if (!targetPath.startsWith(appDirPath)) {
                    return ResponseEntity.badRequest().build();
                }

                // 如果是目录，自动补 index.html
                if (Files.exists(targetPath) && Files.isDirectory(targetPath)) {
                    targetPath = targetPath.resolve("index.html").normalize();
                }
            }

            // 4) 文件不存在时：SPA 路由回退到 index.html
            if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
                Path fallback = appDirPath.resolve("index.html").normalize();
                if (!Files.exists(fallback) || Files.isDirectory(fallback)) {
                    return ResponseEntity.notFound().build();
                }
                targetPath = fallback;
            }

            Resource resource = new UrlResource(targetPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = getContentTypeWithCharset(targetPath.toString());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
