package com.cc3.springjavafx.web.dialog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 对话框组件配置，对应 {@code app.yml} 中的 {@code app.dialog.*}。
 *
 * <pre>{@code
 * app:
 *   dialog:
 *     enabled: true              # 关闭后只记日志，不再弹窗
 *     handler:                   # 自定义处理器 bean 名称/全类名，留空=自动选择
 *     title: 提示                # 默认标题
 *     confirmText: 确定
 *     cancelText: 取消
 *     stylesheet:                # 追加样式表（文件路径 / file: / classpath: / http）
 *     animate: true              # 是否启用淡入动画
 *     jsErrorDialog: false       # JS 错误是否弹窗（默认只记日志，避免打扰）
 * }</pre>
 *
 * @author A-walking-star
 */
@Component
public class WebDialogProperties {

    /** 是否启用对话框组件 */
    @Value("${app.dialog.enabled:true}")
    private boolean enabled = true;

    /** 指定的处理器：bean 名称或全类名，空表示自动选择 */
    @Value("${app.dialog.handler:}")
    private String handler;

    /** 默认标题 */
    @Value("${app.dialog.title:提示}")
    private String title = "提示";

    /** 确定按钮文案 */
    @Value("${app.dialog.confirmText:确定}")
    private String confirmText = "确定";

    /** 取消按钮文案 */
    @Value("${app.dialog.cancelText:取消}")
    private String cancelText = "取消";

    /** 追加样式表路径 */
    @Value("${app.dialog.stylesheet:}")
    private String stylesheet;

    /** 是否启用淡入动画 */
    @Value("${app.dialog.animate:true}")
    private boolean animated = true;

    /** JS 错误是否弹窗 */
    @Value("${app.dialog.jsErrorDialog:false}")
    private boolean jsErrorDialog = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getConfirmText() {
        return confirmText;
    }

    public void setConfirmText(String confirmText) {
        this.confirmText = confirmText;
    }

    public String getCancelText() {
        return cancelText;
    }

    public void setCancelText(String cancelText) {
        this.cancelText = cancelText;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public boolean isJsErrorDialog() {
        return jsErrorDialog;
    }

    public void setJsErrorDialog(boolean jsErrorDialog) {
        this.jsErrorDialog = jsErrorDialog;
    }
}
