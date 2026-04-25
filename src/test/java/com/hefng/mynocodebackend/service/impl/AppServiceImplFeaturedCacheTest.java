package com.hefng.mynocodebackend.service.impl;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hefng.mynocodebackend.config.FeaturedAppsCacheProperties;
import com.hefng.mynocodebackend.constant.AppConstant;
import com.hefng.mynocodebackend.model.dto.app.AppQueryRequest;
import com.hefng.mynocodebackend.model.entity.App;
import com.hefng.mynocodebackend.model.vo.AppVO;
import com.mybatisflex.core.paginate.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppServiceImplFeaturedCacheTest {

    private AppServiceImpl appService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private FeaturedAppsCacheProperties featuredAppsCacheProperties;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        appService = spy(new AppServiceImpl());
        objectMapper = new ObjectMapper();
        featuredAppsCacheProperties = new FeaturedAppsCacheProperties();
        featuredAppsCacheProperties.setEnabled(true);
        featuredAppsCacheProperties.setTtlMinutes(5);
        featuredAppsCacheProperties.setTtlJitterMinutes(5);
        featuredAppsCacheProperties.setKeyPrefix("app:featured:page:");

        ReflectionTestUtils.setField(appService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(appService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(appService, "featuredAppsCacheProperties", featuredAppsCacheProperties);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldReturnCachedResultOnHit() throws Exception {
        AppQueryRequest request = buildRequest();
        String cacheJson = """
                {"records":[{"id":1,"appName":"cached app"}],"pageNumber":1,"pageSize":20,"totalRow":1}
                """;
        when(valueOperations.get(buildExpectedCacheKey(request))).thenReturn(cacheJson);

        Page<AppVO> result = appService.listFeaturedAppByPage(request);

        assertNotNull(result);
        assertEquals(1L, result.getTotalRow());
        assertEquals(1, result.getRecords().size());
        assertEquals("cached app", result.getRecords().getFirst().getAppName());
        verify(appService, never()).page(any(Page.class), any());
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldQueryOriginAndWriteCacheOnMiss() {
        AppQueryRequest request = buildRequest();
        when(valueOperations.get(buildExpectedCacheKey(request))).thenReturn(null);
        mockOriginQuery("origin app", 1L);

        Page<AppVO> result = appService.listFeaturedAppByPage(request);

        assertEquals("origin app", result.getRecords().getFirst().getAppName());
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(anyString(), anyString(), ttlCaptor.capture(), any(TimeUnit.class));
        assertTrue(ttlCaptor.getValue() >= 5 && ttlCaptor.getValue() <= 10);
        verify(appService, times(1)).page(any(Page.class), any());
    }

    @Test
    void shouldRefreshOriginAfterCacheExpiry() {
        AppQueryRequest request = buildRequest();
        when(valueOperations.get(buildExpectedCacheKey(request))).thenReturn(null, null);
        mockOriginQuery("refreshed app", 1L);

        Page<AppVO> firstResult = appService.listFeaturedAppByPage(request);
        Page<AppVO> secondResult = appService.listFeaturedAppByPage(request);

        assertEquals("refreshed app", firstResult.getRecords().getFirst().getAppName());
        assertEquals("refreshed app", secondResult.getRecords().getFirst().getAppName());
        verify(appService, times(2)).page(any(Page.class), any());
        verify(valueOperations, times(2)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldFallbackToOriginWhenCacheReadFails() {
        AppQueryRequest request = buildRequest();
        when(valueOperations.get(buildExpectedCacheKey(request))).thenThrow(new RuntimeException("redis read failed"));
        mockOriginQuery("fallback app", 1L);

        Page<AppVO> result = appService.listFeaturedAppByPage(request);

        assertEquals("fallback app", result.getRecords().getFirst().getAppName());
        verify(appService, times(1)).page(any(Page.class), any());
        verify(valueOperations, times(1)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldReturnOriginResultWhenCacheWriteFails() {
        AppQueryRequest request = buildRequest();
        when(valueOperations.get(buildExpectedCacheKey(request))).thenReturn(null);
        mockOriginQuery("write fallback app", 1L);
        org.mockito.Mockito.doThrow(new RuntimeException("redis write failed"))
                .when(valueOperations)
                .set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        Page<AppVO> result = appService.listFeaturedAppByPage(request);

        assertEquals("write fallback app", result.getRecords().getFirst().getAppName());
        verify(appService, times(1)).page(any(Page.class), any());
    }

    private void mockOriginQuery(String appName, long totalRow) {
        Page<App> appPage = new Page<>(1, 20, totalRow);
        App app = new App();
        app.setId(1L);
        appPage.setRecords(List.of(app));

        AppVO appVO = new AppVO();
        appVO.setId(1L);
        appVO.setAppName(appName);

        doReturn(appPage).when(appService).page(any(Page.class), any());
        doReturn(List.of(appVO)).when(appService).getAppVO(any(List.class));
    }

    private AppQueryRequest buildRequest() {
        AppQueryRequest request = new AppQueryRequest();
        request.setCurrent(1);
        request.setPageSize(20);
        request.setSortField("createTime");
        request.setSortOrder("desc");
        request.setAppName("demo");
        return request;
    }

    private String buildExpectedCacheKey(AppQueryRequest request) {
        Map<String, Object> queryConditions = new LinkedHashMap<>();
        queryConditions.put("current", request.getCurrent());
        queryConditions.put("pageSize", request.getPageSize());
        queryConditions.put("sortField", request.getSortField());
        queryConditions.put("sortOrder", request.getSortOrder());
        queryConditions.put("id", request.getId());
        queryConditions.put("appName", request.getAppName());
        queryConditions.put("appDesc", request.getAppDesc());
        queryConditions.put("codegenType", request.getCodegenType());
        queryConditions.put("appOwnerId", request.getAppOwnerId());
        queryConditions.put("priority", request.getPriority());
        queryConditions.put("featuredPriority", AppConstant.MAX_PRIORITY);
        return featuredAppsCacheProperties.getKeyPrefix()
                + DigestUtils.md5DigestAsHex(JSONUtil.toJsonStr(queryConditions).getBytes());
    }
}
