package com.hefng.mynocodebackend.core.saver;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;
import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;
import com.hefng.mynocodebackend.core.parser.CodeParser;
import com.hefng.mynocodebackend.core.parser.HtmlCodeParser;
import com.hefng.mynocodebackend.core.parser.MultiFileCodeParser;

import java.io.File;

/**
 * 代码文件保存执行器, 根据代码类型选择对应的代码文件保存器进行保存
 *
 * @author hefng
 */
public class CodeFileSaverExecutor {

     private static final CodeFileSaverTemplate<HTMLCodeResult> HTML_CODE_SAVER = new HtmlCodeFileSaver();

     private static final CodeFileSaverTemplate<MultiFileCodeResult> MULTI_FILE_CODE_SAVER = new MultiFileCodeFileSaver();

     /**
      * 保存代码文件的执行方法, 根据代码类型选择对应的代码文件保存器进行保存
      * @param content 代码生成结果对象, 包含需要保存的代码内容
      * @param codeTypeEnum 代码类型枚举, 如 HTML、MULTI_FILE 等
      * @return
      */
     public static File saveCodeFile(Object content, CodegenTypeEnum codeTypeEnum) {
          if (codeTypeEnum == null) {
               throw new IllegalArgumentException("代码类型不能为空");
          }
          return switch (codeTypeEnum) {
               case HTML -> HTML_CODE_SAVER.saveCodeFile((HTMLCodeResult) content);
               case MULTI_FILE -> MULTI_FILE_CODE_SAVER.saveCodeFile((MultiFileCodeResult) content);
          };
     }
}
