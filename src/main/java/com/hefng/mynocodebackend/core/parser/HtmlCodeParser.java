package com.hefng.mynocodebackend.core.parser;

import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;

/**
 * HTML 代码解析器
 *
 * @author hefng
 */
public class HtmlCodeParser implements CodeParser<HTMLCodeResult> {

    /**
     * 从文本中提取 HTML 代码块
     * 支持格式: ```html...``` 或 ```...```
     *
     * @param content 包含代码块的文本
     * @return HTML 代码内容，如果没有找到则返回 null
     */
    @Override
    public HTMLCodeResult parseCode(String content) {
        // 使用 ParserUtil 提取 HTML 代码块
        String htmlChunk = ParserUtil.extractCodeByType(content, "html");
        HTMLCodeResult htmlCodeResult = new HTMLCodeResult();
        htmlCodeResult.setHtmlCode(htmlChunk);
        return htmlCodeResult;
    }
}
