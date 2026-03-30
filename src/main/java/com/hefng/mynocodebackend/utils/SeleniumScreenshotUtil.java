package com.hefng.mynocodebackend.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.util.Iterator;

/**
 * Selenium 截图工具类
 * <p>
 * 通过 {@link WebDriverPool} 复用 ChromeDriver 实例，避免每次截图重复初始化驱动。
 * <p>
 * 使用示例（注入后调用）：
 * <pre>
 *   File screenshot = seleniumScreenshotUtil.screenshot("http://localhost/abc123", "/tmp/cover.png");
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeleniumScreenshotUtil {

    /** 页面加载等待时间（秒） */
    private static final int PAGE_LOAD_TIMEOUT_SECONDS = 15;

    /** 截图前等待页面渲染的时间（毫秒），给 JS 框架留出渲染时间 */
    private static final long RENDER_WAIT_MS = 2000;

    private final WebDriverPool webDriverPool;

    /**
     * 对指定 URL 进行截图，保存到 destPath（自动转为 .webp 格式）
     *
     * @param url      要截图的页面地址
     * @param destPath 截图保存的完整文件路径（含文件名，如 /tmp/cover.png）
     * @return 保存后的截图文件
     * @throws RuntimeException 截图失败时抛出
     */
    public File screenshot(String url, String destPath) {
        WebDriver driver = webDriverPool.borrowDriver();
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT_SECONDS));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            log.info("[Screenshot] 开始截图: url={}, dest={}", url, destPath);
            driver.get(url);

            // 等待 JS 渲染完成
            Thread.sleep(RENDER_WAIT_MS);

            File tempFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            if (!destPath.endsWith(".webp")) {
                destPath = destPath + ".webp";
            }
            File destFile = new File(destPath);

            // 确保目标目录存在
            File parentDir = destFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean success = parentDir.mkdirs();
                if (!success) {
                    log.warn("[Screenshot] 创建目录失败，可能导致截图保存失败: {}", parentDir.getAbsolutePath());
                }
            }

            compressImage(tempFile, destFile, 0.3f);
            log.info("[Screenshot] 截图成功: url={}, dest={}", url, destPath);
            return destFile;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("截图等待被中断: " + url, e);
        } catch (Exception e) {
            throw new RuntimeException("截图失败: url=" + url + ", dest=" + destPath, e);
        } finally {
            webDriverPool.returnDriver(driver);
        }
    }

    /**
     * 将源图片转换为 WebP 格式并按指定质量压缩，保存到目标文件
     * 依赖 webp-imageio 库（org.sejda.imageio:webp-imageio）注册 WebP ImageIO SPI
     */
    private static void compressImage(File src, File dest, float quality) throws Exception {
        BufferedImage image = ImageIO.read(src);
        BufferedImage argbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        argbImage.createGraphics().drawImage(image, 0, 0, null);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (!writers.hasNext()) {
            throw new RuntimeException("找不到 WebP ImageWriter，请确认 webp-imageio 依赖已引入");
        }
        ImageWriter writer = writers.next();
        com.luciad.imageio.webp.WebPWriteParam param = new com.luciad.imageio.webp.WebPWriteParam(writer.getLocale());
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType(param.getCompressionTypes()[com.luciad.imageio.webp.WebPWriteParam.LOSSY_COMPRESSION]);
        param.setCompressionQuality(quality);

        try (FileImageOutputStream output = new FileImageOutputStream(dest)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(argbImage, null, null), param);
        } finally {
            writer.dispose();
        }
        log.info("[Screenshot] 图片转换为 WebP 完成: quality={}, dest={}", quality, dest.getAbsolutePath());
    }
}
