package com.cc3.demo;

import com.cc3.springjavafx.controller.DefaultMainController;
import javafx.stage.Stage;

/**
 * 自定义主窗口控制器：演示扩展点①，只覆写需要改变的行为。
 */
public class DemoMainController extends DefaultMainController {

    public DemoMainController(Stage stage) {
        super(stage);
    }

    /**
     * 页面加载完成后追加标题后缀，用于证明框架确实使用了自定义控制器
     */
    @Override
    public void loadConfig() {
        setTitle(getTitle() + " · demo");
        getLogger().info("自定义主窗口控制器已生效，窗口标题：{}", getTitle());
    }
}
