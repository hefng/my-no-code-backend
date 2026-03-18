package com.hefng.mynocodebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.hefng.mynocodebackend.mapper")
public class MyNoCodeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyNoCodeBackendApplication.class, args);
    }

}
