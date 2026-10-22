package com.cc3.springjavafx.controller;

import javafx.stage.Stage;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * 内置子窗口控制器工厂：所有子窗口统一由 {@link NewStageWebController} 承载。
 *
 * <p>容器里只要出现任意自定义 {@link StageControllerFactory} 实现，框架会优先使用自定义实现
 * （见 {@code ContextUtils#createScene}），本类随即失效。</p>
 */
@Component
public class DefaultStageControllerFactory implements StageControllerFactory {

    @Override
    public GenericBeanDefinition create(String key, Stage stage, BaseController parentController,
                                        String resourceUrl, double width, double height) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(NewStageWebController.class);
        beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(key);
        beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(stage);
        beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(parentController);
        beanDefinition.getPropertyValues().add("resourceUrl", resourceUrl);
        beanDefinition.getPropertyValues().add("width", width);
        beanDefinition.getPropertyValues().add("height", height);
        return beanDefinition;
    }
}
