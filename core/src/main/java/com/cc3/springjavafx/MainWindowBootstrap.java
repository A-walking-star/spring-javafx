package com.cc3.springjavafx;

import com.cc3.springjavafx.config.AppConfig;
import com.cc3.springjavafx.constant.AppConstant;
import com.cc3.springjavafx.controller.BaseController;
import com.cc3.springjavafx.enums.ControllerStatusEnum;
import com.cc3.springjavafx.util.SpringUtils;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.ClassUtils;

/**
 * 主窗口装配：把主窗口控制器注册为 Spring Bean，由容器管理其完整生命周期
 * （{@code @PostConstruct} 打开窗口、销毁时关闭窗口）。
 *
 * <p>单独拆出来是为了让「容器已就绪、但还没创建主窗口」这一阶段可被复用，
 * 例如使用方想自己创建主窗口时可以不经 {@link JavaFxApplication}。</p>
 */
public final class MainWindowBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowBootstrap.class);

    /** JSR-250 注解：Spring 用它判断是否启用 {@code @PostConstruct} / {@code @PreDestroy} */
    private static final String JSR250_ANNOTATION = "javax.annotation.PostConstruct";

    private MainWindowBootstrap() {
    }

    /**
     * 按主窗口配置创建并初始化主窗口。
     *
     * <p>主窗口控制器的构造契约：必须有且仅有一个 {@code (Stage)} 构造参数。
     * 如需其他依赖，请使用字段/方法注入（{@code @Autowired}）而非构造参数。</p>
     *
     * @param primaryStage    JavaFX 主 Stage
     * @param controllerClass 主窗口控制器类型
     * @return 主窗口控制器实例
     */
    public static BaseController start(Stage primaryStage, Class<? extends BaseController> controllerClass) {
        if (primaryStage == null) {
            throw new IllegalArgumentException("primaryStage 不能为空");
        }
        if (controllerClass == null || !BaseController.class.isAssignableFrom(controllerClass)) {
            throw new IllegalArgumentException("主窗口控制器必须是 BaseController 的子类：" + controllerClass);
        }
        checkJsr250Present();

        AppConfig appConfig = SpringUtils.getBean(AppConfig.class);
        applyWindowDefaults(primaryStage, appConfig);

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(controllerClass);
        beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(primaryStage);
        beanDefinition.getPropertyValues().add("resourceUrl", appConfig.getResourceUrl());

        SpringUtils.registerBeanDefinition(AppConstant.MAIN_CONTROLLER, beanDefinition);
        BaseController controller = (BaseController) SpringUtils.getBean(AppConstant.MAIN_CONTROLLER);
        warnIfNotInitialized(controller);
        return controller;
    }

    /**
     * 启动前置校验：JDK 11+ 里 {@code javax.annotation} 已从 JDK 移除，若运行时 classpath 上也没有
     * JSR-250 注解 API，Spring 会**静默**不注册 {@code CommonAnnotationBeanPostProcessor}，
     * 于是所有 {@code @PostConstruct} 都不执行 —— 表现为"启动日志打完就不动了、窗口永远不出现、
     * 也没有任何异常"，非常难排查（IDEA 里用 JDK 8 调试却正常，因为 JDK 8 自带
     * {@code javax.annotation}）。这里直接失败并给出可执行的修复方式。
     */
    private static void checkJsr250Present() {
        if (ClassUtils.isPresent(JSR250_ANNOTATION, MainWindowBootstrap.class.getClassLoader())) {
            return;
        }
        throw new IllegalStateException(
                "运行时缺少 JSR-250 注解 API（javax.annotation.PostConstruct），"
                        + "Spring 会静默跳过所有 @PostConstruct / @PreDestroy 回调：\n"
                        + "  主窗口控制器的 init() 不会执行 → 窗口不显示 → 进程一直挂着且没有任何异常日志。\n"
                        + "  JDK 8：javax.annotation 在 JDK 内，无需处理；\n"
                        + "  JDK 11+：javax.annotation 已从 JDK 移除，必须在运行时 classpath 上提供注解 API：\n"
                        + "    <dependency>\n"
                        + "        <groupId>jakarta.annotation</groupId>\n"
                        + "        <artifactId>jakarta.annotation-api</artifactId>\n"
                        + "        <version>1.3.5</version>\n"
                        + "    </dependency>\n"
                        + "  注意版本：1.3.x 提供的是 javax.annotation 包（适配 Spring 5 / JDK 11+）；\n"
                        + "  2.x 已改名 jakarta.annotation，Spring 5 的 @PostConstruct 不会生效。\n"
                        + "  框架 core 已声明该依赖，若仍报此错，请检查打包时是否被排除（mvn dependency:tree）。");
    }

    /**
     * 兜底告警：bean 拿到了、窗口却没显示，说明初始化回调没跑（典型原因见 {@link #checkJsr250Present()}）
     */
    private static void warnIfNotInitialized(BaseController controller) {
        if (controller.getStatus() != ControllerStatusEnum.INSTANCE) {
            return;
        }
        Stage stage = controller.getStage();
        if (stage != null && stage.isShowing()) {
            return;
        }
        logger.warn("主窗口控制器 {} 已创建，但窗口没有显示：初始化回调（@PostConstruct）可能没有执行。"
                        + "请确认运行时 classpath 上有 javax.annotation.PostConstruct（JDK 11+ 需要 jakarta.annotation-api:1.3.x）",
                controller.getClass().getName());
    }

    /**
     * 应用 app.* 中的窗口默认值（尺寸未配置时已在 AppConfig 内按屏幕计算）
     */
    private static void applyWindowDefaults(Stage stage, AppConfig appConfig) {
        String title = appConfig.getAppName();
        if (title != null && !title.isEmpty()) {
            stage.setTitle(title);
        }
        if (appConfig.getDefaultWidth() != null) {
            stage.setWidth(appConfig.getDefaultWidth());
        }
        if (appConfig.getDefaultHeight() != null) {
            stage.setHeight(appConfig.getDefaultHeight());
        }
        if (appConfig.getDefaultMinWidth() != null) {
            stage.setMinWidth(appConfig.getDefaultMinWidth());
        }
        if (appConfig.getDefaultMinHeight() != null) {
            stage.setMinHeight(appConfig.getDefaultMinHeight());
        }
    }
}