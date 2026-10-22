package com.cc3.springjavafx.util;

import com.cc3.springjavafx.constant.AppConstant;
import com.cc3.springjavafx.controller.BaseController;
import com.cc3.springjavafx.controller.DefaultStageControllerFactory;
import com.cc3.springjavafx.controller.StageControllerFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文工具
 * @author A-walking-star
 */
public class ContextUtils {
    private static final Logger logger = LoggerFactory.getLogger(ContextUtils.class);

    private static final Set<String> keys = ConcurrentHashMap.newKeySet();

    /**
     * 获取主窗口控制器
     * @return 主窗口控制器，主窗口未创建或已销毁时返回 null
     */
    public static BaseController getMainController() {
        try {
            return (BaseController) SpringUtils.getBean(AppConstant.MAIN_CONTROLLER);
        } catch (RuntimeException e) {
            // 容器未就绪（NPE）或 bean 不存在（BeansException）都按"没有主窗口"处理
            return null;
        }
    }

    /**
     * 获取主stage
     */
    public static Stage getPrimaryStage() {
        return (Stage) SpringUtils.getBean(AppConstant.PRIMARY_STAGE);
    }

    /**
     * 通过controllerKey获取对应的stage
     * @param controllerKey controller唯一key
     * @return Stage
     */
    public static Stage getStage(String controllerKey) {
        BaseController controller = getController(controllerKey);
        return controller != null ? controller.getStage() : null;
    }

    /**
     * 获取所有stage
     * @return stage集合（不含已销毁窗口的 null，避免调用方遍历时 NPE）
     */
    public static Set<Stage> getAllStage() {
        Set<Stage> stages = new HashSet<>();
        for (BaseController controller : getAllController()) {
            Stage stage = controller.getStage();
            if (stage != null) {
                stages.add(stage);
            }
        }
        return stages;
    }

    /**
     * 通过controllerKey获取对应的BaseController
     * @param controllerKey controller唯一key
     * @return BaseController
     */
    public static BaseController getController(String controllerKey) {
        if (controllerKey == null) {
            return getMainController();
        }
        try {
            return ((BaseController) SpringUtils.getBean(AppConstant.CONTROLLER_KEY_PREFIX + controllerKey));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 获取所有controller
     * @return controller集合
     */
    public static Set<BaseController> getAllController() {
        Set<BaseController> controllers = new HashSet<>();
        BaseController mainController = getMainController();
        if (mainController != null) {
            controllers.add(mainController);
        }
        try {
            controllers.addAll(SpringUtils.getBeansOfType(BaseController.class).values());
        } catch (RuntimeException ignored) {
            // 容器未就绪或已关闭：只返回已知的主窗口
        }
        return controllers;
    }

    /**
     * 获取所有controllerKey
     * @return controllerKey列表
     */
    public static Set<String> getAllControllerKey() {
        return new HashSet<>(keys);
    }

    /**
     * 判断是否存在controllerKey
     *
     * <p>约定与 {@link #getController(String)} 保持一致：{@code null} 表示主窗口，
     * 因此 {@code null} 时返回"主窗口是否已创建"；空串或未知 key 一律返回 false。</p>
     *
     * @param controllerKey controller唯一key
     * @return 是否存在
     */
    public static boolean hasControllerKey(String controllerKey) {
        if (controllerKey == null) {
            return getMainController() != null;
        }
        return keys.contains(controllerKey);
    }

    /**
     * 通过新的html创建窗口，指定父窗口
     * @param htmlPath html位置
     * @param width 窗口宽度
     * @param height 窗口高度
     * @param parentControllerKey 父窗口ID（null=主窗口）
     * @return stage唯一id
     */
    public static String createScene(String htmlPath, double width, double height, String parentControllerKey) throws FileNotFoundException {
        return createScene(htmlPath, width, height, parentControllerKey, Modality.NONE);
    }

    /**
     * 新建模态窗口（WINDOW_MODAL，指定父窗口）
     * @param htmlPath html位置
     * @param width 窗口宽度
     * @param height 窗口高度
     * @param parentControllerKey 父窗口ID
     * @return stage唯一id
     */
    public static String createWindowScene(String htmlPath, double width, double height, String parentControllerKey) throws FileNotFoundException {
        return createScene(htmlPath, width, height, parentControllerKey, Modality.WINDOW_MODAL);
    }

    /**
     * 新建应用级模态窗口（APPLICATION_MODAL，指定父窗口）
     * @param htmlPath 路由
     * @param width 窗口宽度
     * @param height 窗口高度
     * @param parentControllerKey 父窗口ID
     * @return stage唯一id
     */
    public static String createApplicationScene(String htmlPath, double width, double height, String parentControllerKey) throws FileNotFoundException {
        return createScene(htmlPath, width, height, parentControllerKey, Modality.APPLICATION_MODAL);
    }

    /**
     * 创建窗口（核心方法）
     * @param htmlPath 路由
     * @param width 窗口宽度
     * @param height 窗口高度
     * @param parentControllerKey 父窗口ID（null=主窗口，其他值必须是已存在的窗口 key）
     * @param modality 窗口模态类型
     * @return stage唯一id
     */
    public static String createScene(String htmlPath, double width, double height, String parentControllerKey, Modality modality) throws FileNotFoundException {
        if (htmlPath == null || htmlPath.trim().isEmpty()) {
            throw new IllegalArgumentException("htmlPath 不能为空");
        }
        // 尺寸 <= 0 / NaN 一律按"未指定"处理，回落到 app.defaultWidth / app.defaultHeight
        double w = normalizeSize(width, true);
        double h = normalizeSize(height, false);

        String key = UUID.randomUUID().toString();

        Stage stage = new Stage();

        BaseController parentController = getController(parentControllerKey);
        warnIfParentMissing(parentControllerKey, parentController, htmlPath);
        if (parentController != null && parentController.getStage() != null) {
            stage.initOwner(parentController.getStage());
        }
        stage.initModality(modality);

        String beanName = AppConstant.CONTROLLER_KEY_PREFIX + key;
        try {
            GenericBeanDefinition beanDefinition = resolveStageControllerFactory()
                    .create(key, stage, parentController, htmlPath, w, h);
            if (beanDefinition == null) {
                throw new IllegalStateException("StageControllerFactory 未返回 BeanDefinition，无法创建窗口：" + htmlPath);
            }
            SpringUtils.registerBeanDefinition(beanName, beanDefinition);
            SpringUtils.getBean(beanName);
            keys.add(key);
        } catch (RuntimeException e) {
            // 失败时别留下"半成品"：bean、定义、Stage 都要回收，否则会泄漏一个永不显示的窗口
            try {
                SpringUtils.closeBean(beanName);
            } catch (Throwable closeError) {
                logger.warn("创建窗口失败后回收 bean[{}] 再次失败：{}", beanName, closeError.toString());
            }
            try {
                stage.close();
            } catch (Throwable ignored) {
            }
            throw e;
        }
        return key;
    }

    /**
     * 窗口尺寸兜底：调用方传 0/负数/NaN（JS 传 null 时也会走到这里）时使用 app.defaultWidth / defaultHeight
     *
     * @param value 调用方传入的尺寸
     * @param width true=宽度，false=高度
     * @return 有效尺寸
     */
    private static double normalizeSize(double value, boolean width) {
        if (value > 0 && !Double.isNaN(value) && !Double.isInfinite(value)) {
            return value;
        }
        try {
            com.cc3.springjavafx.config.AppConfig config =
                    SpringUtils.getBean(com.cc3.springjavafx.config.AppConfig.class);
            Double configured = width ? config.getDefaultWidth() : config.getDefaultHeight();
            if (configured != null && configured > 0) {
                return configured;
            }
        } catch (Throwable ignored) {
            // 容器里没有 AppConfig（例如单独使用 MainWindowBootstrap 的场景），用固定兜底值
        }
        return width ? 800 : 600;
    }

    /**
     * 关闭从controller
     *
     * @param controllerKey controller唯一key
     */
    public static boolean closeController(String controllerKey) {
        return closeController(getController(controllerKey));
    }
    /**
     * 关闭从controller
     *
     * @param controller controller唯一key
     */
    public static boolean closeController(BaseController controller) {
        if (controller == null) {
            return false;
        }
        String key = controller.getKey();
        try {
            SpringUtils.closeBean(controller);
        } finally {
            if (key != null) {
                keys.remove(key);
            }
        }
        return true;
    }

    /**
     * 父窗口不存在时给出明确告警。
     *
     * <p>这两类调用都会让窗口"孤儿化"：窗口能打开，但不会出现在任何窗口的
     * {@code getChildKeys()} 里，也不会随父窗口关闭 —— 症状是"子窗口明明开着，
     * 子窗口列表却是空数组"，非常难自查。故在此显式提示。</p>
     *
     * @param parentControllerKey 调用方传入的父窗口 key
     * @param parentController    解析结果
     * @param htmlPath            页面地址
     */
    private static void warnIfParentMissing(String parentControllerKey, BaseController parentController, String htmlPath) {
        if (parentControllerKey == null || parentController != null) {
            return;
        }
        if (parentControllerKey.trim().isEmpty()) {
            logger.warn("createScene：parentControllerKey 为空串，该窗口不会挂到任何父窗口（没有父窗口时不会出现在 getChildKeys 里）。"
                    + "主窗口必须传 null，不要传 \"\"。html={}", htmlPath);
        } else {
            logger.warn("createScene：找不到 controllerKey='{}' 对应的窗口，该窗口不会挂到任何父窗口。html={}",
                    parentControllerKey, htmlPath);
        }
    }

    /**
     * 解析子窗口控制器工厂。
     *
     * <p>优先使用自定义实现；容器中只有内置实现时用内置实现；容器完全没有该 Bean 时兜底 new 一个，
     * 保证框架在极端情况下仍可开窗。</p>
     *
     * @return 工厂
     */
    private static StageControllerFactory resolveStageControllerFactory() {
        Map<String, StageControllerFactory> factories = SpringUtils.getBeansOfType(StageControllerFactory.class);
        StageControllerFactory builtin = null;
        for (StageControllerFactory factory : factories.values()) {
            if (!(factory instanceof DefaultStageControllerFactory)) {
                return factory;
            }
            builtin = factory;
        }
        return builtin != null ? builtin : new DefaultStageControllerFactory();
    }

    private ContextUtils(){}
}