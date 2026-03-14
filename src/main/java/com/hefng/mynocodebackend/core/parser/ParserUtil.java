package com.hefng.mynocodebackend.core.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析工具类, 用于存放各种代码解析器的公共方法
 */
public class ParserUtil {

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

}
