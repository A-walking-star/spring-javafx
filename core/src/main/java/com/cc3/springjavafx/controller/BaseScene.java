package com.cc3.springjavafx.controller;

import com.cc3.springjavafx.util.StringUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public interface BaseScene {

    /**
     * 获取根容器
     * @return BorderPane
     */
    BorderPane getRoot();

    /**
     * 获取stage
     * @return Stage
     */
    Stage getStage();

    /**
     * 获取WebView
     * @return WebView
     */
    WebView getWebView();

    /**
     * 获取窗口宽度
     * @return 宽度
     */
    Double getWidth();

    /**
     * 获取窗口高度
     * @return 高度
     */
    Double getHeight();

    /**
     * 初始化UI界面
     * @param indexView
     * @return BorderPane
     */
    default BorderPane initUI(WebView indexView) {
        BorderPane rootContainer = new BorderPane();

        // 中间内容区
        StackPane contentArea = new StackPane();

        // 默认显示拉取页面
        contentArea.getChildren().add(indexView);

        rootContainer.setCenter(contentArea);

        return rootContainer;
    }

    /**
     * 初始化图标
     * @param iconPath 图标路径
     */
    default void initIcon(String iconPath) {
        // 设置窗口图标
        if (StringUtils.isNotBlank(iconPath)) {
            getStage().getIcons().add(new Image("file:///" + iconPath));
        } else {
            // 框架内置图标放在 com/cc3/springjavafx/logo.png，
            // 不占用使用方的 classpath 根路径，避免与使用方同名资源冲突
            java.io.InputStream logoStream = BaseScene.class.getResourceAsStream("/com/cc3/springjavafx/logo.png");
            if (logoStream != null) {
                getStage().getIcons().add(new Image(logoStream));
            }
        }
    }

    /**
     * 显示窗口
     */
    default void show() {
        Stage stage = getStage();
        if (stage == null) {
            throw new IllegalArgumentException("请先初始化stage");
        }
        stage.setScene(new Scene(getRoot(), getWidth(), getHeight()));
        stage.show();
    }
}