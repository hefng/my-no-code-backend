-- 创建数据库
create
    database if not exists my_nocode_backend;

use my_nocode_backend;


-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    username     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_username (username),
    unique index idx_userAccount (userAccount)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table if not exists app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                           not null comment '应用名称',
    appDesc      varchar(512)                           null comment '应用描述',
    appCover     varchar(1024)                          null comment '应用封面',
    codegenType  varchar(256)                           null comment '代码生成类型',
    initPrompt   varchar(512)                           not null comment '初始提示词',
    deployedKey  varchar(256)                           null comment '部署key',
    deployedTime datetime     default CURRENT_TIMESTAMP null comment '部署时间',
    appOwnerId   bigint                                 not null comment '应用所有者id',
    priority     int          default 0                 not null comment '优先级',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_appName (appName),
    index idx_appOwnerId (appOwnerId),
    unique index idx_deployedKey (deployedKey)
) comment '应用' collate = utf8mb4_unicode_ci;