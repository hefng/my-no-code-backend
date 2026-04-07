# 变更日志 2026-04-07

## 基本信息

- 变动日期：2026-04-07
- 变动时间：今日
- 分支：master
- 提交：refactor: 重构 AI 代码生成模块包结构，新增多工具支持与可视化编辑接口

---

## 变动文件列表

### 包结构重组（重命名/迁移）

| 原路径 | 新路径 |
|--------|--------|
| `ai/AiCodeGenTypeRoutingServiceFactory.java` | `ai/factory/AiCodeGenTypeRoutingServiceFactory.java` |
| `ai/AiCodeGeneratorServiceFactory.java` | `ai/factory/AiCodeGeneratorServiceFactory.java` |
| `ai/VueProjectCodegenServiceFactory.java` | `ai/factory/VueProjectCodegenServiceFactory.java` |
| `ai/AiCodeGenTypeRoutingService.java` | `ai/service/AiCodeGenTypeRoutingService.java` |
| `ai/AiCodegenService.java` | `ai/service/AiCodegenService.java` |
| `ai/VueProjectCodegenService.java` | `ai/service/VueProjectCodegenService.java` |
| `ai/tool/VueProjectFileSaveTool.java` | `ai/tool/ProjectFileSaveTool.java` |

### 新增文件

- `ai/tool/BaseProjectTool.java`
- `ai/tool/ProjectDirectoryReadTool.java`
- `ai/tool/ProjectFileDeleteTool.java`
- `ai/tool/ProjectFileEditTool.java`
- `ai/tool/ProjectFileReadTool.java`
- `ai/tool/ToolManager.java`
- `model/dto/project/ProjectFileEditRequest.java`

### 修改文件

- `ai/AiCodegenServiceFaced.java`
- `ai/factory/VueProjectCodegenServiceFactory.java`
- `ai/service/VueProjectCodegenService.java`
- `controller/AppController.java`
- `controller/ProjectDownloadController.java`
- `test/.../AiCodeGenTypeRoutingServiceTest.java`
- `test/.../AiCodegenServiceTest.java`

---

## 各文件主要修改内容

### 包结构重组

将原本平铺在 `ai/` 包下的 factory 和 service 类分别迁移到 `ai/factory/` 和 `ai/service/` 子包，同时将 `VueProjectFileSaveTool` 重命名为 `ProjectFileSaveTool`，统一工具命名规范（去掉 Vue 前缀，体现通用性）。

### BaseProjectTool（新增）

新增抽象基类，提取所有工具共用的逻辑：
- `buildProjectDir(appId)`：根据 appId 构建项目目录路径
- `resolveAndValidatePath(appId, relativePath)`：路径安全校验，防止路径穿越攻击
- `getToolName()` / `getToolDescription()`：抽象方法，子类实现工具元信息
- `generateToolRequestResponse()`：生成工具调用时展示给用户的提示文本
- `generateToolExecutedResult(JSONObject)`：抽象方法，生成工具执行结果的格式化字符串（用于保存到数据库）

### ProjectFileReadTool（新增）

AI 工具：读取项目中指定文件的完整内容。AI 在修改文件前可先调用此工具获取现有代码，从而生成更精准的 `oldContent`，提升 `ProjectFileEditTool` 的修改成功率。

### ProjectFileEditTool（新增）

AI 工具：对文件中的指定代码片段进行精准替换（`oldContent` → `newContent`），无需重写整个文件。相比 `ProjectFileSaveTool` 大幅减少 token 消耗，适用于局部修改场景。

### ProjectFileDeleteTool（新增）

AI 工具：删除项目中的指定文件，支持路径安全校验。

### ProjectDirectoryReadTool（新增）

AI 工具：读取项目目录结构，列出指定目录下所有文件和子目录的相对路径。AI 可在修改或删除文件前先了解项目结构，做出更准确的决策。

### ToolManager（新增）

集中管理所有 `BaseProjectTool` 实例的容器类：
- `@PostConstruct` 初始化时自动扫描并注册所有工具到 `TOOL_MAP`
- `getToolByName(String)`：按类名查找工具实例
- `getAllTools()`：返回所有工具数组，供 `VueProjectCodegenServiceFactory` 注册到 AI 服务

### VueProjectCodegenServiceFactory（重构）

核心重构点：
- `getService()` 不再需要传入 `appId` 参数
- 改用 `chatMemoryProvider` 替代 `chatMemory`，配合 `@MemoryId` 实现多用户对话记忆按需隔离
- 注入 `ToolManager`，一次性注册所有工具（保存、编辑、读取、删除文件，以及读取目录结构）
- 新增 `hallucinatedToolNameStrategy`，防止 AI 调用不存在的工具名时抛出异常

### VueProjectCodegenService（重构）

- `generateVueProjectStream` 方法新增 `@MemoryId Long appId` 参数
- 接口改为 `public`，适配跨包引用
- 更新注释，说明 `@MemoryId` 与 `chatMemoryProvider` 配合使用的正确姿势

### AiCodegenServiceFaced（修改）

- 更新 import 路径适配新包结构
- `generateVueProjectStream` 调用改为 `vueProjectCodegenServiceFactory.getService()`（无参），并将 `appId` 传入 `service.generateVueProjectStream(userMessage, appId)`

### ProjectDownloadController（扩展）

新增 `POST /project/file/edit` 接口，支持可视化编辑器绕过 AI 直接修改项目文件：
- 参数：`appId`、`relativePath`、`oldContent`、`newContent`
- 鉴权：仅应用所有者可操作
- 安全：路径穿越防护（`Path.normalize()` + `startsWith` 校验）
- 逻辑：精确匹配 `oldContent` 后替换为 `newContent`，未找到匹配内容时返回明确错误

### ProjectFileEditRequest（新增 DTO）

`POST /project/file/edit` 接口的请求体，包含 `appId`、`relativePath`、`oldContent`、`newContent` 四个字段。

---

## 修改原因和目的

1. **包结构优化**：原 `ai/` 包下混放 factory、service、tool 等不同职责的类，随着工具数量增加难以维护，拆分子包提升可读性和可扩展性。

2. **多工具支持**：原 Vue 工程化 AI 只有文件保存一个工具，无法读取现有文件内容，导致 AI 修改时容易产生错误的 `oldContent`。新增读取、编辑、删除、目录查看工具后，AI 可以先读后改，大幅提升修改准确率。

3. **对话记忆隔离修复**：原 `getService(appId)` 每次请求都新建服务实例，存在资源浪费。改用 `chatMemoryProvider` 后，单个服务实例可服务多个用户，对话记忆按 `appId` 自动隔离，同时修复了 `@ToolMemoryId` 无法正确获取 `appId` 的问题。

4. **可视化编辑器支持**：新增直接编辑接口，让前端可视化编辑器在用户直接修改页面元素时，无需经过 AI 即可将修改同步到源文件，响应更快、成本更低。

---

## 可能的影响范围

- **AI 代码生成流程**：`VueProjectCodegenService.generateVueProjectStream` 签名变更，所有调用方（`AiCodegenServiceFaced`）已同步更新。
- **工具注册机制**：新增工具只需继承 `BaseProjectTool` 并标注 `@Component`，`ToolManager` 会自动扫描注册，无需手动修改工厂类。
- **前端可视化编辑**：新增 `/project/file/edit` 接口，前端需配合调用（已在前端 `projectController.ts` 中实现对应 API）。
- **测试文件**：两个测试类仅更新了 import 路径，测试逻辑不变。
