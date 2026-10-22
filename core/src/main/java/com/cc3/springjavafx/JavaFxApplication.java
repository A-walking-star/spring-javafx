package com.cc3.springjavafx;

import com.cc3.springjavafx.config.SpringJavafxConfig;
import com.cc3.springjavafx.constant.AppConstant;
import com.cc3.springjavafx.controller.BaseController;
import com.cc3.springjavafx.controller.DefaultMainController;
import com.cc3.springjavafx.util.SpringUtils;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 框架的启动基类：使用方只需继承本类，并在自己的 {@code main(String[])} 里调用 {@code launch(args)}。
 *
 * <pre>{@code
 * public class MyApp extends JavaFxApplication {
 *     public static void main(String[] args) { launch(args); }
 * }
 * }</pre>
 *
 * <p>可选覆写点：</p>
 * <ul>
 *     <li>{@link #configs()}：追加使用方自己的 {@code @Configuration}（通常带 {@code @ComponentScan}）；</li>
 *     <li>{@link #mainControllerClass()}：替换主窗口控制器；</li>
 *     <li>{@link #onContextReady(ConfigurableApplicationContext)}：容器就绪后的初始化钩子。</li>
 * </ul>
 *
 * <p><b>注意</b>：容器必须在 JavaFX 应用线程内创建（{@code start} 方法内）。
 * 因为 {@code AppConfig} 初始化时会访问 {@code Screen}，而 {@code Screen} 的静态初始化被限制在 FX 线程。</p>
 */
public abstract class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    /**
     * 使用方追加的配置类，默认无。
     * @return 配置类数组
     */
    protected Class<?>[] configs() {
        return new Class<?>[0];
    }

    /**
     * 主窗口控制器类型，默认使用框架内置实现。
     * @return 控制器类型
     */
    protected Class<? extends BaseController> mainControllerClass() {
        return DefaultMainController.class;
    }

    /**
     * 容器刷新完成、主窗口已创建后的回调，供使用方做二次初始化。
     * @param context 容器
     */
    protected void onContextReady(ConfigurableApplicationContext context) {
        // 默认无操作
    }

    @Override
    public final void start(Stage primaryStage) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SpringJavafxConfig.class);
        Class<?>[] extras = configs();
        if (extras != null && extras.length > 0) {
            ctx.register(extras);
        }
        ctx.refresh();
        this.context = ctx;

        SpringUtils.registerSingleton(AppConstant.PRIMARY_STAGE, primaryStage);
        MainWindowBootstrap.start(primaryStage, mainControllerClass());

        onContextReady(ctx);
    }

    @Override
    public void stop() throws Exception {
        if (context != null) {
            try {
                context.close();
            } finally {
                context = null;
                // 断开静态引用，避免容器（及其中所有 WebView/Stage）泄漏
                SpringUtils.clear();
            }
        }
    }

    /**
     * 获取内部 Spring 容器。框架内部实现细节，仅在有特殊集成需求时使用。
     * @return 容器
     */
    public final ConfigurableApplicationContext getContext() {
        return context;
    }
}