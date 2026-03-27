package com.hefng.mynocodebackend.core.saver;

import cn.hutool.core.io.FileUtil;
import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.ai.model.HTMLCodeResult;

import java.io.File;

public class HtmlCodeFileSaver extends CodeFileSaverTemplate<HTMLCodeResult> {

    @Override
    protected File writeToFile(String uniquePath, HTMLCodeResult content) {
        String htmlCode = content.getHtmlCode();
        if (htmlCode == null || htmlCode.isEmpty()) {
            return null;
        }
        String filePath = uniquePath + File.separator + "index.html";
        FileUtil.writeString(htmlCode, filePath, "UTF-8");
        return new File(filePath);
    }

    @Override
    protected String getType() {
        return CodegenTypeEnum.HTML.getType();
    }
}
