package com.cc3.demo;

import com.cc3.springjavafx.JavaFxApplication;
import com.cc3.springjavafx.controller.BaseController;

/**
 * 示例应用：一个使用方工程需要写的全部代码，就是本类 + {@link DemoConfig}。
 */
public class DemoApplication extends JavaFxApplication {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * 追加使用方自己的配置（负责扫描 {@code com.cc3.demo} 包）
     */
    @Override
    protected Class<?>[] configs() {
        return new Class<?>[]{DemoConfig.class};
    }

    /**
     * 替换主窗口控制器（示例扩展点①，不覆写则使用框架内置的 DefaultMainController）
     */
    @Override
    protected Class<? extends BaseController> mainControllerClass() {
        return DemoMainController.class;
    }
}
