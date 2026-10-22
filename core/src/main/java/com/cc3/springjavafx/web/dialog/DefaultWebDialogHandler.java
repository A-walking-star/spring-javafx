package com.cc3.springjavafx.web.dialog;

import com.cc3.springjavafx.util.StringUtils;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内置对话框处理器：无边框圆角卡片 + 级别配色 + 淡入动画，居中于所属窗口。
 *
 * <p>交互约定：</p>
 * <ul>
 *     <li>{@code ALERT}：单个"确定"按钮，Enter / Esc 均可关闭；</li>
 *     <li>{@code CONFIRM}：取消 + 确定，Esc 取消，Enter 确定；</li>
 *     <li>{@code PROMPT}：带输入框，输入框回车即确定，Esc 取消。</li>
 * </ul>
 *
 * <p>定制方式（按侵入性从低到高）：</p>
 * <ol>
 *     <li>{@code app.dialog.stylesheet} 追加 CSS 覆盖配色；</li>
 *     <li>继承本类覆写 {@link #getStylesheets()}（可完全替换样式表）；</li>
 *     <li>实现 {@link WebDialogHandler} 自己画（例如用 JavaFX Dialog 或网页内浮层）。</li>
 * </ol>
 *
 * @author A-walking-star
 */
@Component(DefaultWebDialogHandler.BEAN_NAME)
public class DefaultWebDialogHandler implements WebDialogHandler {

    /** 内置实现固定 bean 名称，框架据此识别"默认实现" */
    public static final String BEAN_NAME = "defaultWebDialogHandler";

    private static final Logger logger = LoggerFactory.getLogger(DefaultWebDialogHandler.class);

    /** 内置样式表：与类同包（com/cc3/springjavafx/web/dialog/dialog.css），不占用使用方 classpath 根路径 */
    private static final String BASE_STYLESHEET = "dialog.css";

    /** 卡片宽度（TDesign Dialog 默认 480） */
    private static final double CARD_WIDTH = 480;

    /** 卡片内边距（TDesign Dialog 默认 24） */
    private static final double CARD_PADDING = 24;

    /** 卡片四周留给投影的空白 */
    private static final double SHADOW_PADDING = 24;

    /** 入场动画时长 */
    private static final Duration ANIMATION_DURATION = Duration.millis(150);

    private final WebDialogProperties properties;

    @Autowired
    public DefaultWebDialogHandler(WebDialogProperties properties) {
        this.properties = properties;
    }

    @Override
    public WebDialogResult show(WebDialogRequest request) {
        if (request == null) {
            return WebDialogResult.cancel();
        }
        if (Platform.isFxApplicationThread()) {
            return buildAndShow(request);
        }
        // 允许 Java 侧在后台线程调用：切到 FX 线程并等待结果
        return showOnFxThread(request);
    }

    // ---------------------------- 构建与显示 ----------------------------

    /**
     * 构建并显示对话框，阻塞直到用户操作完成
     * @param request 请求
     * @return 结果
     */
    private WebDialogResult buildAndShow(WebDialogRequest request) {
        final WebDialogType type = request.getType();
        final WebDialogLevel level = request.getLevel();
        final boolean[] confirmed = {false};

        final Stage dialog = new Stage();
        dialog.setTitle(titleOf(request));
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);
        Window owner = request.getOwner();
        boolean hasOwner = owner != null && owner.isShowing();
        if (hasOwner) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);
        } else {
            dialog.initModality(Modality.NONE);
        }

        // 标题区：图标 + 标题 + 空白 + 关闭按钮（TDesign Dialog 布局）
        Label icon = new Label(level.getIcon());
        icon.getStyleClass().addAll("web-dialog-icon", level.getCssClass());
        Label title = new Label(titleOf(request));
        title.getStyleClass().add("web-dialog-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("web-dialog-close");
        closeButton.setOnAction(e -> dialog.close());
        HBox header = new HBox(8, icon, title, spacer, closeButton);
        header.getStyleClass().add("web-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // 正文
        Label message = new Label(messageOf(request));
        message.getStyleClass().add("web-dialog-message");
        message.setWrapText(true);
        message.setMaxWidth(CARD_WIDTH - 2 * CARD_PADDING);

        VBox card = new VBox(16, header, message);
        card.getStyleClass().add("web-dialog-card");
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);

        // 输入框
        final TextField input;
        if (type == WebDialogType.PROMPT) {
            input = new TextField(request.getDefaultValue() == null ? "" : request.getDefaultValue());
            input.getStyleClass().add("web-dialog-input");
            input.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().add(input);
        } else {
            input = null;
        }

        // 按钮
        final Button confirmButton = new Button(confirmTextOf(request));
        confirmButton.getStyleClass().addAll("web-dialog-btn", "primary", level.getCssClass());
        confirmButton.setOnAction(e -> {
            confirmed[0] = true;
            dialog.close();
        });

        HBox buttons = new HBox(8);
        buttons.getStyleClass().add("web-dialog-buttons");
        buttons.setAlignment(Pos.CENTER_RIGHT);
        if (type != WebDialogType.ALERT) {
            Button cancelButton = new Button(cancelTextOf(request));
            cancelButton.getStyleClass().addAll("web-dialog-btn", "ghost");
            cancelButton.setOnAction(e -> dialog.close());
            buttons.getChildren().add(cancelButton);
        }
        buttons.getChildren().add(confirmButton);
        card.getChildren().add(buttons);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("web-dialog-root");
        // 透明边距留给投影，避免投影被场景裁掉
        root.setPadding(new Insets(SHADOW_PADDING));

        // 键盘：Esc 取消，Enter 确定（输入框回车同样确定）
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                dialog.close();
            } else if (event.getCode() == KeyCode.ENTER && input == null) {
                event.consume();
                confirmed[0] = true;
                dialog.close();
            }
        });
        if (input != null) {
            input.setOnAction(e -> {
                confirmed[0] = true;
                dialog.close();
            });
        }

        // 卡片可拖动
        final double[] dragOffset = new double[2];
        card.setOnMousePressed(e -> {
            dragOffset[0] = e.getScreenX() - dialog.getX();
            dragOffset[1] = e.getScreenY() - dialog.getY();
        });
        card.setOnMouseDragged(e -> {
            dialog.setX(e.getScreenX() - dragOffset[0]);
            dialog.setY(e.getScreenY() - dragOffset[1]);
        });

        Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().addAll(getStylesheets());
        dialog.setScene(scene);

        final boolean animated = properties.isAnimated();
        dialog.setOnShown(event -> {
            centerOn(dialog, request.getOwner());
            if (animated) {
                // 只做卡片的轻微缩放入场：不做整体淡入，避免窗口未映射完成前"看不见"
                ScaleTransition scale = new ScaleTransition(ANIMATION_DURATION, card);
                scale.setFromX(0.94);
                scale.setFromY(0.94);
                scale.setToX(1);
                scale.setToY(1);
                scale.setInterpolator(Interpolator.EASE_OUT);
                scale.play();
            }
            // 焦点：输入框优先，否则主按钮
            if (input != null) {
                input.requestFocus();
                input.selectAll();
            } else {
                confirmButton.requestFocus();
            }
        });

        try {
            dialog.showAndWait();
        } catch (Throwable e) {
            logger.error("显示对话框失败：{}", request, e);
            safelyClose(dialog);
            return WebDialogResult.cancel();
        }
        if (confirmed[0]) {
            return WebDialogResult.ok(input != null ? input.getText() : null);
        }
        return WebDialogResult.cancel();
    }

    // ---------------------------- 样式表 ----------------------------

    /**
     * 当前对话框使用的样式表（内置 + 配置追加）
     *
     * <p>子类可覆写本方法完全接管样式；只需微调时更推荐配置 {@code app.dialog.stylesheet}。</p>
     *
     * @return 样式表 URL 列表，按加载顺序排列（后者优先）
     */
    protected List<String> getStylesheets() {
        List<String> stylesheets = new ArrayList<>(2);
        URL base = DefaultWebDialogHandler.class.getResource(BASE_STYLESHEET);
        if (base != null) {
            stylesheets.add(base.toExternalForm());
        } else {
            logger.warn("内置对话框样式未找到：{}", BASE_STYLESHEET);
        }
        String custom = properties.getStylesheet();
        if (StringUtils.isNotBlank(custom)) {
            String url = resolveStylesheet(custom);
            if (url != null) {
                stylesheets.add(url);
            } else {
                logger.warn("自定义对话框样式不存在或不可读：{}", custom);
            }
        }
        return stylesheets;
    }

    /**
     * 解析样式表位置：支持 http(s)/file 直链、classpath: 前缀和本地文件路径
     * @param location 位置
     * @return URL 字符串，无效返回 null
     */
    private String resolveStylesheet(String location) {
        if (location.startsWith("http://") || location.startsWith("https://") || location.startsWith("file:")) {
            return location;
        }
        if (location.startsWith("classpath:")) {
            URL url = DefaultWebDialogHandler.class.getResource("/" + location.substring("classpath:".length()));
            return url != null ? url.toExternalForm() : null;
        }
        File file = new File(location);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), location);
        }
        return file.exists() ? file.toURI().toString() : null;
    }

    // ---------------------------- 文案与位置 ----------------------------

    private String titleOf(WebDialogRequest request) {
        if (StringUtils.isNotBlank(request.getTitle())) {
            return request.getTitle();
        }
        switch (request.getType()) {
            case CONFIRM:
                return "请确认";
            case PROMPT:
                return "请输入";
            default:
                return StringUtils.isNotBlank(properties.getTitle()) ? properties.getTitle() : "提示";
        }
    }

    private String messageOf(WebDialogRequest request) {
        return request.getMessage() == null ? "" : request.getMessage();
    }

    private String confirmTextOf(WebDialogRequest request) {
        if (StringUtils.isNotBlank(request.getConfirmText())) {
            return request.getConfirmText();
        }
        return StringUtils.isNotBlank(properties.getConfirmText()) ? properties.getConfirmText() : "确定";
    }

    private String cancelTextOf(WebDialogRequest request) {
        if (StringUtils.isNotBlank(request.getCancelText())) {
            return request.getCancelText();
        }
        return StringUtils.isNotBlank(properties.getCancelText()) ? properties.getCancelText() : "取消";
    }

    /**
     * 相对所属窗口居中；无 owner 时居中屏幕
     * @param dialog 对话框
     * @param owner 所属窗口
     */
    private void centerOn(Stage dialog, Window owner) {
        if (owner == null || !owner.isShowing()) {
            dialog.centerOnScreen();
            return;
        }
        double dialogW = dialog.getWidth();
        double dialogH = dialog.getHeight();
        if (Double.isNaN(dialogW) || Double.isNaN(dialogH)) {
            dialog.centerOnScreen();
            return;
        }
        dialog.setX(owner.getX() + (owner.getWidth() - dialogW) / 2);
        dialog.setY(owner.getY() + (owner.getHeight() - dialogH) / 2);
    }

    private void safelyClose(Stage stage) {
        try {
            stage.close();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 在非 FX 线程调用时，切到 FX 线程执行并等待结果
     * @param request 请求
     * @return 结果
     */
    private WebDialogResult showOnFxThread(final WebDialogRequest request) {
        final AtomicReference<WebDialogResult> result = new AtomicReference<>(WebDialogResult.cancel());
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.runLater(() -> {
                try {
                    result.set(buildAndShow(request));
                } catch (Throwable e) {
                    logger.error("显示对话框失败：{}", request, e);
                } finally {
                    latch.countDown();
                }
            });
        } catch (IllegalStateException e) {
            // JavaFX 运行时尚未启动
            logger.error("JavaFX 尚未启动，无法显示对话框：{}", request, e);
            return WebDialogResult.cancel();
        }
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                logger.warn("等待对话框结果超时：{}", request);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("等待对话框结果被中断：{}", request);
        }
        return result.get();
    }
}