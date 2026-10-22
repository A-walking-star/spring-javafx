package com.cc3.springjavafx.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 配置装配：使用方 app.yml 覆盖框架内置 defaults.yml，且未覆盖的键仍取默认值。
 *
 * <p>这是历史踩坑点（{@code addLast} 是"最低优先级"），用测试锁住。</p>
 */
class ConfigSourcesPostProcessorTest {

    @Test
    void appYmlOverridesFrameworkDefaults() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(ConfigSourcesPostProcessor.class);
            context.refresh();

            ConfigurableEnvironment env = context.getEnvironment();
            // test/resources/app.yml
            assertEquals("from-test-app-yml", env.getProperty("app.name"));
            assertEquals("1111", env.getProperty("app.defaultWidth"));
            // spring-javafx/defaults.yml
            assertEquals("0", env.getProperty("app.exitCode"));
            assertEquals("提示", env.getProperty("app.dialog.title"));
        }
    }
}
