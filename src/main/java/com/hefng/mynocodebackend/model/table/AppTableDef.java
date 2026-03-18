package com.hefng.mynocodebackend.model.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 应用 表定义层。
 *
 * @author https://github.com/hefng
 * @since 2026-03-17
 */
public class AppTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用
     */
    public static final AppTableDef APP = new AppTableDef();

    /**
     * id
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 应用描述
     */
    public final QueryColumn APP_DESC = new QueryColumn(this, "appDesc");

    /**
     * 应用名称
     */
    public final QueryColumn APP_NAME = new QueryColumn(this, "appName");

    /**
     * 应用封面
     */
    public final QueryColumn APP_COVER = new QueryColumn(this, "appCover");

    /**
     * 是否删除
     */
    public final QueryColumn IS_DELETE = new QueryColumn(this, "isDelete");

    /**
     * 优先级
     */
    public final QueryColumn PRIORITY = new QueryColumn(this, "priority");

    /**
     * 应用所有者id
     */
    public final QueryColumn APP_OWNER_ID = new QueryColumn(this, "appOwnerId");

    /**
     * 创建时间
     */
    public final QueryColumn CREATE_TIME = new QueryColumn(this, "createTime");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATE_TIME = new QueryColumn(this, "updateTime");

    /**
     * 代码生成类型
     */
    public final QueryColumn CODEGEN_TYPE = new QueryColumn(this, "codegenType");

    /**
     * 部署key
     */
    public final QueryColumn DEPLOYED_KEY = new QueryColumn(this, "deployedKey");

    /**
     * 部署时间
     */
    public final QueryColumn DEPLOYED_TIME = new QueryColumn(this, "deployedTime");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, APP_NAME, APP_DESC, APP_COVER, CODEGEN_TYPE, DEPLOYED_KEY, DEPLOYED_TIME, APP_OWNER_ID, PRIORITY, CREATE_TIME, UPDATE_TIME, };

    public AppTableDef() {
        super("my_nocode_backend", "app");
    }

    private AppTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public AppTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new AppTableDef("my_nocode_backend", "app", alias));
    }

}
