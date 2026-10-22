package com.cc3.springjavafx.controller;

import com.cc3.springjavafx.config.AppConfig;
import com.cc3.springjavafx.constant.AppConstant;
import com.cc3.springjavafx.enums.ControllerStatusEnum;
import com.cc3.springjavafx.util.ContextUtils;
import com.cc3.springjavafx.util.SpringUtils;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 通用新窗口
 * @author A-walking-star
 */
public class NewStageWebController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(NewStageWebController.class);

    /* 组件 */
    private BorderPane borderPane;

    private WebView webView;
    private WebEngine webEngine;
    private final Stage stage;

    /* 窗口信息 */
    private final String key;
    private final BaseController parentController;
    private final List<BaseController> childControllers = new java.util.ArrayList<>();
    private String resourceUrl;

    public NewStageWebController(String key, Stage stage, BaseController parentController) {
        this.key = key;
        this.stage = stage;
        this.parentController = parentController;
    }

    @PostConstruct
    public void init() throws FileNotFoundException {
        transitionTo(ControllerStatusEnum.OPENING);
        try {
            this.stage.setOnCloseRequest(event -> {
                ContextUtils.closeController(this);
                logger.info(resourceUrl + " 窗口关闭！窗口Id: {}", this.key);
            });
            this.webView = new WebView();

            this.borderPane = initUI(this.webView);

            show();

            // 设置窗口图标
            try {
                initIcon(SpringUtils.getBean(AppConfig.class).getWindowIcon());
            } catch (Throwable e) {
                logger.error("加载窗口图标失败！", e);
            }

            this.webEngine = initWebEngine(this.webView.getEngine(), this.resourceUrl);
            transitionTo(ControllerStatusEnum.OPENED);
            // 初始化成功后才登记到父窗口：失败窗口不应出现在父窗口的 getChildKeys() 里
            if (this.parentController != null) {
                this.parentController.registerChild(this);
            }
        } catch (RuntimeException e) {
            transitionTo(ControllerStatusEnum.FAILED);
            throw e;
        } catch (FileNotFoundException e) {
            transitionTo(ControllerStatusEnum.FAILED);
            throw e;
        }
    }

    @Override
    public void initJavaScript() {
        Platform.runLater(() -> {
            getWindowObject().setMember(AppConstant.WEB_GLOBAL_CONTROLLER_ID_NAME, this.key);
            if (this.parentController != null) {
                getWindowObject().setMember(AppConstant.WEB_GLOBAL_PARENT_CONTROLLER_ID_NAME, this.parentController.getKey());
            }
        });
    }

    @Override
    public WebView getWebView() {
        return webView;
    }

    @Override
    public WebEngine getWebEngine() {
        return webEngine;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public String getResourceUrl() {
        return resourceUrl;
    }

    @Override
    public NewStageWebController setResourceUrl(String url) {
        resourceUrl = url;
        return this;
    }

    @Override
    public void registerChild(BaseController childController) {
        if (childController == null) {
            return;
        }
        childControllers.add(childController);
    }

    @Override
    public void unregisterChild(BaseController childController) {
        childControllers.remove(childController);
    }

    @Override
    public Set<BaseController> getAllChildController() {
        return new HashSet<>(childControllers);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public BaseController getParentController() {
        return parentController;
    }

    @Override
    public BorderPane getRoot() {
        return borderPane;
    }

    @Override
    public Stage getStage() {
        return stage;
    }
}