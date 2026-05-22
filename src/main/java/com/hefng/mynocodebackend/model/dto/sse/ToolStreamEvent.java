package com.hefng.mynocodebackend.model.dto.sse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具流式事件载荷。
 * <p>
 * 这个对象只负责承载工具执行过程中的一条 SSE 事件数据，
 * 后端会把它序列化后推给前端，用于展示“工具正在执行 / 已完成 / 已失败”等状态。
 * <p>
 * 这里使用普通 JavaBean，而不是 record，是为了兼容当前项目里的 JSON 序列化和反序列化链路。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolStreamEvent {

    /** 执行阶段：executing、completed、failed。 */
    private String phase;
    /** 工具调用 ID，用于前端区分同一次工具调用的前后两条事件。 */
    private String toolCallId;
    /** 工具名称，对应 LangChain4j 调用的具体工具方法名。 */
    private String toolName;
    /** 给前端展示的简短提示文案。 */
    private String message;
    /** 工具调用参数，便于前端查看当前正在处理哪个文件或目录。 */
    private String arguments;
    /** 工具执行结果的摘要内容。 */
    private String result;
}
