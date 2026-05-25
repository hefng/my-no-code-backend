package com.hefng.mynocodebackend.ai.tool;

import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证工具名称映射和工具执行结果展示文案是否按预期工作。
 */
@SpringBootTest
class ToolManagerMappingTest {

    @Resource
    private ToolManager toolManager;

    @Resource
    private ProjectFileReadTool projectFileReadTool;

    @Test
    void shouldResolveToolByMethodName() {
        assertNotNull(toolManager.getToolByName("saveFile"));
        assertNotNull(toolManager.getToolByName("readFile"));
        assertSame(projectFileReadTool, toolManager.getToolByName("readFile"));
    }

    @Test
    void shouldUseToolResultInExecutedResult() {
        JSONObject arguments = new JSONObject();
        arguments.put("relativePath", "src/App.vue");
        arguments.put("toolResult", "<template>hello</template>");

        String executedResult = projectFileReadTool.generateToolExecutedResult(arguments);
        assertTrue(executedResult.contains("<template>hello</template>"));
    }
}
