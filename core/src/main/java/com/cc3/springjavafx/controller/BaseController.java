package com.cc3.springjavafx.controller;

import com.cc3.springjavafx.api.JavaApiBridge;
import com.cc3.springjavafx.constant.AppConstant;
import com.cc3.springjavafx.util.ContextUtils;
import com.cc3.springjavafx.web.dialog.WebDialogManager;
import com.cc3.springjavafx.enums.ControllerStatusEnum;
import com.cc3.springjavafx.util.ParamsUtils;
import com.cc3.springjavafx.util.SpringUtils;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseController implements BaseScene, DisposableBean {
    public static final String topParamName = "window";

    private final AtomicInteger apiReadyCount = new AtomicInteger(0);
    private final AtomicInteger totalApiReadyCount = new AtomicInteger(0);

    /** 全局未捕获异常处理器只需安装一次（避免每个窗口覆盖、并持有已关闭窗口的引用） */
    private static final AtomicBoolean UNCAUGHT_HANDLER_INSTALLED = new AtomicBoolean(false);

    private static final Logger UNCAUGHT_LOGGER =
            LoggerFactory.getLogger("com.cc3.springjavafx.UncaughtException");

    /** 当前窗口持有的 JavaApiBridge 实例（每个窗口独立一套，窗口关闭时销毁） */
    private final List<JavaApiBridge> apiBridges = new CopyOnWriteArrayList<>();

    /** 防止 close() 重入 */
    private volatile boolean closing = false;

    /** 生命周期状态：构造完成即为 INSTANCE，永不返回 null */
    private volatile ControllerStatusEnum controllerStatus = ControllerStatusEnum.INSTANCE;

    /**
     * 初始化JavaScript接口
     */
    public void initJavaScript() {};

    /**
     * 加载配置
     */
    public void loadConfig() {};

    /**
     * 通知父窗口：子窗口已就绪（在baseApiReady后由父类触发）
     */
    public void notifyParentReady() {
        BaseController parentController = getParentController();
        if (parentController != null) {
            try {
                ParamsUtils.onCallBack(parentController, AppConstant.WEB_CHILD_READY_CALLBACK, getKey());
                getLogger().info("通知父窗口 [{}] 子窗口 [{}] 已就绪", parentController.getKey(), getKey());
            } catch (Throwable e) {
                getLogger().error("子窗口就绪通知父窗口失败: parentKey={}, childKey={}", parentController.getKey(), getKey(), e);
            }
        }
    };

    /**
     * 通知父窗口：子窗口已关闭
     */
    public void notifyParentClosed() {
        BaseController parentController = getParentController();
        if (parentController != null) {
            parentController.unregisterChild(this);
            try {
                ParamsUtils.onCallBack(parentController, AppConstant.WEB_CHILD_CLOSED_CALLBACK, getKey());
                getLogger().info("通知父窗口 [{}] 子窗口 [{}] 已关闭", parentController.getKey(), getKey());
            } catch (Throwable e) {
                getLogger().error("子窗口关闭通知父窗口失败: parentKey={}, childKey={}", parentController.getKey(), getKey(), e);
            }
        }
    };

    /**
     * 获取窗口key
     * @return 窗口key
     */
    public abstract String getKey();

    /**
     * 获取父窗口
     * @return 父窗口，null表示无父窗口
     */
    public abstract BaseController getParentController();

    /**
     * 获取Web引擎
     * @return WebEngine
     */
    public abstract WebEngine getWebEngine();

    /**
     * 获取logger
     * @return Logger
     */
    public abstract Logger getLogger();

    /**
     * 获取控制器生命周期状态
     * @return 状态，永不为 null
     */
    public ControllerStatusEnum getStatus() {
        return controllerStatus;
    }

    /**
     * 是否已就绪（窗口已显示、页面已加载）
     * @return 是否已就绪
     */
    public boolean isOpened() {
        return controllerStatus.isOpened();
    }

    /**
     * 是否已结束（关闭 / 初始化失败 / 已销毁）
     * @return 是否已结束
     */
    public boolean isEnded() {
        return controllerStatus.isTerminal();
    }

    /**
     * 生命周期状态流转。
     *
     * <p>非法流转会被拒绝并告警（保持状态机可信）：{@link ControllerStatusEnum#DESTROYED}
     * 是终态，其余状态只能按 {@link ControllerStatusEnum#canTransitionTo} 的允许集合流转。</p>
     *
     * @param next 目标状态
     * @return 是否完成流转
     */
    protected boolean transitionTo(ControllerStatusEnum next) {
        ControllerStatusEnum current = controllerStatus;
        if (next == null) {
            getLogger().warn("窗口 [{}] 状态流转被忽略：目标状态为 null，当前状态 {}", windowKey(), current);
            return false;
        }
        if (current == next) {
            return true;
        }
        if (!current.canTransitionTo(next)) {
            getLogger().warn("窗口 [{}] 非法的状态流转被拒绝：{} -> {}，请检查调用链", windowKey(), current, next);
            return false;
        }
        controllerStatus = next;
        getLogger().debug("窗口 [{}] 状态：{} -> {}（{}）", windowKey(), current, next, next.getDescription());
        return true;
    }

    /**
     * 日志用的窗口标识（主窗口 key 为 null）
     * @return 窗口标识
     */
    private String windowKey() {
        String key = getKey();
        return key == null ? "main" : key;
    }

    /**
     * 获取页面url
     * @return 页面url
     */
    public abstract String getResourceUrl();

    /**
     * 设置页面url
     * @param url
     * @return BaseController
     */
    public abstract BaseController setResourceUrl(String url);

    /**
     * 获取前端窗口对象
     * @return JSObject
     */
    public JSObject getWindowObject() {
        return (JSObject) getWebEngine().executeScript(topParamName);
    }

    /**
     * 获取窗口标题
     * @return 窗口标题
     */
    public String getTitle() {
        Stage stage = getStage();
        if (stage != null) {
            return stage.getTitle();
        }
        return null;
    }

    /**
     * 设置窗口标题
     * @param title 窗口标题
     */
    public void setTitle(String title) {
        Stage stage = getStage();
        if (stage != null) {
            stage.setTitle(title);
        }
    }

    /**
     * 获取窗口宽度
     * @return 窗口宽度
     */
    public Double getWidth() {
        Stage stage = getStage();
        if (stage != null) {
            return stage.getWidth();
        }
        return null;
    }

    /**
     * 设置宽度
     * @param width 宽度
     */
    public void setWidth(double width) {
        Stage stage = getStage();
        if (stage != null) {
            stage.setWidth(width);
        }
    }

    /**
     * 获取窗口高度
     * @return 窗口高度
     */
    public Double getHeight() {
        Stage stage = getStage();
        if (stage != null) {
            return stage.getHeight();
        }
        return null;
    }

    /**
     * 设置高度
     * @param height 高度
     */
    public void setHeight(double height) {
        Stage stage = getStage();
        if (stage != null) {
            stage.setHeight(height);
        }
    }

    /**
     * 获取窗口最小宽度
     * @return 窗口最小宽度
     */
    public Double getMinWidth() {
        Stage stage = getStage();
        if (stage != null) {
            return stage.getMinWidth();
        }
        return null;
    }

    /**
     * 设置最小宽度
     * @param width 宽度
     */
    public void setMinWidth(double width) {
        Stage stage = getStage();
        if (stage != null) {
            stage.setMinWidth(width);
        }
    }

    /**
     * 获取窗口最小高度
     * @return 窗口最小高度
     */
    public Double getMinHeight() {
        Stage stage = getStage();
        if (stage != null) {
            return stage.getMinHeight();
        }
        return null;
    }

    /**
     * 设置最小高度
     * @param height 高度
     */
    public void setMinHeight(double height) {
        Stage stage = getStage();
        if (stage != null) {
            stage.setMinHeight(height);
        }
    }

    /**
     * 注册子窗口到父窗口
     * @param childController 子窗口控制器
     */
    public abstract void registerChild(BaseController childController);

    /**
     * 注销子窗口（并清理父子双向映射）
     * @param childController 子窗口控制器
     */
    public abstract void unregisterChild(BaseController childController);

    /**
     * 查询所有子窗口控制器
     * @return 子窗口控制器集合
     */
    public abstract Set<BaseController> getAllChildController();

    /**
     * 查询父窗口ID
     * @return 父窗口ID，null 表示无父窗口
     */
    public String getParentKey() {
        BaseController parentController = getParentController();
        return parentController != null ? parentController.getKey() : null;
    }

    /**
     * 关闭当前窗口
     */
    @PreDestroy
    public void close() {
        if (closing) {
            return;
        }
        getLogger().debug("close window: {}", getKey());
        closing = true;
        transitionTo(ControllerStatusEnum.CLOSING);
        try {
            // 递归关闭所有子窗口
            for (BaseController child : new ArrayList<>(getAllChildController())) {
                try{
                    ContextUtils.closeController(child);
                } catch (Throwable ignored) {}
            }
            Stage stage = getStage();
            if (stage != null) {
                stage.close();
            }
            try {
                // 通知父窗口：子窗口关闭
                notifyParentClosed();
            } catch (Throwable ignored) {}
        } finally {
            // 销毁当前窗口持有的 JavaApiBridge，避免实例泄漏
            destroyApiBridges();
            // 桥接器销毁完成，窗口生命周期结束（bean 销毁时再流转到 DESTROYED）
            transitionTo(ControllerStatusEnum.CLOSED);
        }
    }

    /**
     * 初始化web引擎
     * @param webEngine WebEngine
     * @param htmlResource html路径
     * @return WebEngine
     */
    public WebEngine initWebEngine(WebEngine webEngine, String htmlResource) throws FileNotFoundException {
        // 加载HTML页面
        // 先加载外部资源，再加载内部资源，最后加载
        String fullPath;
        if (!(htmlResource.startsWith("http://") || htmlResource.startsWith("https://"))) {
            int delimiter = htmlResource.indexOf("#/");
            if (delimiter == -1) {
                delimiter = htmlResource.length();
            }
            String basePath = htmlResource.substring(0, delimiter);
            String fragment = htmlResource.substring(delimiter); // "#/..." 或空串

            // 兼容 Windows：统一替换反斜杠，避免进入 URI 后格式非法
            basePath = basePath.replace('\\', '/');

            // 判断 basePath 是否已经是合法 URI（支持 file:/、file:///、http://、app:// 等）
            // 正则匹配 URI scheme：以字母开头，后跟字母/数字/+/./-，然后跟冒号
            if (basePath.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
                fullPath = basePath + fragment;
            } else {
                // 尝试文件系统路径（相对路径基于 user.dir，即 jar 包所在目录）
                File file = new File(basePath);
                if (!file.isAbsolute()) {
                    file = new File(System.getProperty("user.dir"), basePath).getAbsoluteFile();
                }

                if (file.exists()) {
                    // toURI() 跨平台安全：
                    //   Windows -> file:/C:/Users/...  或 file:///C:/Users/...
                    //   Linux   -> file:///home/user/...
                    fullPath = file.toURI().toASCIIString() + fragment;
                } else {
                    // 尝试 classpath 加载（去掉前导 /，兼容 jar 内资源）
                    String resourcePath = basePath.startsWith("/") ? basePath.substring(1) : basePath;
                    URL resource = getClass().getClassLoader().getResource(resourcePath);
                    if (resource != null) {
                        fullPath = resource.toExternalForm() + fragment;
                    } else {
                        // 不再构造假的 file URI 交给 WebEngine：那样只会得到 "页面异常！File not found"，
                        // 完全指不出是哪个路径找不到。这里直接失败并把两条尝试路径列出来。
                        throw new FileNotFoundException("页面资源未找到：" + basePath
                                + "\n  已尝试文件系统路径：" + file.getAbsolutePath()
                                + "\n  已尝试 classpath 路径：" + resourcePath
                                + "\n  提示：检查 app.resourceUrl 配置，或确认该文件已打包进 resources。");
                    }
                }
            }
        } else {
            fullPath = htmlResource;
        }
        getLogger().info("页面访问路径：{} (id: {})", fullPath, getKey());
        setResourceUrl(fullPath);

        // 标准化 file:/C:/... → file:///C:/...（JavaFX WebEngine 要求三斜杠格式）
        fullPath = fullPath.replaceFirst("^file:/([^/])", "file:///$1");

        // 页面加载完成后设置JavaScript接口
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            getLogger().info("\nobs: {}\noldState: {}\nnewState: {}", obs, oldState, newState);
            if (Worker.State.SCHEDULED.equals(newState)) {
                getLogger().info("页面加载已调度...");
            } else if (Worker.State.RUNNING.equals(newState)) {
                getLogger().info("页面加载中...");
            } else if (Worker.State.SUCCEEDED.equals(newState)) {
                initWeb();
                getLogger().info("页面加载成功！准备初始化...");
            } else if (Worker.State.FAILED.equals(newState)) {
                getLogger().error("页面加载失败: {}", getWebEngine().getLoadWorker().getException().getMessage());
            }
        });

        // 初始化控制台监听事件
        // 设置 JS 接口接收错误（可选，需要 JSObject）
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldExc, newExc) -> {
            if (newExc != null) {
                getLogger().error("页面异常！", newExc);
            }
        });

        // 启用控制台日志（内部 API，见 installConsoleListener 的兼容性说明）
        installConsoleListener();

        // 框架级对话框：alert / confirm / prompt / JS 错误统一交给 WebDialogManager 处理，
        // 外观与交互由 WebDialogHandler 决定（内置实现见 DefaultWebDialogHandler，可配置、可替换）
        WebDialogManager dialogManager = SpringUtils.getBean(WebDialogManager.class);
        // alert：不阻塞页面，直接弹出提示
        webEngine.setOnAlert(event -> dialogManager.alert(this, event.getData()));
        // confirm：同步返回是否确认，保持与浏览器一致的行为
        webEngine.setConfirmHandler(message -> dialogManager.confirm(this, message));
        // prompt：同步返回输入内容，取消返回 null
        webEngine.setPromptHandler(prompt -> dialogManager.prompt(this, prompt.getMessage(), prompt.getDefaultValue()));
        // JS 执行错误：默认记日志，可通过 app.dialog.jsErrorDialog 决定是否弹窗
        webEngine.setOnError(event -> dialogManager.jsError(this, event.getMessage()));

        webEngine.load(fullPath);
        return webEngine;
    }

    /**
     * 把 JavaFX 内部 API {@code com.sun.javafx.webkit.WebConsoleListener} 接到框架日志。
     *
     * <p>该 API 在 JDK 8 中可直接访问；JDK 11+（openjfx）下需要启动参数
     * {@code --add-exports javafx.web/com.sun.javafx.webkit=ALL-UNNAMED}。
     * 框架改为反射调用，缺少该参数时只丢失"前端 console.log 转发到后端日志"，不影响其它功能。</p>
     */
    private void installConsoleListener() {
        try {
            Class<?> listenerType = Class.forName("com.sun.javafx.webkit.WebConsoleListener");
            Object listener = Proxy.newProxyInstance(listenerType.getClassLoader(),
                    new Class<?>[]{listenerType},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            switch (method.getName()) {
                                case "toString":
                                    return "WebConsoleListenerProxy";
                                case "hashCode":
                                    return System.identityHashCode(proxy);
                                case "equals":
                                    return proxy == args[0];
                                default:
                                    return null;
                            }
                        }
                        if ("messageAdded".equals(method.getName()) && args != null && args.length >= 3) {
                            getLogger().info("{} (id: {}) [Console] {} (line: {})",
                                    getResourceUrl(), getKey(), args[1], args[2]);
                        }
                        return null;
                    });
            listenerType.getMethod("setDefaultListener", listenerType).invoke(null, listener);
        } catch (Throwable e) {
            getLogger().debug("浏览器控制台日志不可用（JDK 11+ 需 --add-exports javafx.web/com.sun.javafx.webkit=ALL-UNNAMED）：{}", e.toString());
        }
    }

    /**
     * 初始化web页面
     */
    public void initWeb() {
        installUncaughtExceptionHandler();
        initBaseApi();
        initJavaScript();
        loadConfig();
    }

    /**
     * 安装全局未捕获异常处理器（整个 JVM 只装一次）。
     *
     * <p>以前每个窗口初始化都覆盖一次：最后一个窗口关闭后处理器仍持有它的引用（内存泄漏），
     * 且所有窗口的异常都会被记到"最后创建的那个窗口"的日志里。</p>
     */
    private static void installUncaughtExceptionHandler() {
        if (!UNCAUGHT_HANDLER_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                UNCAUGHT_LOGGER.error("捕捉到未处理的异常（线程：{}）：{}",
                        thread.getName(), throwable.getMessage(), throwable));
    }

    /**
     * 初始化基础Java API
     *
     * <p>每个窗口都会创建一套独立的 JavaApiBridge 实例，并绑定到当前窗口控制器，
     * 因此 {@code window.WindowApi} 等对象操作的永远是加载它的那个窗口。</p>
     */
    public void initBaseApi() {
        // 页面重新加载时先销毁上一轮实例，避免同一窗口重复累积桥接器
        destroyApiBridges();

        Map<String, JavaApiBridge> apiBeans = SpringUtils.createBeansOfType(JavaApiBridge.class);
        String key = getKey();
        for (Map.Entry<String, JavaApiBridge> entry : apiBeans.entrySet()) {
            JavaApiBridge apiBean = entry.getValue();
            try {
                // 绑定到当前窗口：桥接器的所有窗口操作都以本次绑定为准
                apiBean.bindController(this);
                apiBridges.add(apiBean);
            } catch (Throwable e) {
                getLogger().error("JavaApiBridge[{}] 绑定窗口 [{}] 失败！", entry.getKey(), key == null ? "主窗口" : key, e);
                destroyApiBridge(apiBean);
            }
        }

        apiReadyCount.set(0);
        totalApiReadyCount.set(apiBridges.size());
        getLogger().info("窗口 [{}] 初始化 JavaApiBridge 实例 {} 个：{}", key == null ? "主窗口" : key, apiBridges.size(), namespaces());

        if (apiBridges.isEmpty()) {
            // 没有任何桥接器时也要通知前端就绪，避免页面一直等待
            baseApiReady();
            return;
        }
        for (JavaApiBridge apiBean : apiBridges) {
            loadBaseJavaApi(apiBean);
        }
    }

    /**
     * 销毁当前窗口的所有 JavaApiBridge 实例
     *
     * <p>窗口关闭、页面重新加载时调用；重复调用安全。</p>
     */
    public void destroyApiBridges() {
        if (apiBridges.isEmpty()) {
            return;
        }
        for (JavaApiBridge apiBridge : apiBridges) {
            destroyApiBridge(apiBridge);
        }
        apiBridges.clear();
        String key = getKey();
        getLogger().info("窗口 [{}] 的 JavaApiBridge 已全部销毁", key == null ? "主窗口" : key);
    }

    /**
     * 获取当前窗口持有的 JavaApiBridge 实例
     * @return 只读视图
     */
    public List<JavaApiBridge> getApiBridges() {
        return new ArrayList<>(apiBridges);
    }

    public boolean hasApiBridge(JavaApiBridge apiBridge) {
        return apiBridges.contains(apiBridge);
    }

    /**
     * 销毁单个桥接器，销毁异常不影响其他实例
     * @param apiBridge 桥接器
     */
    private void destroyApiBridge(JavaApiBridge apiBridge) {
        if (!hasApiBridge(apiBridge)) {
            return;
        }
        String namespace = apiBridge.getNamespace();
        try {
            SpringUtils.closeBean(apiBridge);
        } catch (Throwable e) {
            getLogger().error("JavaApiBridge[{}] 销毁失败！窗口id：{}", namespace, getKey(), e);
        } finally {
            apiBridges.remove(apiBridge);
        }
    }

    /**
     * 已注入的桥接器命名空间，用于日志排查
     * @return 命名空间列表
     */
    private List<String> namespaces() {
        List<String> names = new ArrayList<>(apiBridges.size());
        for (JavaApiBridge apiBridge : apiBridges) {
            names.add(apiBridge.getNamespace());
        }
        return names;
    }

    public void baseApiReady() {
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(event -> {
            try {
                WebEngine engine = getWebEngine();
                if (engine != null) {
                    engine.executeScript("!!window['" + AppConstant.WEB_READY_CALLBACK + "'] && "
                            + AppConstant.WEB_READY_CALLBACK + "()");
                }
                notifyParentReady();
            } catch (Throwable e) {
                // 窗口在这 1 秒内被关闭 / 页面已跳转：不能把异常抛到 FX 线程
                getLogger().warn("通知前端就绪失败（窗口可能已关闭）：{}", e.toString());
            }
        });
        pause.play();
    }

    /**
     * 加载基础Java API
     * 用于在JavaScript中调用Java方法
     * 例如：window.WindowApi.setTitle("Mail Tool")
     * 会调用setTitle方法
     * @param javaApiBridge Java API桥接器
     */
    public void loadBaseJavaApi(JavaApiBridge javaApiBridge) {
        final String javaApiPrefix = javaApiBridge.getNamespace();
        if (!isValidJsIdentifier(javaApiPrefix)) {
            getLogger().error("JavaApiBridge 的命名空间不是合法的 JS 标识符：'{}'，已跳过注入。"
                    + "getNamespace() 必须返回 [A-Za-z_$][A-Za-z0-9_$]* 形式的字符串", javaApiPrefix);
            markApiReady();
            return;
        }
        // 延迟一帧执行，确保 WebKit JS 上下文完全就绪
        // 这对于单文件 HTML（内联大量 JS）尤其重要
        Platform.runLater(() -> {
            try {
                injectAndFreezeApi(javaApiBridge, javaApiPrefix);
            } catch (Throwable e) {
                getLogger().error("JavaApiBridge[{}] 注入失败：{}", javaApiPrefix, e.toString());
            } finally {
                markApiReady();
            }
        });
    }

    /**
     * 把桥接器注入 JS 上下文并冻结，防止页面覆盖
     *
     * @param javaApiBridge   桥接器
     * @param javaApiPrefix   已校验的命名空间
     */
    private void injectAndFreezeApi(JavaApiBridge javaApiBridge, String javaApiPrefix) {
        Object windowObject = getWebEngine().executeScript(topParamName);
        if (!(windowObject instanceof JSObject)) {
            throw new IllegalStateException("页面 JS 上下文不可用（window 不是 JSObject），无法注入 " + javaApiPrefix);
        }
        JSObject window = (JSObject) windowObject;
        window.setMember(javaApiPrefix, javaApiBridge);

        // 检查是否注入成功
        boolean success = true;
        for (Method method : javaApiBridge.getClass().getMethods()) {
            int modifiers = method.getModifiers();
            // 过滤：public 且 非 static
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)
                    && !Boolean.TRUE.equals(getWebEngine().executeScript(
                            "!!window." + javaApiPrefix + "." + method.getName()))) {
                success = false;
                getLogger().error("{} 方法注入失败", javaApiPrefix + "." + method.getName());
                break;
            }
        }
        getLogger().info("{}{}", javaApiPrefix, success ? " 注入成功！准备冻结API" : " 注入失败！");

        if (success) {
            // 立即冻结，防止 JS 覆盖（可配置属性设为 false）
            getWebEngine().executeScript(
                    "(function() {" +
                    "  var api = window." + javaApiPrefix + ";" +
                    "  if (!api) { console.error('" + javaApiPrefix + "注入失败：对象不存在'); return; }" +
                    "  try {" +
                    "    Object.defineProperty(window, '" + javaApiPrefix + "', {" +
                    "      value: api," +
                    "      writable: false," +
                    "      configurable: false," +
                    "      enumerable: true" +
                    "    });" +
                    "    console.log('" + javaApiPrefix + " API 已冻结，防止覆盖');" +
                    "  } catch(e) {" +
                    "    console.warn('无法锁定 " + javaApiPrefix + " API:', e);" +
                    "  }" +
                    "})();"
            );
        }
    }

    /**
     * 一个桥接器就绪后计数；全部就绪时通知前端
     */
    private void markApiReady() {
        if (apiReadyCount.incrementAndGet() >= totalApiReadyCount.get()) {
            baseApiReady();
        }
    }

    /**
     * 命名空间必须是合法 JS 标识符：它会被直接拼进 {@code window.<namespace>} 形式的脚本，
     * 含引号/分号/空格等字符会破坏脚本甚至造成注入。
     *
     * @param name 命名空间
     * @return 是否合法
     */
    private static boolean isValidJsIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        if (!(Character.isLetter(first) || first == '_' || first == '$')) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '$')) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void destroy() throws Exception {
        getLogger().debug("destroy window: {}", getKey());
        try {
            // 兜底回收：close() 已经销毁过时，这里是幂等的空操作
            destroyApiBridges();
        } finally {
            Stage stage = getStage();
            if (stage != null) {
                stage.close();
            }
            // bean 已从容器移除，进入终态
            transitionTo(ControllerStatusEnum.DESTROYED);
        }
    }
}