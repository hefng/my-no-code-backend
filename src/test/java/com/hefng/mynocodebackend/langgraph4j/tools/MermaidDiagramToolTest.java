package com.hefng.mynocodebackend.langgraph4j.tools;

import com.hefng.mynocodebackend.langgraph4j.entity.ImageResource;
import com.hefng.mynocodebackend.langgraph4j.entity.enums.ImageCategoryEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class MermaidDiagramToolTest {

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    private static final String SIMPLE_FLOWCHART = """
sequenceDiagram
    participant Admin as 管理员浏览器<br/>(Activities.vue)
    participant Vite as Vite Dev Server<br/>(代理 /api → :8123)
    participant Backend as Spring Boot<br/>(AdminController)
    participant DB as H2 Database

    Admin->>Admin: 点击某活动行的「导出成员」按钮
    Admin->>Admin: 调用 exportMembers(row)
    Admin->>Vite: fetch GET /api/admin/activities/{id}/members/export
    Vite->>Backend: 转发 GET /api/admin/activities/{id}/members/export

    Backend->>DB: activityRepository.findById(id)
    DB-->>Backend: Activity 实体

    alt 活动不存在或已删除
        Backend-->>Admin: HTTP 404
        Admin->>Admin: ElMessage.error('导出失败')
    else 活动存在
        Backend->>DB: userActivityRepository.findByActivityIdAndStatus(id, 1)
        DB-->>Backend: List<UserActivity>（含 User 关联数据）

        Backend->>Backend: 拼装 CSV 字符串<br/>（UTF-8 BOM + 表头 + 数据行）
        Backend->>Backend: 设置响应头<br/>Content-Type: text/csv<br/>Content-Disposition: attachment; filename*=UTF-8''...

        Backend-->>Vite: HTTP 200 + CSV 字节流
        Vite-->>Admin: 透传响应

        Admin->>Admin: response.blob() 转为 Blob 对象
        Admin->>Admin: URL.createObjectURL(blob) 生成临时 URL
        Admin->>Admin: 创建 <a> 标签并触发 click() 下载
        Admin->>Admin: 清理临时 DOM 和对象 URL
        Admin->>Admin: ElMessage.success('导出成功')
    end
            """;

    private static final String SEQUENCE_DIAGRAM = """
            sequenceDiagram
                participant User
                participant Frontend
                participant Backend
                participant AI
                User->>Frontend: 输入需求
                Frontend->>Backend: 发送请求
                Backend->>AI: 调用生成接口
                AI-->>Backend: 返回代码
                Backend-->>Frontend: 返回结果
                Frontend-->>User: 展示页面
            """;

    @Test
    void generateFlowchart() {
        List<ImageResource> resource = mermaidDiagramTool.generateDiagram(SIMPLE_FLOWCHART, "网站生成工作流程图");
        assertNotNull(resource);
        assertFalse(resource.getFirst().getUrl().isBlank(), "返回的 URL 不应为空");
        assertEquals(ImageCategoryEnum.ARCHITECTURE, resource.getFirst().getCategory());
        log.info("架构图资源: category={}, description={}, url={}", resource.getFirst().getCategory(), resource.getFirst().getDescription(), resource.getFirst().getUrl());
    }

    @Test
    void generateSequenceDiagram() {
        List<ImageResource> resource = mermaidDiagramTool.generateDiagram(SEQUENCE_DIAGRAM, "前后端交互时序图");
        ImageResource first = resource.getFirst();
        assertNotNull(resource);
        assertFalse(resource.getFirst().getUrl().isBlank(), "返回的 URL 不应为空");
        assertEquals(ImageCategoryEnum.ARCHITECTURE, resource.getFirst().getCategory());
        log.info("时序图资源: category={}, description={}, url={}", resource.getFirst().getCategory(), resource.getFirst().getDescription(), resource.getFirst().getUrl());
    }
}
