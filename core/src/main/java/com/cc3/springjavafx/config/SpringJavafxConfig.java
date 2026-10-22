package com.cc3.springjavafx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * 框架的装配入口。
 *
 * <p>使用方通常不需要直接接触本类：继承 {@code com.cc3.springjavafx.JavaFxApplication}
 * 即可自动加载。只有在需要把框架嵌入已有 Spring 容器时，才需要手动 {@code @Import} 本类。</p>
 *
 * <p>配置文件由 {@link ConfigSourcesPostProcessor} 统一装配，优先级见该类说明。</p>
 */
@Configuration
@ComponentScan("com.cc3.springjavafx")
public class SpringJavafxConfig {

    /**
     * 占位符配置器（static 确保最先初始化，早于所有 @Value 注入）
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
