package com.hefng.mynocodebackend.manager;

import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.utils.SeleniumScreenshotUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS对象存储管理器
 *
 * @author hefng
 */
@Slf4j
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    private SeleniumScreenshotUtil seleniumScreenshotUtil;

    /**
     * 上传图片并获取图片信息
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
            return cosClient.putObject(putObjectRequest);
        } catch (Exception e) {
            // 处理服务端异常
            log.error("上传COS对象失败，服务端异常：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 下载对象存储中的文件
     *
     * @param key
     * @return
     */
    public COSObject getObject(String key) {
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
            return cosClient.getObject(getObjectRequest);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取COS对象失败：" + e.getMessage());
        }
    }

    /**
     * 对指定 URL 截图并上传至 COS，返回可访问的图片 URL
     *
     * @param pageUrl        要截图的页面地址
     * @param screenshotPath 截图本地保存路径
     * @param cosKey         COS 存储路径，如 covers/{userId}/{fileName}
     * @return 可公开访问的图片 URL
     */
    public String screenshotAndUpload(String pageUrl, String screenshotPath, String cosKey) {
        try {
            File screenshotFile = seleniumScreenshotUtil.screenshot(pageUrl, screenshotPath);
            putPictureObject(cosKey, screenshotFile);
            return cosClientConfig.getHost() + "/" + cosKey;
        } catch (Exception e) {
            log.error("截图并上传COS失败：{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "截图并上传COS失败：" + e.getMessage());
        }
    }

    /**
     * 删除对象存储中的文件
     *
     * @param key
     */
    public void deleteObject(String key) {
        try {
            cosClient.deleteObject(cosClientConfig.getBucket(), key);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除COS对象失败：" + e.getMessage());
        }
    }
}