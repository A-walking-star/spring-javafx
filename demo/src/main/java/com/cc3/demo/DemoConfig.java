package com.cc3.demo;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 使用方自己的组件扫描配置。
 *
 * <p>框架只扫描 {@code com.cc3.springjavafx}，使用方的包必须自己声明，
 * 这样使用方的 Bean 与框架 Bean 互不干扰。</p>
 */
@Configuration
@ComponentScan("com.cc3.demo")
public class DemoConfig {
}
