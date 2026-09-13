package com.campusrepair.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.campusrepair.repository")
public class MybatisPlusConfig { }
