package com.hefng.mynocodebackend.core.parser;

import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;

import static com.hefng.mynocodebackend.core.parser.ParserUtil.extractCodeByType;

/**
 * 多文件代码解析器
 *
 * @author hefng
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    @Override
    public MultiFileCodeResult parseCode(String content) {
        MultiFileCodeResult result = new MultiFileCodeResult();

        if (content == null || content.isEmpty()) {
            return result;
        }
        // 提取 HTML、CSS、JavaScript 代码块, 封装到 MultiFileCodeResult 对象中
        result.setHtmlCode(extractCodeByType(content, "html"));
        result.setCssCode(extractCodeByType(content, "css"));
        result.setJsCode(extractCodeByType(content, "javascript"));

        return result;
    }
}
