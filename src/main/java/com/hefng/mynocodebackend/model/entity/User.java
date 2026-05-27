package com.hefng.mynocodebackend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Builder;
import lombok.Data;

/**
 * 用户 实体类。
 *
 * @author https://github.com/hefng
 * @since 2026-03-05
 */
@Data
@Table(value = "user", schema = "my_nocode_backend")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 账号
     */
    @Column("userAccount")
    private String userAccount;

    /**
     * 密码
     */
    @Column("userPassword")
    private String userPassword;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 用户头像
     */
    @Column("userAvatar")
    private String userAvatar;

    /**
     * 用户简介
     */
    @Column("userProfile")
    private String userProfile;

    /**
     * GitHub 用户 ID（用于 OAuth 登录关联）
     */
    @Column("githubId")
    private Long githubId;

    /**
     * 用户角色：user/admin/ban
     */
    @Column("userRole")
    private String userRole;

    /**
     * 最大可创建应用数
     */
    @Column("appMaxCount")
    private Integer appMaxCount;

    /**
     * 已创建应用数
     */
    @Column("appUsedCount")
    private Integer appUsedCount;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
