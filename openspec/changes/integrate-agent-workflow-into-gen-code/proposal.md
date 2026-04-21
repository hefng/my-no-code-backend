## Why

当前 `chatToGenCode` 只走单一路径，已完成的工作流能力无法按业务场景接入，导致“是否启用 Agent 工作流”“首次创建后会话式修改差异化处理”“业务 appId 透传”“对话留痕”这四类核心需求无法在同一入口稳定落地。随着代码生成能力扩展，需要在不破坏现有接口兼容性的前提下完成集成。

## What Changes

- 在 `chatToGenCode` 请求中新增 `isAgent` 字段，用于路由到“传统生成”或“工作流生成”路径。
- 将业务侧 `appId` 注入工作流上下文，替换当前固定 `appId` 的执行方式。
- 在工作流中显式区分“首次创建应用”和“已创建应用的会话式修改”模式：当用户在应用创建后通过对话发出局部改动指令（如“标题改为xxx”）时，走修改模式并跳过图片收集与提示词增强节点。
- 为工作流执行补齐用户对话记录持久化，保证会话可追踪与后续调试能力。
- 保持现有调用方默认行为不变（未传 `isAgent` 时沿用原路径）。

## Capabilities

### New Capabilities
- `agent-codegen-workflow-integration`: 统一定义 Agent 路由、业务 `appId` 透传、首次创建/会话式修改分支和工作流对话持久化的端到端行为。

### Modified Capabilities
- 无

## Impact

- Backend API: `chatToGenCode` 入参模型与控制层参数校验。
- Backend Service: 代码生成应用服务路由逻辑、工作流调用参数构建。
- Workflow Engine: 节点条件分支与上下文变量（`appId`、操作类型）。
- Data Layer: 对话记录写入链路（实体、Mapper/Repository、Service 调用点）。
- 测试: 接口层与服务层新增路由/分支/持久化测试。
