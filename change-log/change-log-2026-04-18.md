# 变更日志 2026-04-18

## 变动日期与时间
2026-04-18

## 变动文件列表

| 文件路径 | 变动类型 |
|---|---|
| `src/main/java/com/hefng/mynocodebackend/langgraph4j/CodeGenWorkflow.java` | 修改 |
| `src/main/java/com/hefng/mynocodebackend/langgraph4j/WorkFlowApp.java` | 修改 |
| `src/main/java/com/hefng/mynocodebackend/langgraph4j/node/CodeGeneratorNode.java` | 修改 |
| `src/main/java/com/hefng/mynocodebackend/langgraph4j/node/ProjectBuilderNode.java` | 修改 |

---

## 各文件主要修改内容

### 1. `CodeGenWorkflow.java`

- **新增条件路由逻辑**：将原来 `code_generator → project_builder` 的固定边，改为基于 `CodegenTypeEnum` 的条件边（`addConditionalEdges`）。
  - 若生成类型为 `VUE_PROJECT`，路由到 `project_builder` 节点执行构建。
  - 其他类型（如单文件 HTML）直接路由到 `END`，跳过构建步骤，减少不必要的开销。
- **新增私有方法** `buildRouter()` 和 `buildRouteMap()`，将路由逻辑封装，提升可读性。
- **重构 `WorkFlowApp` 的重复代码**：`WorkFlowApp.main` 中原有的工作流构建逻辑统一委托给 `CodeGenWorkflow.createWorkflow()`，消除重复。
- 日志文案精简，去除冗余注释。

### 2. `WorkFlowApp.java`

- **移除重复的工作流构建代码**：不再在 `main` 方法中手动拼装节点和边，改为直接调用 `new CodeGenWorkflow().createWorkflow()`，与生产路径保持一致。
- 移除不再需要的 `import`（`MessagesStateGraph`、`node.*` 通配符等）。
- 日志文案与 `CodeGenWorkflow` 保持统一风格。

### 3. `CodeGeneratorNode.java`

- **新增 `context.setBuildResultDir(generatedCodeDir)` 默认赋值**：代码生成完成后，先将构建产物目录默认设置为生成代码目录；若后续进入 `ProjectBuilderNode`，则会被 `dist` 目录覆盖。这样即使跳过构建步骤，`buildResultDir` 也始终有有效值。
- 调整日志顺序，先取 `generationType` 再打印，逻辑更清晰。
- 移除冗余注释，精简代码结构。

### 4. `ProjectBuilderNode.java`

- **简化节点职责**：由于条件路由已保证只有 `VUE_PROJECT` 类型才会进入此节点，移除了节点内部的 `if (generationType == VUE_PROJECT)` 分支判断，节点逻辑更单一。
- 移除不再需要的 `import`（`AiCodegenServiceFaced`、`CodegenTypeEnum`、`AppConstant`、`Flux`、`Duration`、`CompletableFuture`）。
- 构建失败时异常处理保留，但去掉了"回退到原目录"的兜底逻辑（因为 `CodeGeneratorNode` 已提前设置默认值）。

---

## 修改原因与目的

- **解耦构建步骤**：不同代码生成类型（Vue 完整项目 vs 单文件 HTML）的后处理需求不同。通过条件路由，避免非 Vue 项目进入构建节点，减少无效执行和潜在错误。
- **消除重复代码**：`WorkFlowApp` 与 `CodeGenWorkflow` 原本各自维护一套工作流构建逻辑，重构后统一入口，降低维护成本。
- **提升状态完整性**：`buildResultDir` 在 `CodeGeneratorNode` 阶段即赋默认值，确保无论是否经过构建节点，下游读取该字段时都不会得到 `null`。
- **代码整洁**：移除冗余注释、无用 `import`，统一日志风格。

---

## 可能的影响范围

- **工作流执行路径变化**：非 `VUE_PROJECT` 类型的代码生成请求将在 `code_generator` 节点后直接结束，不再触发 `ProjectBuilderNode`，行为与预期一致但与旧版不同，需确认相关测试用例覆盖此路径。
- **`buildResultDir` 字段语义变化**：该字段现在由 `CodeGeneratorNode` 负责初始化，`ProjectBuilderNode` 负责覆盖（仅 Vue 项目）。依赖此字段的下游逻辑（如文件上传、预览 URL 生成）行为不变，但初始化时机提前。
- **`WorkFlowApp` 测试**：`main` 方法现在走与生产相同的 `createWorkflow()` 路径，本地调试结果更贴近真实环境。
