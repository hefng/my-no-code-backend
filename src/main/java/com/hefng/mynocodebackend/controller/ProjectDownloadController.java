package com.hefng.mynocodebackend.controller;

import cn.hutool.core.io.FileUtil;
import com.hefng.mynocodebackend.common.BaseResponse;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.common.ResultUtils;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.model.dto.project.ProjectFileEditRequest;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.service.AppService;
import com.hefng.mynocodebackend.service.ProjectDownloadService;
import com.hefng.mynocodebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 项目文件控制层
 *
 * @author hefng
 */
@Slf4j
@RestController
@RequestMapping("/project")
public class ProjectDownloadController {

    @Resource
    private ProjectDownloadService projectDownloadService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 下载项目文件（ZIP 压缩包）
     * 根据 appId 和 codegenType 自动推导项目目录路径
     *
     * @param appId       应用 id
     * @param codegenType 代码生成类型（html / multi-file / vue-project）
     * @param response    HTTP 响应
     */
    @GetMapping("/download")
    public void downloadProject(@RequestParam Long appId,
                                @RequestParam String codegenType,
                                HttpServletResponse response) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(codegenType == null || codegenType.isBlank(), ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        // 路径规则与 VueProjectFileSaveTool 保持一致：{CODEGEN_DIR}/{codegenType}_{appId}
        String dirName = codegenType + "_" + appId;
        String projectPath = AppConstant.CODEGEN_DIR + dirName;
        projectDownloadService.downloadProject(projectPath, dirName, response);
    }

    /**
     * 直接编辑项目文件（不经过 AI，用于可视化编辑器直接修改）
     * <p>
     * 将文件中的 oldContent 精确替换为 newContent，仅应用所有者可操作。
     *
     * @param request HTTP 请求（用于获取登录用户）
     * @param body    编辑请求体
     * @return 操作结果
     */
    @PostMapping("/file/edit")
    public BaseResponse<Boolean> editProjectFile(@RequestBody ProjectFileEditRequest body,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(body == null, ErrorCode.PARAMS_ERROR);
        Long appId = body.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不合法");
        ThrowUtils.throwIf(StringUtils.isBlank(body.getRelativePath()), ErrorCode.PARAMS_ERROR, "文件路径不能为空");
        ThrowUtils.throwIf(body.getOldContent() == null || body.getNewContent() == null,
                ErrorCode.PARAMS_ERROR, "oldContent 和 newContent 不能为空");

        // 鉴权：仅应用所有者可修改
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getAppOwnerId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改此应用的文件");
        }

        // 构建并校验路径（防止路径穿越）
        String projectDir = AppConstant.CODEGEN_DIR + app.getCodegenType() + "_" + appId;
        Path projectDirPath = Paths.get(projectDir).normalize();
        Path resolvedPath = projectDirPath.resolve(body.getRelativePath()).normalize();
        if (!resolvedPath.startsWith(projectDirPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法文件路径");
        }

        File file = resolvedPath.toFile();
        ThrowUtils.throwIf(!file.exists(), ErrorCode.NOT_FOUND_ERROR, "文件不存在: " + body.getRelativePath());

        try {
            String content = FileUtil.readString(file, StandardCharsets.UTF_8);
            if (!content.contains(body.getOldContent())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未找到匹配的原始内容，请确认内容是否与文件完全一致");
            }
            String updated = content.replace(body.getOldContent(), body.getNewContent());
            FileUtil.writeString(updated, file, StandardCharsets.UTF_8);
            log.info("直接编辑文件成功，appId={}, path={}", appId, resolvedPath);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("直接编辑文件失败，appId={}, path={}", appId, resolvedPath, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件写入失败: " + e.getMessage());
        }

        return ResultUtils.success(true);
    }
}
