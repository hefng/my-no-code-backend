package com.hefng.mynocodebackend.langgraph4j.demo;

import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * LangGraph4j 入门 Demo
 *
 * 核心思想：把业务逻辑拆成一个个"节点(Node)"，节点之间用"边(Edge)"连接，
 * 共享一个"状态(State)"对象在节点间传递数据。
 *
 * 本例图结构（有向无环图）：
 *
 *   [START] --> [greeter] --> [responder] --> [END]
 *
 * 执行流程：
 *   1. greeter 节点向 state 追加一条问候消息
 *   2. responder 节点读取问候消息，追加一条回复消息
 *   3. 到达 END，图执行结束
 */
public class SimpleGraphDemo {

    // =========================================================================
    // 第一部分：定义 State（状态）
    //
    // State 是整张图的"共享内存"，所有节点都从这里读数据、往这里写数据。
    // LangGraph4j 要求 State 继承 AgentState，底层是一个 Map<String, Object>。
    // =========================================================================

    static class SimpleState extends AgentState {

        // 约定一个 key 常量，避免字符串硬编码
        static final String MESSAGES_KEY = "messages";

        /**
         * SCHEMA 定义了 State 中每个字段的"合并策略（Channel）"。
         *
         * 这里使用 Channels.appender(ArrayList::new)：
         *   - 当节点返回 Map.of("messages", "某条消息") 时，
         *     框架不会覆盖旧值，而是把新值追加到列表末尾。
         *   - ArrayList::new 是列表的初始化工厂，State 初始化时自动创建空列表。
         *
         * 对比：如果用 Channels.last()，则每次更新都会覆盖旧值（类似普通变量赋值）。
         */
        static final Map<String, Channel<?>> SCHEMA = Map.of(
                MESSAGES_KEY, Channels.appender(ArrayList::new)
        );

        /**
         * 构造函数固定写法：把初始数据透传给父类 AgentState。
         * 框架在创建 State 实例时会调用这个构造函数。
         */
        SimpleState(Map<String, Object> initData) {
            super(initData);
        }

        /**
         * 便捷读取方法：从 State 中取出 messages 列表。
         *
         * value(key) 返回 Optional，如果该 key 还没有值则返回空列表，避免 NPE。
         */
        @SuppressWarnings("unchecked")
        List<String> messages() {
            return this.<List<String>>value(MESSAGES_KEY).orElse(List.of());
        }
    }

    // =========================================================================
    // 第二部分：定义节点（Node）
    //
    // 节点就是一个普通函数，签名固定为：
    //   Map<String, Object> someNode(YourState state)
    //
    // 入参：当前 State（只读，用来读取上下文）
    // 返回：一个 Map，表示"本次执行后要更新 State 的哪些字段"。
    //       框架会按照 SCHEMA 中定义的 Channel 策略把返回值合并进 State。
    // =========================================================================

    /**
     * 问候节点
     * 职责：向 messages 列表追加一条问候语。
     * 返回的 Map key 必须和 SCHEMA 中定义的 key 一致。
     */
    static Map<String, Object> greeterNode(SimpleState state) {
        System.out.println("[greeter] 执行前，messages = " + state.messages());
        // 返回要更新的字段，框架会把 "Hello from GreeterNode!" 追加到 messages 列表
        return Map.of(SimpleState.MESSAGES_KEY, "Hello from GreeterNode!");
    }

    /**
     * 回复节点
     * 职责：读取 greeter 写入的问候语，追加一条回复。
     * 演示了节点之间通过 State 传递数据的方式。
     */
    static Map<String, Object> responderNode(SimpleState state) {
        System.out.println("[responder] 执行前，messages = " + state.messages());
        // 读取上一个节点写入的数据，做出不同的回复
        String reply = state.messages().contains("Hello from GreeterNode!")
                ? "Acknowledged greeting!"
                : "No greeting found.";
        return Map.of(SimpleState.MESSAGES_KEY, reply);
    }

    // =========================================================================
    // 第三部分：构建图、编译、运行
    // =========================================================================

    public static void main(String[] args) throws Exception {

        // ----------------------------------------------------------------------
        // 3-1. 构建 StateGraph（图的"蓝图"，此时还不能运行）
        //
        // new StateGraph<>(SCHEMA, 构造函数引用)
        //   - SCHEMA：告诉框架每个字段怎么合并
        //   - SimpleState::new：框架用这个工厂方法创建 State 实例
        // ----------------------------------------------------------------------
        var graph = new StateGraph<>(SimpleState.SCHEMA, SimpleState::new)

                // addNode("节点名", 节点函数)
                // node_async(...) 把同步函数包装成框架要求的异步接口，写法固定
                .addNode("greeter",   node_async(SimpleGraphDemo::greeterNode))
                .addNode("responder", node_async(SimpleGraphDemo::responderNode))

                // addEdge(from, to) 定义节点间的流转关系
                // START / END 是框架内置的特殊节点名，代表图的入口和出口
                .addEdge(START,       "greeter")    // 图启动后第一个执行 greeter
                .addEdge("greeter",   "responder")  // greeter 执行完后执行 responder
                .addEdge("responder", END);          // responder 执行完后图结束

        // ----------------------------------------------------------------------
        // 3-2. 编译图
        //
        // compile() 会校验图结构（比如有没有孤立节点、边是否完整），
        // 返回一个不可变的 CompiledGraph，才能真正执行。
        // ----------------------------------------------------------------------
        var compiled = graph.compile();

        System.out.println("=== 开始执行图 ===");

        // ----------------------------------------------------------------------
        // 3-3. 运行图：stream 模式
        //
        // compiled.stream(初始State数据) 启动图，返回一个可迭代对象。
        // 每执行完一个节点，就 yield 一次，输出该节点执行后的 State 快照。
        // 这里传入空 Map，表示初始 State 没有任何预置数据。
        //
        // 另一种运行方式是 compiled.invoke(...)，它会等图完全执行完再返回最终 State，
        // 适合不需要中间状态的场景。
        // ----------------------------------------------------------------------
        for (var nodeOutput : compiled.stream(Map.of())) {
            // nodeOutput 包含：节点名 + 该节点执行后的完整 State
            System.out.println("节点输出: " + nodeOutput);
        }

        System.out.println("=== 执行完毕 ===");
    }
}
