package com.hefng.mynocodebackend.core.parser;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;
import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;

public class CodeParserExecutor {

     private static final CodeParser<HTMLCodeResult> HTML_CODE_PARSER = new HtmlCodeParser();

     private static final CodeParser<MultiFileCodeResult> MULTI_FILE_CODE_PARSER = new MultiFileCodeParser();

     public static Object parseCode(String content, CodegenTypeEnum codeTypeEnum) {
          if (codeTypeEnum == null) {
               throw new IllegalArgumentException("代码类型不能为空");
          }
          return switch (codeTypeEnum) {
               case HTML -> HTML_CODE_PARSER.parseCode(content);
               case MULTI_FILE -> MULTI_FILE_CODE_PARSER.parseCode(content);
               // VUE_PROJECT 的文件由 AI 通过 @Tool 直接写入，无需后端解析代码块
               case VUE_PROJECT -> null;
          };
     }
}
