# 代码变更日志

## 变动时间
- 日期时间：2026-04-17 18:45:26 +08:00

## 变动文件列表
- pom.xml
- src/main/java/com/hefng/mynocodebackend/core/builder/VueProjectBuilder.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/CodeGenWorkflow.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/WorkFlowApp.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/demo/SimpleGraphDemo.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/demo/SimpleStatefulWorkflowApp.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/entity/ImageResource.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/entity/enums/ImageCategoryEnum.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/factory/ImageCollectionServiceFactory.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/CodeGeneratorNode.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/ImageCollectorNode.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/ProjectBuilderNode.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/PromptEnhancerNode.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/SmartRouterNode.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/service/AiImageCollectService.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/state/WorkflowContext.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/tools/LogoGeneratorTool.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/tools/MermaidDiagramTool.java
- src/main/java/com/hefng/mynocodebackend/langgraph4j/tools/PexelsImageSearchTool.java
- src/main/java/com/hefng/mynocodebackend/utils/SpringContextUtil.java
- src/main/resources/application.yml
- src/main/resources/prompts/image-collection-system-prompt.txt
- src/test/java/com/hefng/mynocodebackend/ai/AiCodegenServiceTest.java
- src/test/java/com/hefng/mynocodebackend/langgraph4j/AiImageCollectServiceTest.java
- src/test/java/com/hefng/mynocodebackend/langgraph4j/CodeGenWorkflowTest.java
- src/test/java/com/hefng/mynocodebackend/langgraph4j/tools/LogoGeneratorToolTest.java
- src/test/java/com/hefng/mynocodebackend/langgraph4j/tools/MermaidDiagramToolTest.java
- src/test/java/com/hefng/mynocodebackend/langgraph4j/tools/PexelsImageSearchToolTest.java

## 每个文件的主要修改内容
- pom.xml：补充与 LangGraph4j / 图像处理相关依赖与构建配置，支持新增工作流模块编译与测试。
- src/main/java/com/hefng/mynocodebackend/core/builder/VueProjectBuilder.java：增强项目构建参数处理，适配新的工作流上下文输入。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/CodeGenWorkflow.java：完善代码生成工作流编排，串联路由、提示增强、代码生成与项目构建节点。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/WorkFlowApp.java：新增工作流启动/装配入口，统一节点与状态对象初始化。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/demo/SimpleGraphDemo.java：新增图执行演示样例，验证基本图结构与节点连线能力。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/demo/SimpleStatefulWorkflowApp.java：补充有状态工作流演示，验证状态在多节点间传递。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/entity/ImageResource.java：新增图片资源实体字段，承载采集结果与元信息。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/entity/enums/ImageCategoryEnum.java：新增图片分类枚举，标准化图片用途分类。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/factory/ImageCollectionServiceFactory.java：新增服务工厂，根据配置选择图片采集服务实现。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/CodeGeneratorNode.java：完善代码生成节点输入输出，增强与上下游节点对接。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/ImageCollectorNode.java：新增/强化图片采集节点逻辑，将图片资源注入工作流上下文。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/ProjectBuilderNode.java：扩展项目构建节点，处理图片与代码生成结果的落盘构建。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/PromptEnhancerNode.java：完善提示词增强策略，提升下游生成稳定性。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/node/SmartRouterNode.java：优化路由节点决策逻辑，按任务类型分流执行路径。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/service/AiImageCollectService.java：增强图片采集服务接口/实现，支撑多来源图片检索。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/state/WorkflowContext.java：扩展上下文字段与状态管理能力，承载新增流程数据。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/tools/LogoGeneratorTool.java：新增 Logo 生成工具，提供图像生成能力封装。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/tools/MermaidDiagramTool.java：新增 Mermaid 图生成工具，用于流程/架构图产物生成。
- src/main/java/com/hefng/mynocodebackend/langgraph4j/tools/PexelsImageSearchTool.java：新增 Pexels 图片搜索工具，实现外部图库检索。
- src/main/java/com/hefng/mynocodebackend/utils/SpringContextUtil.java：新增 Spring 上下文工具，便于在工具/节点中动态获取 Bean。
- src/main/resources/application.yml：补充工作流与图片采集相关基础配置项。
- src/main/resources/prompts/image-collection-system-prompt.txt：新增图片采集系统提示词模板。
- src/test/java/com/hefng/mynocodebackend/ai/AiCodegenServiceTest.java：调整既有测试以匹配新配置与调用路径。
- src/test/java/com/hefng/mynocodebackend/langgraph4j/AiImageCollectServiceTest.java：新增/完善图片采集服务测试，覆盖典型输入输出。
- src/test/java/com/hefng/mynocodebackend/langgraph4j/CodeGenWorkflowTest.java：新增工作流编排测试，验证关键节点串联与状态流转。
- src/test/java/com/hefng/mynocodebackend/langgraph4j/tools/LogoGeneratorToolTest.java：新增 Logo 工具测试，验证工具调用与结果结构。
- src/test/java/com/hefng/mynocodebackend/langgraph4j/tools/MermaidDiagramToolTest.java：新增 Mermaid 工具测试，验证图文本生成逻辑。
- src/test/java/com/hefng/mynocodebackend/langgraph4j/tools/PexelsImageSearchToolTest.java：新增 Pexels 工具测试，验证检索参数与响应处理。

## 修改原因和目的
- 引入基于 LangGraph4j 的可编排后端工作流能力，解决原有流程扩展性与可维护性不足的问题。
- 将图片采集、提示增强、代码生成与项目构建模块化为独立节点，提高可组合性和可测试性。
- 通过新增工具类与测试用例，降低集成外部能力（图像搜索/生成、图文本生成）的接入成本与回归风险。

## 可能的影响范围
- 影响代码生成链路：工作流入口、节点执行顺序与状态传递机制发生变化。
- 影响配置层：pplication.yml 新增配置项后，需要保证各环境配置完整。
- 影响测试与CI：新增测试集与依赖后，构建时间和测试执行范围会增加。
- 影响运行时集成：涉及外部图片服务与AI工具调用，需关注网络权限、鉴权配置与超时策略。
