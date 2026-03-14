package com.hefng.mynocodebackend.core;

import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析工具类, 将 AI 生成的代码字符串解析成对应的代码文件对象
 *
 * @author hefng
 */
@Deprecated
public class CodeParser {

    /**
     * 从文本中提取 HTML 代码块
     * 支持格式: ```html...``` 或 ```...```
     *
     * @param text 包含代码块的文本
     * @return HTML 代码内容，如果没有找到则返回 null
     */
    public static String extractHtmlCode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 正则表达式匹配 ```html...``` 或 ```...``` 代码块
        // (?s) 表示 DOTALL 模式，让 . 匹配换行符
        // (?:html)? 表示可选的 html 标识
        Pattern pattern = Pattern.compile("```(?:html)?\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            // 返回第一个匹配的代码块内容（去除首尾空白）
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 检查文本中是否包含 HTML 代码块
     *
     * @param text 待检查的文本
     * @return 如果包含 HTML 代码块返回 true，否则返回 false
     */
    public static boolean containsHtmlCode(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        Pattern pattern = Pattern.compile("```(?:html)?\\s*\\n.*?```", Pattern.DOTALL);
        return pattern.matcher(text).find();
    }

    /**
     * 从文本中提取指定类型的代码块
     *
     * @param text 包含代码块的文本
     * @param codeType 代码类型（如 html、css、javascript）
     * @return 代码内容，如果没有找到则返回 null
     */
    public static String extractCodeByType(String text, String codeType) {
        if (text == null || text.isEmpty() || codeType == null) {
            return null;
        }

        // 正则表达式匹配指定类型的代码块
        String regex = "```" + codeType + "\\s*\\n(.*?)```";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 从文本中提取 HTML、CSS、JavaScript 三种代码块
     *
     * @param text 包含代码块的文本
     * @return 包含三种代码的 MultiCodeResult 对象
     */
    public static MultiFileCodeResult extractMultiFileCode(String text) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        
        if (text == null || text.isEmpty()) {
            return result;
        }
        // 提取 HTML、CSS、JavaScript 代码块, 封装到 MultiFileCodeResult 对象中
        result.setHtmlCode(extractCodeByType(text, "html"));
        result.setCssCode(extractCodeByType(text, "css"));
        result.setJsCode(extractCodeByType(text, "javascript"));

        return result;
    }

    /**
     * 从文本中提取所有 HTML 代码块
     *
     * @param text 包含代码块的文本
     * @return HTML 代码内容列表
     */
    public static List<String> extractAllHtmlCodes(String text) {
        List<String> htmlCodes = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return htmlCodes;
        }

        // 正则表达式匹配所有 ```html...``` 或 ```...``` 代码块
        Pattern pattern = Pattern.compile("```(?:html)?\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            htmlCodes.add(matcher.group(1).trim());
        }

        return htmlCodes;
    }

}
