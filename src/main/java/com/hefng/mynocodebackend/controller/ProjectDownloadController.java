package com.hefng.mynocodebackend.controller;

import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.service.ProjectDownloadService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * 项目下载控制层
 *
 * @author hefng
 */
@Slf4j
@RestController
@RequestMapping("/project")
public class ProjectDownloadController {

    @Resource
    private ProjectDownloadService projectDownloadService;

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
}
