package com.hefng.mynocodebackend.aop;

import com.hefng.mynocodebackend.annotation.RateLimit;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/**
 * 限流切面测试，验证基于用户维度的 Redisson 限流是否按预期生效。
 */
class RateLimitInterceptorTest {

    private final RateLimitInterceptor rateLimitInterceptor = new RateLimitInterceptor();

    @Mock
    private UserService userService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RRateLimiter firstUserRateLimiter;

    @Mock
    private RRateLimiter secondUserRateLimiter;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private RateLimit rateLimit;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(rateLimitInterceptor, "userService", userService);
        ReflectionTestUtils.setField(rateLimitInterceptor, "redissonClient", redissonClient);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = RateLimitAnnotatedService.class.getDeclaredMethod("limitedMethod");
        rateLimit = method.getAnnotation(RateLimit.class);
    }

    @Test
    /**
     * 验证同一用户在一分钟内前 5 次请求放行，第 6 次请求会被限流拦截。
     */
    void shouldAllowFirstFiveRequestsAndRejectSixthForSameUser() throws Throwable {
        User loginUser = new User();
        loginUser.setId(1L);

        when(userService.getLoginUser(org.mockito.ArgumentMatchers.any(HttpServletRequest.class))).thenReturn(loginUser);
        when(redissonClient.getRateLimiter("rate_limit:chatToGenCode:user:1")).thenReturn(firstUserRateLimiter);
        when(firstUserRateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.MINUTES)).thenReturn(true);
        when(firstUserRateLimiter.tryAcquire(1)).thenReturn(true, true, true, true, true, false);
        when(joinPoint.proceed()).thenReturn("ok");

        for (int i = 0; i < 5; i++) {
            Object result = rateLimitInterceptor.doInterceptor(joinPoint, rateLimit);
            assertEquals("ok", result);
        }

        BusinessException exception = assertThrows(BusinessException.class,
                () -> rateLimitInterceptor.doInterceptor(joinPoint, rateLimit));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("请求过于频繁，请稍后再试", exception.getMessage());
        verify(joinPoint, times(5)).proceed();
        verify(firstUserRateLimiter, times(6))
                .trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.MINUTES);
    }

    @Test
    /**
     * 验证限流按用户维度隔离，不同用户的请求配额互不影响。
     */
    void shouldIsolateRequestsBetweenDifferentUsers() throws Throwable {
        User firstUser = new User();
        firstUser.setId(1L);
        User secondUser = new User();
        secondUser.setId(2L);

        when(userService.getLoginUser(org.mockito.ArgumentMatchers.any(HttpServletRequest.class)))
                .thenReturn(firstUser, secondUser);
        when(redissonClient.getRateLimiter("rate_limit:chatToGenCode:user:1")).thenReturn(firstUserRateLimiter);
        when(redissonClient.getRateLimiter("rate_limit:chatToGenCode:user:2")).thenReturn(secondUserRateLimiter);
        when(firstUserRateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.MINUTES)).thenReturn(true);
        when(secondUserRateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.MINUTES)).thenReturn(true);
        when(firstUserRateLimiter.tryAcquire(1)).thenReturn(true);
        when(secondUserRateLimiter.tryAcquire(1)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object firstResult = rateLimitInterceptor.doInterceptor(joinPoint, rateLimit);
        Object secondResult = rateLimitInterceptor.doInterceptor(joinPoint, rateLimit);

        assertEquals("ok", firstResult);
        assertEquals("ok", secondResult);
        verify(redissonClient).getRateLimiter("rate_limit:chatToGenCode:user:1");
        verify(redissonClient).getRateLimiter("rate_limit:chatToGenCode:user:2");
    }

    @Test
    /**
     * 验证注解中的时间窗口和次数配置会被正确转换为 Redisson 的限流规则。
     */
    void shouldConvertAnnotationTimeUnitToRedissonRate() throws Throwable {
        User loginUser = new User();
        loginUser.setId(3L);

        when(userService.getLoginUser(org.mockito.ArgumentMatchers.any(HttpServletRequest.class))).thenReturn(loginUser);
        when(redissonClient.getRateLimiter("rate_limit:chatToGenCode:user:3")).thenReturn(firstUserRateLimiter);
        when(firstUserRateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.MINUTES)).thenReturn(true);
        when(firstUserRateLimiter.tryAcquire(1)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = rateLimitInterceptor.doInterceptor(joinPoint, rateLimit);

        assertEquals("ok", result);
        verify(firstUserRateLimiter).trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.MINUTES);
    }

    @Test
    /**
     * 验证限流 key 会按“前缀 + 用户 ID”格式拼装，确保用户级隔离。
     */
    void shouldBuildUserScopedKey() {
        String key = rateLimitInterceptor.buildRateLimitKey("rate_limit:chatToGenCode", 99L);
        assertEquals("rate_limit:chatToGenCode:user:99", key);
    }

    @Test
    /**
     * 验证分钟级时间单位会被正确映射为 Redisson 的 MINUTES 枚举值。
     */
    void shouldSupportMinutesTimeUnit() {
        RateIntervalUnit unit = rateLimitInterceptor.toRateIntervalUnit(TimeUnit.MINUTES);
        assertEquals(RateIntervalUnit.MINUTES, unit);
    }

    private static class RateLimitAnnotatedService {

        @RateLimit(keyPrefix = "rate_limit:chatToGenCode", time = 1, unit = TimeUnit.MINUTES, count = 5)
        public void limitedMethod() {
        }
    }
}
