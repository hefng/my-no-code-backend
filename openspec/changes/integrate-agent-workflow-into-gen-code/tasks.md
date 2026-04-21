## 1. API Contract and Routing

- [x] 1.1 在 `chatToGenCode` 请求 DTO 中新增可选字段 `isAgent`，并补充字段说明与默认行为。
- [x] 1.2 更新 Controller 入参校验逻辑，确保 `isAgent=true` 时对 `appId` 做必填与格式校验。
- [x] 1.3 在代码生成应用服务中实现统一路由：`isAgent=true` 走工作流，默认走原有生成链路。

## 2. Workflow Context and Branching

- [x] 2.1 改造工作流入口参数，移除固定 `appId` 使用，统一读取业务 `appId`。
- [x] 2.2 在服务层实现 `operationType` 自动判定：基于应用是否已创建 + 用户指令语义识别 CREATE/MODIFY。
- [x] 2.3 在工作流上下文新增并传递 `operationType`（CREATE/MODIFY），并在流程图中增加条件分支：`MODIFY` 跳过图片收集与提示词增强节点。
- [x] 2.4 为会话式 `MODIFY` 模式补充最小可用提示模板，覆盖“标题改为xxx”等局部改动指令。

## 3. Conversation Persistence

- [x] 3.1 梳理并补齐对话记录实体/Mapper/Service，确保支持按 `appId` 关联会话。
- [x] 3.2 在工作流执行前持久化用户输入消息。
- [x] 3.3 在工作流完成或失败时持久化助手输出或错误结果。
- [x] 3.4 增加必要日志字段（`traceId`、`appId`、operationType）以便排障。

## 4. Testing and Rollout

- [x] 4.1 新增服务层测试：覆盖 `isAgent` 路由、默认兼容行为与参数校验失败分支。
- [x] 4.2 新增工作流测试：覆盖 `appId` 透传、首次创建分支和会话式修改分支行为。
- [x] 4.3 新增对话持久化测试：覆盖成功与失败场景下的记录写入。
- [x] 4.4 准备灰度配置与回滚开关验证，确保可快速切回传统路径。
