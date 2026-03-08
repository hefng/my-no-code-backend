package com.hefng.mynocodebackend.mapper;

import com.hefng.mynocodebackend.model.entity.User;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 映射层。
 *
 * @author https://github.com/hefng
 * @since 2026-03-05
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
