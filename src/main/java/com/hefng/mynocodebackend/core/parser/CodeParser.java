package com.hefng.mynocodebackend.core.parser;

/**
 * 代码解析器接口
 */
public interface CodeParser<R> {

    /**
     * 解析代码字符串并返回对应的代码对象
     * @param content
     * @return
     */
    R parseCode(String content);

}
