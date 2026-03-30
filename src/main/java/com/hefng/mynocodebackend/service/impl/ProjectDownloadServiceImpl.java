package com.hefng.mynocodebackend.service.impl;

import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 项目下载服务实现类
 *
 * @author hefng
 */
@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 需要过滤的文件和目录名称
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     * 需要过滤的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    /**
     * 下载项目文件并通过 HTTP 响应返回给客户端
     *
     * @param projectPath      项目目录路径
     * @param downloadFileName 下载文件名（不含 .zip 后缀）
     * @param response         HTTP 响应
     */
    @Override
    public void downloadProject(String projectPath, String downloadFileName, HttpServletResponse response) {
        // 基础校验
        Path rootPath = Paths.get(projectPath);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "项目路径不存在或不是目录: " + projectPath);
        }

        // 设置文件下载响应头
        String zipFileName = downloadFileName.endsWith(".zip") ? downloadFileName : downloadFileName + ".zip";
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");

        // 将项目文件打包成 ZIP 并写入响应输出流
        try (OutputStream os = response.getOutputStream();
             ZipOutputStream zos = new ZipOutputStream(os)) {
            Files.walk(rootPath)
                    .filter(filePath -> !filePath.equals(rootPath))
                    // 忽略不需要下载的文件和目录
                    .filter(filePath -> !shouldIgnore(rootPath, filePath))
                    .forEach(filePath -> {
                        try {
                            String entryName = rootPath.relativize(filePath).toString().replace("\\", "/");
                            if (Files.isDirectory(filePath)) {
                                zos.putNextEntry(new ZipEntry(entryName + "/"));
                                zos.closeEntry();
                            } else {
                                zos.putNextEntry(new ZipEntry(entryName));
                                Files.copy(filePath, zos);
                                zos.closeEntry();
                            }
                        } catch (IOException e) {
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打包文件失败: " + filePath + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.error("项目下载失败, projectPath={}", projectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目下载失败" + e);
        }
    }

    /**
     * 判断文件是否应该被忽略（不包含在下载包中）
     * @param projectPath 项目根目录路径
     * @param filePath 当前文件路径
     * @return
     */
    private boolean shouldIgnore(Path projectPath, Path filePath) {
        Path relativizePath = projectPath.relativize(filePath);
        for (Path path : relativizePath) {
            String name = path.getFileName().toString();
            // 检查是否在忽略名称列表中
            if (IGNORED_NAMES.contains(name)) {
                return true;
            }
            // 检查文件扩展名
            if (IGNORED_EXTENSIONS.stream().anyMatch(name::endsWith)) {
                return true;
            }
        }
        return false;
    }
}

