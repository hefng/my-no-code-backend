package com.hefng.mynocodebackend.config;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

/**
 * MybatisFlex 代码生成器
 * @author hefng
 */
public class MybatisFlexCodegen {

    public static final String CREATE_TABLE = "chat_history";

    public static void main(String[] args) {
        Dict dict = YamlUtil.loadByPath("application.yml");
        Map<String, Object> databaseConfig = dict.getByPath("spring.datasource");
        String username = String.valueOf(databaseConfig.get("username"));
        String url = String.valueOf(databaseConfig.get("url"));
        String password = String.valueOf(databaseConfig.get("password"));

        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        //创建配置内容，两种风格都可以。
        GlobalConfig globalConfig = createGlobalConfig();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        //生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfig() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        //设置根包
        globalConfig.setBasePackage("com.hefng.mynocodebackend.generated");

        // 生成哪个Schema下的表
        globalConfig.setGenerateSchema("my_nocode_backend");
        //设置表前缀和只生成哪些表
        globalConfig.setGenerateTable(CREATE_TABLE);
        // 设置逻辑删除字段
        globalConfig.setLogicDeleteColumn("isDelete");

        //设置生成 entity 并启用 Lombok
        globalConfig.setEntityGenerateEnable(true);
        globalConfig.setEntityWithLombok(true);
        //设置项目的JDK版本
        globalConfig.setEntityJdkVersion(17);

        //设置生成 mapper
        globalConfig.setMapperGenerateEnable(true);

        // 注释配置
        // 设置生成的作者
        globalConfig.setAuthor("https://github.com/hefng");

        // 允许生成的代码产物类型
        globalConfig.enableEntity();
        globalConfig.enableMapper();
        globalConfig.enableController();
        globalConfig.enableService();
        globalConfig.enableServiceImpl();
        globalConfig.enableMapperXml();
        globalConfig.enableTableDef();

        return globalConfig;
    }
}