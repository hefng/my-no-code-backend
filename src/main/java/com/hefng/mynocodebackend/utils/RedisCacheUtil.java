package com.hefng.mynocodebackend.utils;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RedisCacheUtil {

    private static final long MAX_RANDOM_EXPIRE_MINUTES = 5;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public <T> T getWithCache(Object keyObject,
                              String keyPrefix,
                              long expireTime,
                              TimeUnit timeUnit,
                              Supplier<T> dbSupplier,
                              TypeReference<T> typeReference) {
        String cacheKey = buildCacheKey(keyObject, keyPrefix);
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            return JSONUtil.toBean(cacheValue, typeReference, true);
        }

        T dbResult = dbSupplier.get();
        String jsonValue = JSONUtil.toJsonStr(dbResult);
        stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, expireTime + RandomUtil.randomInt(0, 5), timeUnit);
        return dbResult;
    }

    private String buildCacheKey(Object keyObject, String keyPrefix) {
        String keyJson = JSONUtil.toJsonStr(keyObject);
        String md5Key = DigestUtil.md5Hex(keyJson);
        if (StrUtil.isBlank(keyPrefix)) {
            return md5Key;
        }
        return keyPrefix + md5Key;
    }
}
