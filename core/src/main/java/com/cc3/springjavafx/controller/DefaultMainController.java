package com.cc3.springjavafx.controller;

import com.cc3.springjavafx.config.AppConfig;
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
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 框架内置的主窗口控制器。
 *
 * <p>使用方可以直接使用（不写任何代码），也可以继承本类做少量定制：</p>
 *
 * <pre><code>
 * public class MyMainController extends DefaultMainController {
 *     public MyMainController(Stage stage) { super(stage); }
 *
 *     &#64;Override
 *     public void loadConfig() {
 *         Platform.runLater(() -&gt; getStage().setMaximized(true));
 *     }
 * }
 * </code></pre>
 *
 * <p>然后在 {@code JavaFxApplication#mainControllerClass()} 中返回 {@code MyMainController.class}。
 * 自定义控制器必须保留 {@code (Stage)} 构造参数。</p>
 */
public class DefaultMainController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMainController.class);

    private final Stage stage;
    private WebView webView;
    private WebEngine webEngine;
    private BorderPane borderPane;

    private final List<BaseController> childControllers = new ArrayList<>();
    private String resourceUrl;

    private AppConfig appConfig;

    public DefaultMainController(Stage stage) {
        this.stage = stage;
    }

    /**
     * 注入配置（用 setter 注入，子类自定义构造参数时无需关心）
     * @param appConfig 配置
     */
    @Autowired
    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @PostConstruct
    public void init() throws FileNotFoundException {
        transitionTo(ControllerStatusEnum.OPENING);
        try {
            this.stage.setOnCloseRequest(event -> {
                ContextUtils.closeController(this);
                Platform.exit();
                System.exit(exitCode());
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

            this.webEngine = initWebEngine(webView.getEngine(), resourceUrl);
            transitionTo(ControllerStatusEnum.OPENED);
        } catch (RuntimeException e) {
            transitionTo(ControllerStatusEnum.FAILED);
            throw e;
        } catch (FileNotFoundException e) {
            transitionTo(ControllerStatusEnum.FAILED);
            throw e;
        }
    }

    /**
     * 进程退出码：取 {@code app.exitCode}，未注入配置时回退为 0
     * @return 退出码
     */
    protected int exitCode() {
        Integer configured = appConfig != null ? appConfig.getExitCode() : null;
        return configured != null ? configured : 0;
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
    public String getKey() {
        return null;
    }

    @Override
    public BaseController getParentController() {
        return null;
    }

    @Override
    public BorderPane getRoot() {
        return borderPane;
    }

    @Override
    public Stage getStage() {
        return stage;
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
    public DefaultMainController setResourceUrl(String url) {
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
}