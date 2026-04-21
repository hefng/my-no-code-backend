## 1. Context and Configuration

- [x] 1.1 在 `WorkflowContext` 增加代码质量检查重试计数与最大重试次数字段，并补充默认值初始化逻辑
- [x] 1.2 将默认最大重试次数统一设置为 `2`，并保留可覆盖入口（上下文注入或配置读取）

## 2. Node and Routing Logic

- [x] 2.1 在 `CodeQualityCheckerNode` 中接入失败重试计数递增逻辑，并保持成功场景计数行为一致
- [x] 2.2 在质量检查失败且达到上限时设置明确“重试耗尽”状态/错误信息
- [x] 2.3 更新工作流条件路由逻辑：`retryCount >= maxRetries` 时直接结束流程，不再回到修复-复检环路

## 3. Observability and Validation

- [x] 3.1 增加日志字段（当前重试次数、最大重试次数、是否耗尽）以支持线上排查
- [x] 3.2 增加或更新测试，覆盖“未达上限继续重试”“达到上限直接结束流程”“自定义上限覆盖默认值 2”场景
