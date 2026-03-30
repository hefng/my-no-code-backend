package com.hefng.mynocodebackend.utils;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SeleniumScreenshotUtilTest {

    @Resource
    private SeleniumScreenshotUtil seleniumScreenshotUtil;

    @Test
    void screenshot() {
        String url = "https://www.baidu.com";
        String destPath = System.getProperty("user.dir") + "/tmp/pic/cover.png";
        try {
            File screenshot = seleniumScreenshotUtil.screenshot(url, destPath);
            assertTrue(screenshot.exists(), "截图文件应该存在");
            assertTrue(screenshot.length() > 0, "截图文件应该非空");
        } catch (Exception e) {
            fail("截图过程中发生异常: " + e.getMessage());
        }
    }
}
