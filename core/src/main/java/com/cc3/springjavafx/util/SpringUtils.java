package com.cc3.springjavafx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * spring工具类 方便在非spring管理环境中获取bean
 */
@Component
public final class SpringUtils implements BeanFactoryPostProcessor
{
    private static final Logger logger = LoggerFactory.getLogger(SpringUtils.class);

    /** Spring应用上下文环境 */
    private static ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        SpringUtils.beanFactory = beanFactory;
    }

    /**
     * 获取对象
     *
     * @param name
     * @return Object 一个以所给名字注册的bean的实例
     * @throws BeansException
     *
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException
    {
        return (T) beanFactory.getBean(name);
    }

    /**
     * 获取类型为requiredType的对象
     *
     * @param clz
     * @throws BeansException
     *
     */
    public static <T> T getBean(Class<T> clz) throws BeansException
    {
        T result = (T) beanFactory.getBean(clz);
        return result;
    }

    /**
     * 如果BeanFactory包含一个与所给名称匹配的bean定义，则返回true
     *
     * @param name
     * @return boolean
     */
    public static boolean containsBean(String name)
    {
        return beanFactory.containsBean(name);
    }

    /**
     * 判断以给定名字注册的bean定义是一个singleton还是一个prototype。 如果与给定名字相应的bean定义没有被找到，将会抛出一个异常（NoSuchBeanDefinitionException）
     *
     * @param name
     * @return boolean
     * @throws NoSuchBeanDefinitionException
     *
     */
    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException
    {
        return beanFactory.isSingleton(name);
    }

    /**
     * @param name
     * @return Class 注册对象的类型
     * @throws NoSuchBeanDefinitionException
     *
     */
    public static Class<?> getType(String name) throws NoSuchBeanDefinitionException
    {
        return beanFactory.getType(name);
    }

    /**
     * 如果给定的bean名字在bean定义中有别名，则返回这些别名
     *
     * @param name
     * @throws NoSuchBeanDefinitionException
     *
     */
    public static String[] getAliases(String name) throws NoSuchBeanDefinitionException
    {
        return beanFactory.getAliases(name);
    }

    /**
     * 获取aop代理对象
     * 
     * @param invoker
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAopProxy(T invoker)
    {
        return (T) AopContext.currentProxy();
    }

    /**
     * 注册一个已存在的单例对象到 Spring IoC 容器
     *
     * @param beanName  bean 名称
     * @param singleton 单例对象
     */
    public static void registerSingleton(String beanName, Object singleton) {
        beanFactory.registerSingleton(beanName, singleton);
    }

    /**
     * 获取所有实现指定类型的 bean
     *
     * @param type bean 类型
     * @return bean 名称 -> bean 实例的 Map
     */
    public static <T> java.util.Map<String, T> getBeansOfType(Class<T> type) {
        return beanFactory.getBeansOfType(type);
    }

    /**
     * 按类型创建 bean 实例集合（适用于需要"每次拿到新实例"的场景，例如窗口级隔离的桥接器）
     *
     * <p>prototype 作用域的 bean 每次调用都会创建新实例；若 bean 是单例，则返回容器中的共享实例
     * 并打印告警，因为共享实例无法做到按窗口隔离。</p>
     *
     * @param type bean 类型
     * @return bean 名称 -> bean 实例的 Map，保持容器中的注册顺序
     */
    public static <T> Map<String, T> createBeansOfType(Class<T> type) {
        Map<String, T> beans = new LinkedHashMap<>();
        for (String beanName : beanFactory.getBeanNamesForType(type)) {
            if (beanFactory.isSingleton(beanName)) {
                logger.warn("bean[{}] 为单例，多次获取将返回同一实例；如需每个窗口持有独立实例请标注 @Scope(\"prototype\")", beanName);
            }
            beans.put(beanName, beanFactory.getBean(beanName, type));
        }
        return beans;
    }

    /**
     * 销毁并移除指定名称的 bean。
     *
     * @param beanName bean名称，null 或不存在时直接返回
     */
    public static void closeBean(String beanName) {
        if (beanName == null || !beanFactory.containsBean(beanName)) {
            return;
        }
        BeanDefinitionRegistry registry = beanDefinitionRegistry();
        boolean hasDefinition = registry != null && registry.containsBeanDefinition(beanName);
        if (beanFactory.containsSingleton(beanName)) {
            Object singleton = beanFactory.getSingleton(beanName);
            if (beanFactory instanceof DefaultSingletonBeanRegistry) {
                if (!hasDefinition) {
                    // registerSingleton 手动注册的实例没有销毁适配器，destroySingleton 不会回调，先补一次
                    beanFactory.destroyBean(singleton);
                }
                // 触发 @PreDestroy / DisposableBean.destroy()，并从单例缓存移除
                ((DefaultSingletonBeanRegistry) beanFactory).destroySingleton(beanName);
            } else {
                beanFactory.destroyBean(beanName, singleton);
            }
        }
        if (hasDefinition) {
            registry.removeBeanDefinition(beanName);
            logger.debug("bean[{}] 已销毁并从容器移除", beanName);
        }
    }

    /**
     * 销毁并移除指定的 bean（按实例反查 bean 名称）。
     *
     * @param bean bean对象，null 时直接返回
     */
    public static void closeBean(Object bean) {
        if (bean == null) {
            return;
        }
        List<String> beanNames = findSingletonNames(bean);
        if (beanNames.isEmpty()) {
            // 非单例：容器不持有该实例，只能执行销毁回调
            beanFactory.destroyBean(bean);
            return;
        }
        for (String beanName : beanNames) {
            closeBean(beanName);
        }
    }

    /**
     * 按实例反查单例名称（同一个实例可能以多个名称注册）
     *
     * @param bean bean对象
     * @return bean名称列表，非单例返回空列表
     */
    public static List<String> findSingletonNames(Object bean) {
        List<String> beanNames = new ArrayList<>();
        for (String beanName : beanFactory.getSingletonNames()) {
            if (beanFactory.getSingleton(beanName) == bean) {
                beanNames.add(beanName);
            }
        }
        return beanNames;
    }

    /**
     * 清空静态持有的 BeanFactory。
     *
     * <p>容器关闭时必须调用：静态字段会强引用整个容器（及其所有单例、WebView、Stage），
     * 不清理的话，反复创建/关闭容器的场景（测试、重启内嵌容器）会持续泄漏。</p>
     */
    public static void clear() {
        beanFactory = null;
    }

    /**
     * 获取 BeanDefinitionRegistry（非注册表实现时返回 null）
     *
     * @return BeanDefinitionRegistry
     */
    private static BeanDefinitionRegistry beanDefinitionRegistry() {
        return beanFactory instanceof BeanDefinitionRegistry ? (BeanDefinitionRegistry) beanFactory : null;
    }

    /**
     * 动态注册BeanDefinition，由Spring完整管理生命周期
     *
     * @param beanName       bean名称
     * @param beanDefinition bean定义
     */
    public static void registerBeanDefinition(String beanName, GenericBeanDefinition beanDefinition) {
        ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(beanName, beanDefinition);
    }
}