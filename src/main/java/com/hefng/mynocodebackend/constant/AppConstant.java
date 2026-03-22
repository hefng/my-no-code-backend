package com.hefng.mynocodebackend.constant;

/**
 * 应用模块常量
 */
public interface AppConstant {

    /**
     * 默认优先级, 用于普通应用排序
     */
    int DEFAULT_PRIORITY = 0;

    /**
     * 精选应用优先级, 用于在首页展示优先展示
     */
    int MAX_PRIORITY = 99;

    String DEFAULT_DIR = System.getProperty("user.dir") + "\\tmp\\";

    String CODEGEN_DIR = DEFAULT_DIR + "codegen\\";

    String DEPLOY_DIR = DEFAULT_DIR + "code_deploy\\";

    String CODE_DEPLOY_HOST = "http://localhost/";


    String DEFAULT_CODEGEN_TYPE = "html";
}
