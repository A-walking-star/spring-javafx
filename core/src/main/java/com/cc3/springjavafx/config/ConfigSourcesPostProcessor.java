package com.cc3.springjavafx.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件装配器：显式决定配置来源的优先级。
 *
 * <p>不使用 {@code @PropertySource} 是因为注解声明顺序与最终生效顺序在 Spring 内部并不直观
 * （实测把默认值放在最后声明时，默认值反而压过了使用方配置）。这里统一用
 * {@link MutablePropertySources#addLast} 装载，并按"先加入 = 优先级高"的顺序排列，结果可预期、可测试。</p>
 *
 * <p><b>优先级（高 → 低）</b>：</p>
 * <ol>
 *     <li>JVM 系统属性、环境变量（Spring 默认，位于最前）</li>
 *     <li>{@code classpath:app.yml}</li>
 *     <li>{@code classpath:app.properties}</li>
 *     <li>{@code classpath:spring-javafx/defaults.yml}（框架默认值）</li>
 * </ol>
 */
@Component
public class ConfigSourcesPostProcessor implements BeanFactoryPostProcessor, PriorityOrdered, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(ConfigSourcesPostProcessor.class);

    /** 使用方配置文件：数组靠后的优先级更高 */
    private static final String[] USER_LOCATIONS = {
            "classpath:app.properties",
            "classpath:app.yml",
    };

    /** 框架默认配置，优先级最低 */
    private static final String FRAMEWORK_DEFAULTS = "classpath:spring-javafx/defaults.yml";

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment) {
            this.environment = (ConfigurableEnvironment) environment;
        }
    }

    @Override
    public int getOrder() {
        // 早于 PropertySourcesPlaceholderConfigurer，保证 @Value 解析前配置已就位
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) {
            logger.warn("Environment 不可用，跳过配置文件装配");
            return;
        }
        MutablePropertySources sources = environment.getPropertySources();
        // 属性解析从头开始命中，因此"先加入的优先级更高"：先装使用方配置，最后装框架默认值
        List<String> loaded = new ArrayList<>();
        for (String location : USER_LOCATIONS) {
            if (loadInto(sources, location)) {
                loaded.add(location);
            }
        }
        if (loadInto(sources, FRAMEWORK_DEFAULTS)) {
            loaded.add(FRAMEWORK_DEFAULTS);
        }
        // 用 info 而非 debug：这一行是排查"配置没生效"的第一入口，默认级别就要看得见
        logger.info("配置装配完成（优先级从高到低）：{}", loaded);
    }

    /**
     * 加载一个配置文件到属性源末尾。
     *
     * <p>文件不存在时跳过（等价于 ignoreResourceNotFound）；文件存在但解析失败时**直接抛异常**，
     * 因为这种情况意味着使用方的配置全部丢失，继续运行只会得到一堆"页面找不到""配置不生效"的
     * 间接错误，比启动失败更难排查。</p>
     *
     * @return 是否实际加载
     */
    private boolean loadInto(MutablePropertySources sources, String location) {
        String path = location.startsWith("classpath:")
                ? location.substring("classpath:".length())
                : location;
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return false;
        }
        EncodedResource encoded = new EncodedResource(resource, "UTF-8");
        PropertySource<?> source;
        try {
            if (path.endsWith(".yml") || path.endsWith(".yaml")) {
                source = new YamlPropertySourceFactory().createPropertySource(path, encoded);
            } else {
                source = new ResourcePropertySource(path, encoded);
            }
        } catch (NoClassDefFoundError e) {
            throw new IllegalStateException(
                    "配置文件 " + location + " 存在，但 classpath 中缺少 YAML 解析依赖 snakeyaml（"
                            + e.getMessage() + "）。\n"
                            + "  修复方式：确认 org.yaml:snakeyaml 已在运行时 classpath 上；\n"
                            + "  Maven/IDEA 项目请执行 Reload All Maven Projects 后重新运行。", e);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "配置文件 " + location + " 解析失败：" + e, e);
        }
        if (sources.contains(source.getName())) {
            sources.remove(source.getName());
        }
        sources.addLast(source);
        logger.debug("已加载配置：{}", location);
        return true;
    }
}
