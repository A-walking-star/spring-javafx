package com.cc3.springjavafx.controller;

import javafx.stage.Stage;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * 子窗口控制器工厂：决定 {@code ContextUtils.createScene(...)} 创建出来的窗口由哪个控制器承载。
 *
 * <p>返回 {@link GenericBeanDefinition} 而不是控制器实例，是为了让子窗口控制器依旧由 Spring
 * 管理生命周期（{@code @PostConstruct} / {@code @PreDestroy} / 依赖注入都保持有效）。</p>
 *
 * <p>替换方式：实现本接口并注册为 Bean（例如 {@code @Component}），框架会自动使用；
 * 容器中不存在自定义实现时使用内置的 {@link DefaultStageControllerFactory}。</p>
 */
public interface StageControllerFactory {

    /**
     * 为一次新建窗口构造控制器定义。
     *
     * @param key             窗口唯一 key（同时用于 {@code __controllerKey:<key>} 的 Bean 名称）
     * @param stage           新建的 Stage（尚未显示）
     * @param parentController 父窗口控制器，{@code null} 表示无父窗口
     * @param resourceUrl     页面地址
     * @param width           窗口宽度
     * @param height          窗口高度
     * @return 控制器 Bean 定义
     */
    GenericBeanDefinition create(String key, Stage stage, BaseController parentController,
                                 String resourceUrl, double width, double height);
}
