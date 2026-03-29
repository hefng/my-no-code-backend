# 变更日志 - 2026-03-29

## 变动时间
2026-03-29

## 变动文件列表

1. `.gitignore`
2. `src/main/java/com/hefng/mynocodebackend/ai/AiCodegenServiceFaced.java`
3. `src/main/java/com/hefng/mynocodebackend/ai/model/CodegenTypeEnum.java`
4. `src/main/java/com/hefng/mynocodebackend/constant/AppConstant.java`
5. `src/main/java/com/hefng/mynocodebackend/controller/AppController.java`
6. `src/main/java/com/hefng/mynocodebackend/controller/StaticResourceController.java`
7. `src/main/java/com/hefng/mynocodebackend/core/builder/VueProjectBuilder.java`（新增）
8. `src/main/java/com/hefng/mynocodebackend/service/AppService.java`
9. `src/main/java/com/hefng/mynocodebackend/service/impl/AppServiceImpl.java`
10. `src/main/resources/prompts/vue-project-generator-system-prompt.txt`
11. `src/test/java/com/hefng/mynocodebackend/ai/AiCodegenServiceTest.java`

---

## 各文件修改详情

### `.gitignore`
- 新增忽略 `tmp` 目录（本地临时文件）
- 新增忽略 `src/main/resources/application-local.yml`（本地环境配置，不应提交）

### `CodegenTypeEnum.java` & `AppConstant.java`
- 将代码生成类型的枚举值从下划线风格改为连字符风格：
  - `multi_file` → `multi-file`
  - `vue_project` → `vue-project`
- 目的：与前端传参风格保持一致，避免前后端类型值不匹配

### `VueProjectBuilder.java`（新增）
- 新增 Vue 工程化项目构建器组件
- 功能：AI 生成 Vue 项目源码后，异步执行 `npm i` + `npm run build`，将源码打包为可部署的静态文件（dist 目录）
- 使用虚拟线程（`Thread.ofVirtual`）执行耗时的 npm 命令，不阻塞平台线程
- 兼容 Windows（`cmd /c`）和 Unix（`sh -c`）环境

### `AiCodegenServiceFaced.java`
- 注入 `VueProjectBuilder`
- 在 Vue 工程化项目流式生成完成（`onCompleteResponse`）后，异步触发 `VueProjectBuilder.buildAsync(appId)`，执行 npm 构建
- 构建结果通过 `CompletableFuture.whenComplete` 记录日志，不影响响应流

### `AppServiceImpl.java`
- `deployApp` 方法新增对 Vue 工程化项目的特殊部署逻辑：
  - 部署时使用 `dist` 目录（npm build 产物）而非源码目录
  - 仅复制 `dist/index.html` 和 `dist/assets`，精简部署内容
  - 若 dist 目录不存在，返回友好错误提示（项目尚未构建完成）
- 错误提示文案同步更新为 `multi-file/vue-project` 连字符风格

### `AppController.java`
- 注释中代码生成类型说明更新为连字符风格（`multi-file/vue-project`）
- `/app/get/vo` 接口从 `@GetMapping` 改为 `@PostMapping`（接口接收 RequestBody，应使用 POST）

### `StaticResourceController.java`
- 重构静态资源访问逻辑，简化路径解析方式
- 使用 `HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE` 获取资源路径，替代手动字符串截取
- 路径分隔符改为 Windows 兼容的反斜杠
- 移除 `UrlResource`，改用 `FileSystemResource` 直接返回文件
- 移除 HTTP 缓存控制头（`Cache-Control`），简化响应
- 路由参数从 `{codeGenType}_{appId}` 改为 `{deployKey}`，与部署逻辑解耦

### `vue-project-generator-system-prompt.txt`
- 新增两条生成约束规则：
  1. 禁止使用任何状态管理库、类型校验库、代码格式化库
  2. 以可运行为第一要义，确保代码能够直接运行，没有任何编译错误

### `AiCodegenServiceTest.java`
- 测试用例中 appId 从 `1L` 改为 `667L`，对应实际测试应用

---

## 修改原因与目的

- **Vue 工程化项目完整部署链路**：此前 Vue 项目生成后缺少自动构建步骤，无法直接部署。本次新增 `VueProjectBuilder` 并在生成完成后自动触发构建，打通了从 AI 生成到可访问静态页面的完整链路。
- **类型值规范化**：统一使用连字符风格（kebab-case），与 HTTP API 和前端惯例保持一致。
- **静态资源控制器重构**：原实现路径解析逻辑复杂且存在 Windows 路径兼容问题，重构后更简洁可靠。
- **接口方法修正**：`/app/get/vo` 使用 `@RequestBody` 接收参数，应为 POST 请求，原 GET 注解有误。

## 影响范围

- Vue 工程化项目生成后会自动触发 npm 构建，需确保服务器环境已安装 Node.js 和 npm
- `CodegenTypeEnum` 类型值变更为连字符风格，数据库中已存储的旧值（`multi_file`/`vue_project`）需同步迁移
- `/app/get/vo` 接口由 GET 改为 POST，前端调用方式需同步更新
- 静态资源访问 URL 格式由 `/{codeGenType}_{appId}/` 改为 `/{deployKey}/`，已部署应用的访问链接不受影响（deployKey 不变）
