package com.hefng.mynocodebackend.model.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 历史对话 表定义层。
 *
 * @author https://github.com/hefng
 * @since 2026-03-22
 */
public class ChatHistoryTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 历史对话
     */
    public static final ChatHistoryTableDef CHAT_HISTORY = new ChatHistoryTableDef();

    /**
     * id
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 应用id
     */
    public final QueryColumn APP_ID = new QueryColumn(this, "appId");

    /**
     * 用户id
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "userId");

    /**
     * 是否删除
     */
    public final QueryColumn IS_DELETE = new QueryColumn(this, "isDelete");

    /**
     * 对话消息列表
     */
    public final QueryColumn MESSAGES = new QueryColumn(this, "messages");

    /**
     * 创建时间
     */
    public final QueryColumn CREATE_TIME = new QueryColumn(this, "createTime");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATE_TIME = new QueryColumn(this, "updateTime");

    /**
     * 对话消息类型
     */
    public final QueryColumn CHAT_MESSAGE_TYPE = new QueryColumn(this, "chatMessageType");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, APP_ID, USER_ID, MESSAGES, CHAT_MESSAGE_TYPE, CREATE_TIME, UPDATE_TIME, };

    public ChatHistoryTableDef() {
        super("my_nocode_backend", "chat_history");
    }

    private ChatHistoryTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public ChatHistoryTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new ChatHistoryTableDef("my_nocode_backend", "chat_history", alias));
    }

}
