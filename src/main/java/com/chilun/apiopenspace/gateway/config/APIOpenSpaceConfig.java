package com.chilun.apiopenspace.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 配置类，用于配置APIOpenSpace的组件扫描和自动装配
 *
 * @author 齿轮
 * @date 2024-02-20-23:07
 */
@Configuration
@Import({com.chilun.apiopenspace.starter.QuickAccessAutoConfiguration.class})
public class APIOpenSpaceConfig {
}
