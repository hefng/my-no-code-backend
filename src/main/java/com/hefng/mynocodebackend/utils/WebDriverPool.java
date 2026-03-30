package com.hefng.mynocodebackend.utils;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebDriver 连接池
 * <p>
 * 维护一组无头 ChromeDriver 实例，按需借用和归还，避免每次截图重复初始化驱动。
 * 池满时借用操作会阻塞等待，超时后抛出异常。
 */
@Slf4j
@Component
public class WebDriverPool {

    /** 池中最大 driver 数量 */
    @Value("${webdriver.pool.max-size}")
    private int maxPoolSize;

    /** 借用 driver 的最大等待时间（秒） */
    @Value("${webdriver.pool.borrow-timeout-seconds}")
    private int borrowTimeoutSeconds;

    /** 截图窗口宽度 */
    @Value("${webdriver.window.width}")
    private int width;

    /** 截图窗口高度 */
    @Value("${webdriver.window.height}")
    private int height;

    /** 空闲 driver 队列 */
    private BlockingQueue<WebDriver> idlePool;

    /** 当前池中已创建的 driver 总数（含借出的） */
    private final AtomicInteger totalCreated = new AtomicInteger(0);

    /** 初始化标志，懒加载，首次借用时初始化 */
    private volatile boolean initialized = false;
    private final Object initLock = new Object();

    /**
     * 从池中借用一个 WebDriver。
     * 若池中有空闲实例则直接返回；若未达上限则新建；否则阻塞等待直到超时。
     *
     * @return 可用的 WebDriver 实例
     * @throws RuntimeException 等待超时或中断时抛出
     */
    public WebDriver borrowDriver() {
        ensureInitialized();
        // 先尝试从空闲队列取
        WebDriver driver = idlePool.poll();
        if (driver != null) {
            log.debug("[WebDriverPool] 从池中借用 driver，当前空闲数: {}", idlePool.size());
            return driver;
        }
        // 空闲队列为空，尝试新建（未超上限）
        if (totalCreated.get() < maxPoolSize) {
            if (totalCreated.incrementAndGet() <= maxPoolSize) {
                log.info("[WebDriverPool] 新建 driver，当前总数: {}", totalCreated.get());
                return createDriver();
            } else {
                // 超出上限，回退计数
                totalCreated.decrementAndGet();
            }
        }
        // 已达上限，阻塞等待归还
        log.debug("[WebDriverPool] 池已满，等待空闲 driver，超时: {}s", borrowTimeoutSeconds);
        try {
            driver = idlePool.poll(borrowTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("[WebDriverPool] 等待 driver 被中断", e);
        }
        if (driver == null) {
            throw new RuntimeException("[WebDriverPool] 等待 driver 超时（" + borrowTimeoutSeconds + "s），请稍后重试");
        }
        return driver;
    }

    /**
     * 归还 WebDriver 到池中。
     * 归还前做健康检查，异常实例直接销毁并补充新实例。
     *
     * @param driver 要归还的 WebDriver
     */
    public void returnDriver(WebDriver driver) {
        if (driver == null) {
            return;
        }
        if (isDriverAlive(driver)) {
            idlePool.offer(driver);
            log.info("[WebDriverPool] driver 已归还，当前空闲数: {}", idlePool.size());
        } else {
            // driver 已失效，销毁并补充新实例
            log.warn("[WebDriverPool] driver 已失效，销毁并补充新实例");
            quietQuit(driver);
            try {
                WebDriver newDriver = createDriver();
                idlePool.offer(newDriver);
            } catch (Exception e) {
                totalCreated.decrementAndGet();
                log.error("[WebDriverPool] 补充新 driver 失败", e);
            }
        }
    }

    /**
     * 检查 driver 是否仍然可用
     */
    private boolean isDriverAlive(WebDriver driver) {
        try {
            driver.getCurrentUrl();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建一个新的无头 ChromeDriver
     */
    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments(String.format("--window-size=%d,%d", width, height));
        options.addArguments("--disable-extensions");
        return new ChromeDriver(options);
    }

    /**
     * 懒加载初始化：首次借用时预热一个 driver 放入池中
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    idlePool = new ArrayBlockingQueue<>(maxPoolSize);
                    // 预热一个 driver
                    try {
                        WebDriver driver = createDriver();
                        totalCreated.incrementAndGet();
                        idlePool.offer(driver);
                        log.info("[WebDriverPool] 连接池初始化完成，预热 1 个 driver，最大池大小: {}", maxPoolSize);
                    } catch (Exception e) {
                        log.error("[WebDriverPool] 预热 driver 失败，将在首次借用时重试", e);
                    }
                    initialized = true;
                }
            }
        }
    }

    /**
     * Spring 容器关闭时销毁所有 driver
     */
    @PreDestroy
    public void destroy() {
        if (idlePool == null) {
            return;
        }
        log.info("[WebDriverPool] 关闭连接池，销毁所有 driver，数量: {}", idlePool.size());
        WebDriver driver;
        while ((driver = idlePool.poll()) != null) {
            quietQuit(driver);
        }
    }

    /**
     * 安静地销毁 driver，捕获并记录任何异常，避免影响池的正常关闭
     * @param driver
     */
    private void quietQuit(WebDriver driver) {
        try {
            driver.quit();
        } catch (Exception e) {
            log.warn("[WebDriverPool] 销毁 driver 时出错（已忽略）: {}", e.getMessage());
        }
    }
}
