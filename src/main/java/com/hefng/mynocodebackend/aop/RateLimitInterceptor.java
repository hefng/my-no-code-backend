package com.hefng.mynocodebackend.aop;

import com.hefng.mynocodebackend.annotation.RateLimit;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的接口限流切面
 */
@Aspect
@Component
public class RateLimitInterceptor {

    private static final String RATE_LIMIT_ERROR_MESSAGE = "请求过于频繁，请稍后再试";

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Around("@annotation(rateLimit)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);

        String rateLimitKey = buildRateLimitKey(rateLimit.keyPrefix(), loginUser.getId());
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimitKey);
        rateLimiter.trySetRate(
                RateType.OVERALL,
                rateLimit.count(),
                rateLimit.time(),
                toRateIntervalUnit(rateLimit.unit())
        );

        if (!rateLimiter.tryAcquire(1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, RATE_LIMIT_ERROR_MESSAGE);
        }
        return joinPoint.proceed();
    }

    String buildRateLimitKey(String keyPrefix, Long userId) {
        return keyPrefix + ":user:" + userId;
    }

    RateIntervalUnit toRateIntervalUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case SECONDS -> RateIntervalUnit.SECONDS;
            case MINUTES -> RateIntervalUnit.MINUTES;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.DAYS;
            default -> throw new IllegalArgumentException("不支持的限流时间单位: " + timeUnit);
        };
    }
}
