package com.hefng.mynocodebackend.mapper;

import com.hefng.mynocodebackend.model.entity.App;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用 映射层。
 *
 * @author https://github.com/hefng
 * @since 2026-03-17
 */
@Mapper
public interface AppMapper extends BaseMapper<App> {

}
