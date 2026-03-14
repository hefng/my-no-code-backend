package com.hefng.mynocodebackend.core.saver;

import cn.hutool.core.io.FileUtil;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.model.MultiFileCodeResult;
import opennlp.tools.util.StringUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 多文件代码文件保存器
 *
 * @author hefng
 */
public class MultiFileCodeFileSaver extends CodeFileSaverTemplate<MultiFileCodeResult> {
    @Override
    protected File writeToFile(String uniquePath, MultiFileCodeResult multiFileCodeResult) {
        String htmlCode = multiFileCodeResult.getHtmlCode();
        String cssCode = multiFileCodeResult.getCssCode();
        String jsCode = multiFileCodeResult.getJsCode();
        if (!StringUtil.isEmpty(htmlCode)) {
            FileUtil.writeString(htmlCode, uniquePath + File.separator + "index.html", StandardCharsets.UTF_8);
        }
        if (!StringUtil.isEmpty(cssCode)) {
            FileUtil.writeString(htmlCode, uniquePath + File.separator + "index.css", StandardCharsets.UTF_8);
        }
        if (!StringUtil.isEmpty(jsCode)) {
            FileUtil.writeString(htmlCode, uniquePath + File.separator + "index.js", StandardCharsets.UTF_8);
        }
        return new File(uniquePath);
    }

    @Override
    protected String getType() {
        return CodegenTypeEnum.MULTI_FILE.getType();
    }
}
