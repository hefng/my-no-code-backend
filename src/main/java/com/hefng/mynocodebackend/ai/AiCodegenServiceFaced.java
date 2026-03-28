package com.hefng.mynocodebackend.ai;

import cn.hutool.json.JSONUtil;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;
import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.core.parser.CodeParserExecutor;
import com.hefng.mynocodebackend.core.saver.CodeFileSaverExecutor;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.model.enums.SseEventTypeEnum;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.util.Map;

/**
 * AI 代码生成服务门面类
 * <p>
 * 统一入口，根据 codegenType 路由到不同的生成策略：
 * - HTML / MULTI_FILE：使用 TokenStream，支持深度思考过程输出（thought/answer 事件拆分），
 *   流结束后解析代码块并保存文件
 * - VUE_PROJECT：深度推理模型 + @Tool 文件保存 + thought/answer 事件拆分
 * <p>
 * 所有流式输出格式统一为：{"event": "thought"|"answer", "d": "内容片段"}
 *
 * @author hefng
 */
@Service
@Slf4j
public class AiCodegenServiceFaced {

    @Resource
    private AiCodegenService aiCodegenService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectCodegenServiceFactory vueProjectCodegenServiceFactory;

    /**
     * 根据用户输入的需求生成代码并保存到文件（非流式，仅 HTML/MULTI_FILE 使用）
     */
    public File generateAndSaveCode(String userMessage, CodegenTypeEnum codegenTypeEnum, Long appId) {
        if (codegenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        return switch (codegenTypeEnum) {
            case HTML -> {
                HTMLCodeResult htmlCodeResult = aiCodegenService.generateHtml(userMessage, 1L);
                yield CodeFileSaverExecutor.saveCodeFile(htmlCodeResult, CodegenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodegenService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.saveCodeFile(multiFileCodeResult, CodegenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "Vue工程化项目仅支持流式生成");
        };
    }

    /**
     * 根据用户输入的需求生成代码并保存到文件（流式输出）
     * <p>
     * 返回的 Flux<String> 中每个元素是 JSON 字符串，格式统一为：
     * {"event": "thought"|"answer", "d": "内容片段"}
     * <p>
     * - thought：深度推理模型的思考过程（若模型不支持则不会出现）
     * - answer：最终代码内容片段
     * <p>
     * 注意：VUE_PROJECT 的文件保存由 AI 通过 @Tool 调用完成；HTML/MULTI_FILE 在流结束后解析代码块保存。
     */
    public Flux<String> generateAndSaveCodeWithStream(String userMessage, CodegenTypeEnum codegenTypeEnum, Long appId) {
        if (codegenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        AiCodegenService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        return switch (codegenTypeEnum) {
            case HTML -> generateCodeStream(
                    aiCodeGeneratorService.generateHtmlStream(userMessage),
                    CodegenTypeEnum.HTML, appId);
            case MULTI_FILE -> generateCodeStream(
                    aiCodeGeneratorService.generateMultiFileCodeStream(userMessage),
                    CodegenTypeEnum.MULTI_FILE, appId);
            case VUE_PROJECT -> generateVueProjectStream(userMessage, appId);
        };
    }

    /**
     * Vue 工程化项目流式生成
     * <p>
     * 使用 TokenStream 的 onPartialThinking / onPartialResponse 回调分别获取
     * 深度推理模型的思考过程和最终答案，通过 Sinks.Many 桥接为 Flux<String>。
     * <p>
     * 相比之前基于 <think> 标签的字符串解析方案，此方案直接从 LangChain4j 的事件回调
     * 中获取 reasoning_content，不依赖标签是否完整，更可靠。
     */
    private Flux<String> generateVueProjectStream(String userMessage, Long appId) {
        VueProjectCodegenService service = vueProjectCodegenServiceFactory.getService(appId);

        // Sinks.Many 作为 Flux 的发布者，LATEST 背压策略适合 SSE 场景
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        service.generateVueProjectStream(userMessage)
                .onPartialThinking(partialThinking -> {
                    String text = partialThinking.text();
                    if (text != null && !text.isEmpty()) {
                        sink.tryEmitNext(buildEventJson(SseEventTypeEnum.THOUGHT, text));
                    }
                })
                .onPartialResponse(partialResponse -> {
                    if (partialResponse != null && !partialResponse.isEmpty()) {
                        sink.tryEmitNext(buildEventJson(SseEventTypeEnum.ANSWER, partialResponse));
                    }
                })
                .onCompleteResponse(response -> sink.tryEmitComplete())
                .onError(error -> {
                    log.error("[VueProject] 流式生成异常, appId={}", appId, error);
                    sink.tryEmitError(error);
                })
                .start();

        return sink.asFlux();
    }

    /**
     * 构建 SSE 事件 JSON 字符串
     * 格式：{"event": "thought"|"answer", "d": "内容"}
     * <p>
     * 前端通过 event 字段区分思考流和答案流，通过 d 字段获取内容。
     * 与现有 HTML 生成的 {"d": "..."} 格式保持向后兼容（前端可通过 event 字段是否存在来判断类型）。
     */
    private String buildEventJson(SseEventTypeEnum eventType, String content) {
        return JSONUtil.toJsonStr(Map.of("event", eventType.getValue(), "d", content));
    }

    /**
     * HTML / MULTI_FILE 流式生成，支持深度思考过程输出
     * <p>
     * 与 VUE_PROJECT 逻辑相同：通过 onPartialThinking/onPartialResponse 分别推送 thought/answer 事件。
     * 流结束后收集完整 answer 内容，解析代码块并保存文件。
     */
    private Flux<String> generateCodeStream(TokenStream tokenStream,
                                            CodegenTypeEnum codegenTypeEnum, Long appId) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder answerBuilder = new StringBuilder();

        tokenStream
                .onPartialThinking(partialThinking -> {
                    String text = partialThinking.text();
                    if (text != null && !text.isEmpty()) {
                        sink.tryEmitNext(buildEventJson(SseEventTypeEnum.THOUGHT, text));
                    }
                })
                .onPartialResponse(partialResponse -> {
                    if (partialResponse != null && !partialResponse.isEmpty()) {
                        answerBuilder.append(partialResponse);
                        sink.tryEmitNext(buildEventJson(SseEventTypeEnum.ANSWER, partialResponse));
                    }
                })
                .onCompleteResponse(response -> {
                    try {
                        String code = answerBuilder.toString();
                        Object parseResult = CodeParserExecutor.parseCode(code, codegenTypeEnum);
                        CodeFileSaverExecutor.saveCodeFile(parseResult, codegenTypeEnum, appId);
                    } catch (Exception e) {
                        log.error("[{}] 代码解析/保存失败, appId={}", codegenTypeEnum, appId, e);
                        sink.tryEmitError(new BusinessException(ErrorCode.OPERATION_ERROR, "代码生成失败: " + e.getMessage()));
                        return;
                    }
                    sink.tryEmitComplete();
                })
                .onError(error -> {
                    log.error("[{}] 流式生成异常, appId={}", codegenTypeEnum, appId, error);
                    sink.tryEmitError(error);
                })
                .start();

        return sink.asFlux();
    }
}
