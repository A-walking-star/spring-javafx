package com.cc3.springjavafx.api;

import com.cc3.springjavafx.controller.BaseController;
import com.cc3.springjavafx.util.*;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 窗口 API。
 *
 * <p>每个窗口持有独立实例，并绑定到加载它的窗口控制器：
 * 不带 controllerKey 参数的方法（如 {@code setTitle}、{@code closeWindow}）操作的都是当前窗口；
 * 带 controllerKey 参数的方法操作指定窗口。</p>
 *
 * <p><b>key 约定</b>：<b>只有 {@code null} 表示主窗口</b>；空串或未知 key 一律视为
 * "找不到窗口"（getter 返回 null / 默认值，setter 返回 false 或不生效）。</p>
 *
 * <p><b>线程约定</b>：前端（WebView 中的 JS）调用天然在 JavaFX 线程上，可同步读写；
 * Java 侧从后台线程调用时，"写"操作会自动投递到 JavaFX 线程（异步生效），
 * "读"操作与创建/选择窗口的同步方法应自行切到 JavaFX 线程。</p>
 *
 * <p><b>兼容性</b>：原有的 {@code xxxByControllerKey} 写方法（如 {@code setTitleByControllerKey}）
 * 由 {@code void} 改为返回 {@code boolean}（目标窗口是否存在）。Java 调用方若忽略返回值则源码兼容，
 * 重新编译即可；新增方法遵循同一约定。</p>
 *
 * @author A-walking-star
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WindowApi extends AbstractJavaApiBridge {

    public WindowApi() {
    }

    // ==================================================================
    // 文件 / 目录快捷操作（历史能力，保留在 WindowApi 中）
    // ==================================================================

    /**
     * 打开文件夹
     *
     * @param path 文件夹路径
     * @return 成功返回 null，失败返回错误信息
     */
    public String openDir(String path) {
        if (StringUtils.isBlank(path)) {
            return "路径不能为空";
        }
        File dir = new File(path);
        if (!dir.exists()) {
            return "路径不存在：" + path;
        }
        try {
            Desktop.getDesktop().open(dir);
        } catch (Throwable e) {
            error("{} 文件夹打开失败！", path, e);
            return e.getMessage();
        }
        return null;
    }

    /**
     * 选择文件夹（目录选择器）
     *
     * @param initDirPath 初始路径，可为空
     * @return 选中的绝对路径，取消返回 null
     */
    public String chooseDir(String initDirPath) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择目录");
        if (StringUtils.isNotBlank(initDirPath)) {
            File initDir = new File(initDirPath);
            if (initDir.isDirectory()) {
                chooser.setInitialDirectory(initDir);
            }
        }
        File selected = chooser.showDialog(getStage());
        return selected != null ? selected.getAbsolutePath() : null;
    }

    /**
     * 创建文件夹（父目录不存在时一并创建）
     *
     * @param path 路径
     * @return 是否创建成功
     */
    public boolean createDir(String path) {
        return AppPathUtils.createDir(path);
    }

    /**
     * 删除文件夹（递归删除）
     *
     * @param path 文件夹路径
     * @return 是否删除成功（有任何一个文件删不掉就返回 false）
     */
    public boolean deleteFiles(String path) {
        return AppPathUtils.deleteFiles(path);
    }

    // ==================================================================
    // 窗口创建
    // ==================================================================

    /**
     * 通过新的 html 创建窗口，并指定父窗口。
     *
     * @param htmlPath            html 位置（classpath 相对路径或 file/http 地址）
     * @param width               窗口宽度，null/0 回落到 app.defaultWidth
     * @param height              窗口高度，null/0 回落到 app.defaultHeight
     * @param parentControllerKey 父窗口 ID（null=主窗口）
     * @return controllerKey
     */
    public String createScene(String htmlPath, Double width, Double height, String parentControllerKey) throws FileNotFoundException {
        return ContextUtils.createScene(htmlPath, sizeOf(width), sizeOf(height), parentControllerKey);
    }

    /**
     * 新建 WINDOW_MODAL 模态窗口
     *
     * @param htmlPath            html 位置
     * @param width               窗口宽度
     * @param height              窗口高度
     * @param parentControllerKey 父窗口 ID（必填，WINDOW_MODAL 需要 owner）
     * @return controllerKey
     */
    public String createWindowScene(String htmlPath, Double width, Double height, String parentControllerKey) throws FileNotFoundException {
        return ContextUtils.createWindowScene(htmlPath, sizeOf(width), sizeOf(height), parentControllerKey);
    }

    /**
     * 新建 APPLICATION_MODAL 模态窗口
     *
     * @param htmlPath            html 位置
     * @param width               窗口宽度
     * @param height              窗口高度
     * @param parentControllerKey 父窗口 ID（可选）
     * @return controllerKey
     */
    public String createApplicationScene(String htmlPath, Double width, Double height, String parentControllerKey) throws FileNotFoundException {
        return ContextUtils.createApplicationScene(htmlPath, sizeOf(width), sizeOf(height), parentControllerKey);
    }

    /**
     * 以<b>当前窗口</b>为父窗口创建普通窗口。
     *
     * <p>这是最常用的开窗方式：主窗口调用时父窗口为 null，子窗口调用时父窗口就是它自己。</p>
     *
     * @param htmlPath html 位置
     * @param width    窗口宽度，null/0 使用默认值
     * @param height   窗口高度，null/0 使用默认值
     * @return controllerKey
     */
    public String createChildScene(String htmlPath, Double width, Double height) throws FileNotFoundException {
        return ContextUtils.createScene(htmlPath, sizeOf(width), sizeOf(height), currentKey());
    }

    /**
     * 以<b>当前窗口</b>为 owner 创建 WINDOW_MODAL 模态窗口。
     *
     * @param htmlPath html 位置
     * @param width    窗口宽度
     * @param height   窗口高度
     * @return controllerKey
     */
    public String createChildWindowScene(String htmlPath, Double width, Double height) throws FileNotFoundException {
        return ContextUtils.createWindowScene(htmlPath, sizeOf(width), sizeOf(height), currentKey());
    }

    /**
     * 以<b>当前窗口</b>为 owner 创建 APPLICATION_MODAL 模态窗口。
     *
     * @param htmlPath html 位置
     * @param width    窗口宽度
     * @param height   窗口高度
     * @return controllerKey
     */
    public String createChildApplicationScene(String htmlPath, Double width, Double height) throws FileNotFoundException {
        return ContextUtils.createApplicationScene(htmlPath, sizeOf(width), sizeOf(height), currentKey());
    }

    /**
     * JS 侧可能传 {@code null}（未传宽度/高度），不能直接拆箱，否则 NPE。
     * 0 表示"未指定"，由 {@code ContextUtils} 回落到 {@code app.defaultWidth/Height}。
     *
     * @param value JS 传入的尺寸
     * @return 原始值，null 视为 0
     */
    private static double sizeOf(Double value) {
        return value == null ? 0 : value;
    }

    // ==================================================================
    // 当前窗口：标识与层级
    // ==================================================================

    /**
     * 获取当前窗口 ID。
     *
     * @return 窗口 ID，主窗口返回 null
     */
    public String getKey() {
        BaseController c = this.controller;
        return c != null ? c.getKey() : null;
    }

    /**
     * 当前窗口是否为主窗口。
     *
     * @return 主窗口返回 true
     */
    public boolean isMainWindow() {
        return getKey() == null;
    }

    /**
     * 获取当前窗口的父窗口 ID。
     *
     * @return 父窗口 ID，null 表示无父窗口（主窗口或孤儿窗口）
     */
    public String getParentKey() {
        BaseController c = this.controller;
        return c != null ? c.getParentKey() : null;
    }

    /**
     * 获取当前窗口的所有子窗口 ID。
     *
     * <p><b>JS 侧注意</b>：JavaFX WebView 把 {@code String[]} 返回成真正的 JS 数组，
     * 但数组元素是 Java String 的包装对象而不是原始字符串，因此
     * {@code keys[0] === 'xxx'} 恒为 false、{@code JSON.stringify(keys)} 得到 {@code [{}]}。
     * 前端取值请统一用 {@code String(keys[i])}（或用 {@code join(',')}、{@code Array.prototype.map}
     * 配合 String 转换）。</p>
     *
     * @return 子窗口 ID 集合（按 key 升序，便于展示）
     */
    public String[] getChildKeys() {
        BaseController c = this.controller;
        if (c == null) {
            return new String[0];
        }
        return keysOf(c.getAllChildController());
    }

    /**
     * 获取当前应用的<b>全部</b>普通窗口 ID（不含主窗口，主窗口的 key 是 null）。
     *
     * <p>包含 Java 侧创建的窗口，以及其它页面实例创建的子窗口；只返回仍存活（未结束）的窗口。</p>
     *
     * @return 窗口 ID 集合（按 key 升序）
     */
    public String[] getAllKeys() {
        List<String> keys = new ArrayList<>();
        for (BaseController c : ContextUtils.getAllController()) {
            if (isAlive(c) && c.getKey() != null) {
                keys.add(c.getKey());
            }
        }
        Collections.sort(keys);
        return keys.toArray(new String[0]);
    }

    /**
     * 获取当前应用的窗口总数（包含主窗口）。
     *
     * @return 仍存活的窗口数量
     */
    public int getWindowCount() {
        int count = 0;
        for (BaseController c : ContextUtils.getAllController()) {
            if (isAlive(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取当前窗口的生命周期状态。
     *
     * @return 形如 {@code OPENED(已打开)} 的状态名，桥接器未绑定时返回 {@code DESTROYED(已销毁)}
     */
    public String getStatus() {
        BaseController c = this.controller;
        return c != null ? String.valueOf(c.getStatus()) : "DESTROYED(已销毁)";
    }

    /**
     * 当前窗口是否已就绪（窗口已显示、页面已加载）。
     *
     * @return 是否已就绪
     */
    public boolean isOpened() {
        BaseController c = this.controller;
        return c != null && c.isOpened();
    }

    // ==================================================================
    // 当前窗口：标题与尺寸
    // ==================================================================

    /**
     * 获取标题
     *
     * @return 标题，窗口不可用时返回 null
     */
    public String getTitle() {
        Stage stage = getStage();
        return stage != null ? stage.getTitle() : null;
    }

    /**
     * 设置标题
     *
     * @param title 标题，null 视为空串
     */
    public void setTitle(String title) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setTitle");
            return;
        }
        final String value = title == null ? "" : title;
        onFxThread("setTitle", () -> stage.setTitle(value));
    }

    /**
     * 获取宽度（含窗口边框，由 JavaFX 提供）
     *
     * @return 宽度，窗口不可用时返回 0
     */
    public double getWidth() {
        Stage stage = getStage();
        return stage != null ? stage.getWidth() : 0d;
    }

    /**
     * 设置宽度
     *
     * @param width 宽度，必须为正数且有限
     */
    public void setWidth(double width) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setWidth");
            return;
        }
        if (!isValidSize(width)) {
            warn("setWidth 忽略非法宽度：{}（要求大于 0 的有限数值）", width);
            return;
        }
        onFxThread("setWidth", () -> stage.setWidth(width));
    }

    /**
     * 获取高度
     *
     * @return 高度，窗口不可用时返回 0
     */
    public double getHeight() {
        Stage stage = getStage();
        return stage != null ? stage.getHeight() : 0d;
    }

    /**
     * 设置高度
     *
     * @param height 高度，必须为正数且有限
     */
    public void setHeight(double height) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setHeight");
            return;
        }
        if (!isValidSize(height)) {
            warn("setHeight 忽略非法高度：{}（要求大于 0 的有限数值）", height);
            return;
        }
        onFxThread("setHeight", () -> stage.setHeight(height));
    }

    /**
     * 同时设置宽度和高度（一次布局，避免两次 set 引起的中间态）。
     *
     * @param width  宽度，非法值会被忽略
     * @param height 高度，非法值会被忽略
     */
    public void setSize(double width, double height) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setSize");
            return;
        }
        if (!isValidSize(width) || !isValidSize(height)) {
            warn("setSize 忽略非法尺寸：{} × {}", width, height);
            return;
        }
        onFxThread("setSize", () -> {
            stage.setWidth(width);
            stage.setHeight(height);
        });
    }

    /**
     * 获取最小宽度
     *
     * @return 最小宽度，窗口不可用时返回 0
     */
    public double getMinWidth() {
        Stage stage = getStage();
        return stage != null ? stage.getMinWidth() : 0d;
    }

    /**
     * 设置最小宽度
     *
     * @param width 最小宽度（0 表示不限制）
     */
    public void setMinWidth(double width) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setMinWidth");
            return;
        }
        if (!isValidLimit(width)) {
            warn("setMinWidth 忽略非法值：{}", width);
            return;
        }
        onFxThread("setMinWidth", () -> stage.setMinWidth(width));
    }

    /**
     * 获取最小高度
     *
     * @return 最小高度，窗口不可用时返回 0
     */
    public double getMinHeight() {
        Stage stage = getStage();
        return stage != null ? stage.getMinHeight() : 0d;
    }

    /**
     * 设置最小高度
     *
     * @param height 最小高度（0 表示不限制）
     */
    public void setMinHeight(double height) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setMinHeight");
            return;
        }
        if (!isValidLimit(height)) {
            warn("setMinHeight 忽略非法值：{}", height);
            return;
        }
        onFxThread("setMinHeight", () -> stage.setMinHeight(height));
    }

    /**
     * 同时设置最小宽度和最小高度。
     *
     * @param width  最小宽度（0 表示不限制）
     * @param height 最小高度（0 表示不限制）
     */
    public void setMinSize(double width, double height) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setMinSize");
            return;
        }
        if (!isValidLimit(width) || !isValidLimit(height)) {
            warn("setMinSize 忽略非法值：{} × {}", width, height);
            return;
        }
        onFxThread("setMinSize", () -> {
            stage.setMinWidth(width);
            stage.setMinHeight(height);
        });
    }

    /**
     * 设置当前窗口是否可调整大小
     *
     * @param resizable 是否可调整大小
     */
    public void setResizable(boolean resizable) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setResizable");
            return;
        }
        onFxThread("setResizable", () -> stage.setResizable(resizable));
    }

    /**
     * 获取当前窗口是否可调整大小
     *
     * @return 是否可调整大小，窗口不可用时返回 false
     */
    public boolean getResizable() {
        Stage stage = getStage();
        return stage != null && stage.isResizable();
    }

    // ==================================================================
    // 当前窗口：位置与外观
    // ==================================================================

    /**
     * 获取窗口左上角 X 坐标
     *
     * @return X 坐标，窗口不可用时返回 0
     */
    public double getX() {
        Stage stage = getStage();
        return stage != null ? stage.getX() : 0d;
    }

    /**
     * 获取窗口左上角 Y 坐标
     *
     * @return Y 坐标，窗口不可用时返回 0
     */
    public double getY() {
        Stage stage = getStage();
        return stage != null ? stage.getY() : 0d;
    }

    /**
     * 设置窗口左上角 X 坐标（支持负值，便于多显示器）
     *
     * @param x X 坐标
     */
    public void setX(double x) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setX");
            return;
        }
        if (!isFinite(x)) {
            warn("setX 忽略非法坐标：{}", x);
            return;
        }
        onFxThread("setX", () -> stage.setX(x));
    }

    /**
     * 设置窗口左上角 Y 坐标（支持负值，便于多显示器）
     *
     * @param y Y 坐标
     */
    public void setY(double y) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setY");
            return;
        }
        if (!isFinite(y)) {
            warn("setY 忽略非法坐标：{}", y);
            return;
        }
        onFxThread("setY", () -> stage.setY(y));
    }

    /**
     * 同时设置窗口位置。
     *
     * @param x X 坐标
     * @param y Y 坐标
     */
    public void setPosition(double x, double y) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setPosition");
            return;
        }
        if (!isFinite(x) || !isFinite(y)) {
            warn("setPosition 忽略非法坐标：({}, {})", x, y);
            return;
        }
        onFxThread("setPosition", () -> {
            stage.setX(x);
            stage.setY(y);
        });
    }

    /**
     * 把窗口移动到当前屏幕的可见区域中央（多显示器时取窗口所在屏幕）。
     */
    public void centerOnScreen() {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("centerOnScreen");
            return;
        }
        onFxThread("centerOnScreen", () -> center(stage));
    }

    /**
     * 获取窗口不透明度
     *
     * @return 0.0 ~ 1.0，窗口不可用时返回 1.0
     */
    public double getOpacity() {
        Stage stage = getStage();
        return stage != null ? stage.getOpacity() : 1d;
    }

    /**
     * 设置窗口不透明度（会按 0.0 ~ 1.0 截断）。
     *
     * @param opacity 不透明度
     */
    public void setOpacity(double opacity) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setOpacity");
            return;
        }
        if (!isFinite(opacity)) {
            warn("setOpacity 忽略非法值：{}", opacity);
            return;
        }
        final double value = clampOpacity(opacity);
        if (value != opacity) {
            warn("setOpacity 已把 {} 截断为 {}", opacity, value);
        }
        onFxThread("setOpacity", () -> stage.setOpacity(value));
    }

    // ==================================================================
    // 当前窗口：状态与生命周期
    // ==================================================================

    /**
     * 当前窗口是否正在显示。
     *
     * @return 是否显示中
     */
    public boolean isShowing() {
        Stage stage = getStage();
        return stage != null && stage.isShowing();
    }

    /**
     * 当前窗口是否已最小化。
     *
     * @return 是否已最小化
     */
    public boolean isIconified() {
        Stage stage = getStage();
        return stage != null && stage.isIconified();
    }

    /**
     * 最小化当前窗口到任务栏。
     */
    public void minimizeWindow() {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("minimizeWindow");
            return;
        }
        onFxThread("minimizeWindow", () -> stage.setIconified(true));
    }

    /**
     * 最小化当前窗口（{@link #minimizeWindow()} 的历史别名，保留兼容）。
     */
    public void hiddenWindow() {
        minimizeWindow();
    }

    /**
     * 从最小化恢复，并置顶/获取焦点。
     */
    public void restoreWindow() {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("restoreWindow");
            return;
        }
        onFxThread("restoreWindow", () -> restore(stage));
    }

    /**
     * 显示并前置当前窗口（等价于"确保窗口可见"）。
     *
     * <p>若窗口已被关闭（但桥接器仍可用），会重新 show；重复调用安全。</p>
     */
    public void showWindow() {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("showWindow");
            return;
        }
        onFxThread("showWindow", () -> {
            if (!stage.isShowing()) {
                stage.show();
            }
            restore(stage);
        });
    }

    /**
     * 设置当前窗口是否最大化。
     *
     * @param maximized 是否最大化
     */
    public void setMaximized(boolean maximized) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setMaximized");
            return;
        }
        onFxThread("setMaximized", () -> stage.setMaximized(maximized));
    }

    /**
     * 获取当前窗口是否最大化。
     *
     * @return 是否最大化
     */
    public boolean isMaximized() {
        Stage stage = getStage();
        return stage != null && stage.isMaximized();
    }

    /**
     * 在最大化和还原之间切换。
     */
    public void toggleMaximized() {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("toggleMaximized");
            return;
        }
        onFxThread("toggleMaximized", () -> stage.setMaximized(!stage.isMaximized()));
    }

    /**
     * 设置当前窗口是否全屏。
     *
     * @param fullScreen 是否全屏
     */
    public void setFullScreen(boolean fullScreen) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setFullScreen");
            return;
        }
        onFxThread("setFullScreen", () -> stage.setFullScreen(fullScreen));
    }

    /**
     * 获取当前窗口是否全屏。
     *
     * @return 是否全屏
     */
    public boolean isFullScreen() {
        Stage stage = getStage();
        return stage != null && stage.isFullScreen();
    }

    /**
     * 切换全屏。
     */
    public void toggleFullScreen() {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("toggleFullScreen");
            return;
        }
        onFxThread("toggleFullScreen", () -> stage.setFullScreen(!stage.isFullScreen()));
    }

    /**
     * 设置当前窗口是否总在最前。
     *
     * @param alwaysOnTop 是否置顶
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("setAlwaysOnTop");
            return;
        }
        onFxThread("setAlwaysOnTop", () -> stage.setAlwaysOnTop(alwaysOnTop));
    }

    /**
     * 获取当前窗口是否总在最前。
     *
     * @return 是否置顶
     */
    public boolean isAlwaysOnTop() {
        Stage stage = getStage();
        return stage != null && stage.isAlwaysOnTop();
    }

    /**
     * 把当前窗口置于最前并请求焦点。
     */
    public void toFront() {
        final Stage stage = getStage();
        if (stage == null) {
            logWindowGone("toFront");
            return;
        }
        onFxThread("toFront", () -> {
            stage.toFront();
            stage.requestFocus();
        });
    }

    /**
     * 关闭当前窗口（强制关闭，不会触发 {@code setOnCloseRequest} 二次确认）。
     *
     * <p>主窗口关闭后应用会退出（JavaFX 隐式退出）。需要走关闭确认逻辑时用
     * {@link #requestCloseWindow()}。</p>
     */
    public void closeWindow() {
        final BaseController c = this.controller;
        if (c == null) {
            logWindowGone("closeWindow");
            return;
        }
        closeLater(c);
    }

    /**
     * 请求关闭当前窗口：先触发 {@code WINDOW_CLOSE_REQUEST} 事件（会走窗口自己的
     * {@code setOnCloseRequest} 逻辑，如"未保存"确认），事件未被 {@code consume()} 时兜底关闭。
     *
     * <p>主窗口的默认处理器会退出进程；子窗口的默认处理器会关闭窗口。</p>
     */
    public void requestCloseWindow() {
        final BaseController c = this.controller;
        if (c == null) {
            logWindowGone("requestCloseWindow");
            return;
        }
        requestCloseLater(c);
    }

    /**
     * 关闭当前窗口的全部子窗口。
     *
     * <p>须在 JavaFX 线程调用（前端调用天然满足）；关闭是同步的，返回时子窗口已被移除。</p>
     *
     * @return 实际提交关闭的子窗口数量
     */
    public int closeChildWindows() {
        BaseController c = this.controller;
        if (c == null) {
            logWindowGone("closeChildWindows");
            return 0;
        }
        int closed = 0;
        for (BaseController child : new ArrayList<>(c.getAllChildController())) {
            if (child != null && ContextUtils.closeController(child)) {
                closed++;
            }
        }
        if (closed > 0) {
            info("已关闭窗口 [{}] 的 {} 个子窗口", c.getKey() == null ? "main" : c.getKey(), closed);
        }
        return closed;
    }

    // ==================================================================
    // 跨窗口：层级与查询
    // ==================================================================

    /**
     * 获取指定窗口的父窗口 ID
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 父窗口 ID，找不到窗口或无父窗口返回 null
     */
    public String getParentKeyByControllerKey(String controllerKey) {
        BaseController c = ContextUtils.getController(controllerKey);
        return c != null ? c.getParentKey() : null;
    }

    /**
     * 获取指定窗口的所有子窗口 ID
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 子窗口 ID 集合（按 key 升序），找不到窗口返回空数组
     */
    public String[] getChildKeysByControllerKey(String controllerKey) {
        BaseController c = ContextUtils.getController(controllerKey);
        if (c == null) {
            return new String[0];
        }
        return keysOf(c.getAllChildController());
    }

    /**
     * 判断指定窗口是否存在且仍存活。
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 是否存在
     */
    public boolean hasWindow(String controllerKey) {
        BaseController c = ContextUtils.getController(controllerKey);
        return c != null && !c.isEnded();
    }

    /**
     * 获取指定窗口的生命周期状态
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 形如 {@code OPENED(已打开)}，找不到窗口返回 null
     */
    public String getStatusByControllerKey(String controllerKey) {
        BaseController c = ContextUtils.getController(controllerKey);
        return c != null ? String.valueOf(c.getStatus()) : null;
    }

    /**
     * 判断指定窗口是否已就绪
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 是否已就绪，找不到窗口返回 false
     */
    public boolean isOpenedByControllerKey(String controllerKey) {
        BaseController c = ContextUtils.getController(controllerKey);
        return c != null && c.isOpened();
    }

    /**
     * 获取指定窗口标题
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 窗口标题，找不到窗口返回 null
     */
    public String getTitleByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.getTitle() : null;
    }

    /**
     * 获取指定窗口宽度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 窗口宽度，找不到窗口返回 null
     */
    public Double getWidthByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.getWidth() : null;
    }

    /**
     * 获取指定窗口高度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 窗口高度，找不到窗口返回 null
     */
    public Double getHeightByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.getHeight() : null;
    }

    /**
     * 获取指定窗口最小宽度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 窗口最小宽度，找不到窗口返回 null
     */
    public Double getMinWidthByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.getMinWidth() : null;
    }

    /**
     * 获取指定窗口最小高度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 窗口最小高度，找不到窗口返回 null
     */
    public Double getMinHeightByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.getMinHeight() : null;
    }

    /**
     * 获取指定窗口是否可调整大小
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 是否可调整大小，找不到窗口返回 null
     */
    public Boolean getResizableByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.isResizable() : null;
    }

    /**
     * 获取指定窗口左上角 X 坐标
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return X 坐标，找不到窗口返回 null
     */
    public Double getXByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.getX() : null;
    }

    /**
     * 获取指定窗口左上角 Y 坐标
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return Y 坐标，找不到窗口返回 null
     */
    public Double getYByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.getY() : null;
    }

    /**
     * 获取指定窗口不透明度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 不透明度，找不到窗口返回 null
     */
    public Double getOpacityByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.getOpacity() : null;
    }

    /**
     * 获取指定窗口是否正在显示
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 是否显示中，找不到窗口返回 null
     */
    public Boolean isShowingByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.isShowing() : null;
    }

    /**
     * 获取指定窗口是否已最小化
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 是否已最小化，找不到窗口返回 null
     */
    public Boolean isIconifiedByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.isIconified() : null;
    }

    /**
     * 获取指定窗口是否最大化
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 是否最大化，找不到窗口返回 null
     */
    public Boolean isMaximizedByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.isMaximized() : null;
    }

    /**
     * 获取指定窗口是否全屏
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 是否全屏，找不到窗口返回 null
     */
    public Boolean isFullScreenByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.isFullScreen() : null;
    }

    /**
     * 获取指定窗口是否总在最前
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 是否置顶，找不到窗口返回 null
     */
    public Boolean isAlwaysOnTopByControllerKey(String controllerKey) {
        Stage stage = stageByKey(controllerKey);
        return stage != null ? stage.isAlwaysOnTop() : null;
    }

    // ==================================================================
    // 跨窗口：属性设置
    // ==================================================================

    /**
     * 设置指定窗口标题
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param title         标题，null 视为空串
     * @return 目标窗口是否存在
     */
    public boolean setTitleByControllerKey(String controllerKey, String title) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        final String value = title == null ? "" : title;
        onFxThread("setTitleByControllerKey", () -> stage.setTitle(value));
        return true;
    }

    /**
     * 设置指定窗口宽度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param width         宽度
     * @return 目标窗口是否存在
     */
    public boolean setWidthByControllerKey(String controllerKey, double width) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        if (!isValidSize(width)) {
            warn("setWidthByControllerKey 忽略非法宽度：{}", width);
            return false;
        }
        onFxThread("setWidthByControllerKey", () -> stage.setWidth(width));
        return true;
    }

    /**
     * 设置指定窗口高度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param height        高度
     * @return 目标窗口是否存在
     */
    public boolean setHeightByControllerKey(String controllerKey, double height) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        if (!isValidSize(height)) {
            warn("setHeightByControllerKey 忽略非法高度：{}", height);
            return false;
        }
        onFxThread("setHeightByControllerKey", () -> stage.setHeight(height));
        return true;
    }

    /**
     * 同时设置指定窗口的宽高
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param width         宽度
     * @param height        高度
     * @return 目标窗口是否存在
     */
    public boolean setSizeByControllerKey(String controllerKey, double width, double height) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        if (!isValidSize(width) || !isValidSize(height)) {
            warn("setSizeByControllerKey 忽略非法尺寸：{} × {}", width, height);
            return false;
        }
        onFxThread("setSizeByControllerKey", () -> {
            stage.setWidth(width);
            stage.setHeight(height);
        });
        return true;
    }

    /**
     * 设置指定窗口最小宽度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param width         最小宽度（0 表示不限制）
     * @return 目标窗口是否存在
     */
    public boolean setMinWidthByControllerKey(String controllerKey, double width) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        if (!isValidLimit(width)) {
            warn("setMinWidthByControllerKey 忽略非法值：{}", width);
            return false;
        }
        onFxThread("setMinWidthByControllerKey", () -> stage.setMinWidth(width));
        return true;
    }

    /**
     * 设置指定窗口最小高度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param height        最小高度（0 表示不限制）
     * @return 目标窗口是否存在
     */
    public boolean setMinHeightByControllerKey(String controllerKey, double height) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        if (!isValidLimit(height)) {
            warn("setMinHeightByControllerKey 忽略非法值：{}", height);
            return false;
        }
        onFxThread("setMinHeightByControllerKey", () -> stage.setMinHeight(height));
        return true;
    }

    /**
     * 同时设置指定窗口的最小宽高
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param width         最小宽度（0 表示不限制）
     * @param height        最小高度（0 表示不限制）
     * @return 目标窗口是否存在
     */
    public boolean setMinSizeByControllerKey(String controllerKey, double width, double height) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        if (!isValidLimit(width) || !isValidLimit(height)) {
            warn("setMinSizeByControllerKey 忽略非法值：{} × {}", width, height);
            return false;
        }
        onFxThread("setMinSizeByControllerKey", () -> {
            stage.setMinWidth(width);
            stage.setMinHeight(height);
        });
        return true;
    }

    /**
     * 设置指定窗口是否可调整大小
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param resizable     是否可调整大小
     * @return 目标窗口是否存在
     */
    public boolean setResizableByControllerKey(String controllerKey, boolean resizable) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("setResizableByControllerKey", () -> stage.setResizable(resizable));
        return true;
    }

    /**
     * 设置指定窗口左上角 X 坐标
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param x             X 坐标
     * @return 目标窗口是否存在
     */
    public boolean setXByControllerKey(String controllerKey, double x) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null || !isFinite(x)) {
            return false;
        }
        onFxThread("setXByControllerKey", () -> stage.setX(x));
        return true;
    }

    /**
     * 设置指定窗口左上角 Y 坐标
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param y             Y 坐标
     * @return 目标窗口是否存在
     */
    public boolean setYByControllerKey(String controllerKey, double y) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null || !isFinite(y)) {
            return false;
        }
        onFxThread("setYByControllerKey", () -> stage.setY(y));
        return true;
    }

    /**
     * 同时设置指定窗口的位置
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param x             X 坐标
     * @param y             Y 坐标
     * @return 目标窗口是否存在
     */
    public boolean setPositionByControllerKey(String controllerKey, double x, double y) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null || !isFinite(x) || !isFinite(y)) {
            return false;
        }
        onFxThread("setPositionByControllerKey", () -> {
            stage.setX(x);
            stage.setY(y);
        });
        return true;
    }

    /**
     * 把指定窗口移动到其所在屏幕的可见区域中央
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 目标窗口是否存在
     */
    public boolean centerOnScreenByControllerKey(String controllerKey) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("centerOnScreenByControllerKey", () -> center(stage));
        return true;
    }

    /**
     * 设置指定窗口不透明度
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param opacity       不透明度（会按 0.0 ~ 1.0 截断）
     * @return 目标窗口是否存在
     */
    public boolean setOpacityByControllerKey(String controllerKey, double opacity) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null || !isFinite(opacity)) {
            return false;
        }
        final double value = clampOpacity(opacity);
        onFxThread("setOpacityByControllerKey", () -> stage.setOpacity(value));
        return true;
    }

    // ==================================================================
    // 跨窗口：状态与生命周期
    // ==================================================================

    /**
     * 最小化指定窗口
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 目标窗口是否存在
     */
    public boolean minimizeWindowByControllerKey(String controllerKey) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("minimizeWindowByControllerKey", () -> stage.setIconified(true));
        return true;
    }

    /**
     * 最小化指定窗口（{@link #minimizeWindowByControllerKey(String)} 的历史别名）。
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     */
    public void hiddenWindowByControllerKey(String controllerKey) {
        minimizeWindowByControllerKey(controllerKey);
    }

    /**
     * 恢复指定窗口（取消最小化、置顶并获取焦点）
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 目标窗口是否存在
     */
    public boolean restoreWindowByControllerKey(String controllerKey) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("restoreWindowByControllerKey", () -> restore(stage));
        return true;
    }

    /**
     * 显示并前置指定窗口
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 目标窗口是否存在
     */
    public boolean showWindowByControllerKey(String controllerKey) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("showWindowByControllerKey", () -> {
            if (!stage.isShowing()) {
                stage.show();
            }
            restore(stage);
        });
        return true;
    }

    /**
     * 设置指定窗口是否最大化
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param maximized     是否最大化
     * @return 目标窗口是否存在
     */
    public boolean setMaximizedByControllerKey(String controllerKey, boolean maximized) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("setMaximizedByControllerKey", () -> stage.setMaximized(maximized));
        return true;
    }

    /**
     * 设置指定窗口是否全屏
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param fullScreen    是否全屏
     * @return 目标窗口是否存在
     */
    public boolean setFullScreenByControllerKey(String controllerKey, boolean fullScreen) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("setFullScreenByControllerKey", () -> stage.setFullScreen(fullScreen));
        return true;
    }

    /**
     * 设置指定窗口是否总在最前
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param alwaysOnTop   是否置顶
     * @return 目标窗口是否存在
     */
    public boolean setAlwaysOnTopByControllerKey(String controllerKey, boolean alwaysOnTop) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("setAlwaysOnTopByControllerKey", () -> stage.setAlwaysOnTop(alwaysOnTop));
        return true;
    }

    /**
     * 把指定窗口置于最前并请求焦点
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @return 目标窗口是否存在
     */
    public boolean toFrontByControllerKey(String controllerKey) {
        final Stage stage = stageByKey(controllerKey);
        if (stage == null) {
            return false;
        }
        onFxThread("toFrontByControllerKey", () -> {
            stage.toFront();
            stage.requestFocus();
        });
        return true;
    }

    /**
     * 关闭指定窗口（强制关闭，不触发关闭确认）
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     */
    public void closeWindowByControllerKey(String controllerKey) {
        BaseController target = ContextUtils.getController(controllerKey);
        if (target == null) {
            this.logger.warn("关闭窗口失败：controllerKey 不存在 [{}]", controllerKey);
            return;
        }
        closeLater(target);
    }

    /**
     * 请求关闭指定窗口（先触发 {@code WINDOW_CLOSE_REQUEST}，未被拦截时兜底关闭）
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     */
    public void requestCloseWindowByControllerKey(String controllerKey) {
        BaseController target = ContextUtils.getController(controllerKey);
        if (target == null) {
            this.logger.warn("请求关闭窗口失败：controllerKey 不存在 [{}]", controllerKey);
            return;
        }
        requestCloseLater(target);
    }

    // ==================================================================
    // 回调
    // ==================================================================

    /**
     * 触发指定窗口的前端回调
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param callBack      回调方法名
     */
    public void onCallByControllerKey(String controllerKey, String callBack) {
        BaseController c = ContextUtils.getController(controllerKey);
        if (c == null) {
            error("onCallByControllerKey 回调失败！窗口id：{}；不存在", controllerKey);
            return;
        }
        callBack(c, callBack, "onCallByControllerKey");
    }

    /**
     * 触发所有窗口的前端回调
     *
     * @param callBack 回调方法名
     */
    public void onCallAllStage(String callBack) {
        for (BaseController c : ContextUtils.getAllController()) {
            if (!isAlive(c)) {
                continue;
            }
            callBack(c, callBack, "onCallAllStage");
        }
    }

    /**
     * 在指定窗口执行一段 JS。
     *
     * <p>比 {@code onCallByControllerKey} 更通用：可以传带参数的表达式。
     * 脚本会在 JavaFX 线程执行，异常只记日志，不影响调用方。</p>
     *
     * @param controllerKey 窗口唯一 ID（null 表示主窗口）
     * @param script        JS 脚本，例如 {@code "console.log('hi')"}
     * @return 目标窗口是否存在
     */
    public boolean executeJsByControllerKey(String controllerKey, final String script) {
        final BaseController c = ContextUtils.getController(controllerKey);
        if (c == null) {
            return false;
        }
        if (StringUtils.isBlank(script)) {
            warn("executeJsByControllerKey 忽略空脚本");
            return false;
        }
        try {
            Platform.runLater(() -> {
                try {
                    if (c.getWebEngine() != null) {
                        c.getWebEngine().executeScript(script);
                    }
                } catch (Throwable e) {
                    warn("窗口 [{}] 执行 JS 失败：{}", c.getKey(), e.getMessage());
                }
            });
        } catch (Throwable e) {
            warn("执行 JS 投递到 JavaFX 线程失败：{}", e.getMessage());
        }
        return true;
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    /**
     * 当前窗口 key（主窗口为 null）
     */
    private String currentKey() {
        BaseController c = this.controller;
        return c != null ? c.getKey() : null;
    }

    /**
     * 按 controllerKey 解析 Stage（null 表示主窗口）
     */
    private static Stage stageByKey(String controllerKey) {
        BaseController c = ContextUtils.getController(controllerKey);
        return c != null ? c.getStage() : null;
    }

    /**
     * 窗口是否仍可用：未结束且 Stage 存在
     */
    private static boolean isAlive(BaseController c) {
        return c != null && !c.isEnded() && c.getStage() != null;
    }

    /**
     * 子窗口集合 → 稳定排序的 key 数组（过滤 null / 已结束窗口）
     */
    private static String[] keysOf(Set<BaseController> controllers) {
        List<String> keys = new ArrayList<>();
        if (controllers != null) {
            for (BaseController child : controllers) {
                if (isAlive(child) && child.getKey() != null) {
                    keys.add(child.getKey());
                }
            }
        }
        Collections.sort(keys);
        return keys.toArray(new String[0]);
    }

    /**
     * 在 JavaFX 线程执行：已在 FX 线程时同步执行，否则投递到 FX 线程（异步生效）。
     *
     * <p>前端调用天然在 FX 线程，因此"设置后立即读取"仍是同步语义。</p>
     */
    private void onFxThread(String action, Runnable task) {
        if (task == null) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            try {
                task.run();
            } catch (Throwable e) {
                warn("{} 执行失败：{}", action, e.getMessage());
            }
            return;
        }
        try {
            Platform.runLater(() -> {
                try {
                    task.run();
                } catch (Throwable e) {
                    warn("{} 执行失败：{}", action, e.getMessage());
                }
            });
        } catch (Throwable e) {
            warn("{} 投递到 JavaFX 线程失败：{}", action, e.getMessage());
        }
    }

    /**
     * 恢复窗口：取消最小化 → 必要时 show → 置顶/聚焦
     */
    private static void restore(Stage stage) {
        if (stage.isIconified()) {
            stage.setIconified(false);
        }
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();
        stage.requestFocus();
    }

    /**
     * 把窗口放到所在屏幕中央。
     *
     * <p>窗口尚未布局时 {@code getWidth()/getHeight()} 可能是 0 或 NaN，这里做归一化，
     * 避免 {@link Screen#getScreensForRectangle} 收到非法矩形。</p>
     */
    private static void center(Stage stage) {
        double x = finiteOrZero(stage.getX());
        double y = finiteOrZero(stage.getY());
        double width = positiveOrOne(stage.getWidth());
        double height = positiveOrOne(stage.getHeight());

        Rectangle2D bounds = null;
        List<Screen> screens = Screen.getScreensForRectangle(x, y, width, height);
        if (screens != null && !screens.isEmpty()) {
            bounds = screens.get(0).getVisualBounds();
        }
        if (bounds == null) {
            bounds = Screen.getPrimary().getVisualBounds();
        }
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
    }

    private static double finiteOrZero(double value) {
        return isFinite(value) ? value : 0d;
    }

    private static double positiveOrOne(double value) {
        return isFinite(value) && value > 0 ? value : 1d;
    }

    /**
     * 延迟关闭，避免在 WebView 正在执行 JS 时销毁它的 JS 上下文
     */
    private void closeLater(final BaseController target) {
        try {
            Platform.runLater(() -> {
                try {
                    ContextUtils.closeController(target);
                } catch (Throwable e) {
                    warn("关闭窗口 [{}] 失败：{}", target.getKey(), e.getMessage());
                }
            });
        } catch (Throwable e) {
            warn("关闭窗口 [{}] 投递失败：{}", target.getKey(), e.getMessage());
        }
    }

    /**
     * 延迟触发关闭请求；处理器未拦截（未 consume）且窗口仍在显示时兜底关闭
     */
    private void requestCloseLater(final BaseController target) {
        try {
            Platform.runLater(() -> {
                Stage stage = target.getStage();
                if (stage == null) {
                    return;
                }
                try {
                    WindowEvent event = new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST);
                    stage.fireEvent(event);
                    if (!event.isConsumed() && stage.isShowing()) {
                        ContextUtils.closeController(target);
                    }
                } catch (Throwable e) {
                    warn("请求关闭窗口 [{}] 失败：{}", target.getKey(), e.getMessage());
                }
            });
        } catch (Throwable e) {
            warn("请求关闭窗口 [{}] 投递失败：{}", target.getKey(), e.getMessage());
        }
    }

    /**
     * 调用前端回调（统一异常处理）
     */
    private void callBack(BaseController c, String callBack, String action) {
        if (StringUtils.isBlank(callBack)) {
            warn("{} 忽略空回调名", action);
            return;
        }
        try {
            ParamsUtils.onCallBack(c, callBack);
        } catch (Throwable e) {
            error("{} 回调失败！窗口id：{}；错误原因：{}", action, c.getKey(), e.getMessage());
        }
    }

    private void logWindowGone(String action) {
        debug("{} 跳过：当前窗口不可用（已关闭或桥接器已销毁）", action);
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /**
     * 尺寸必须为正的有限数：0 / 负数 / NaN / Infinity 都视为非法
     */
    private static boolean isValidSize(double value) {
        return isFinite(value) && value > 0;
    }

    /**
     * 最小尺寸允许 0（表示不限制），但不允许负数/NaN/Infinity
     */
    private static boolean isValidLimit(double value) {
        return isFinite(value) && value >= 0;
    }

    private static double clampOpacity(double opacity) {
        if (opacity < 0d) {
            return 0d;
        }
        if (opacity > 1d) {
            return 1d;
        }
        return opacity;
    }
}
