package com.cc3.springjavafx.web.dialog;

/**
 * 对话框语义级别，决定内置对话框的图标、配色和主按钮颜色。
 *
 * <p>颜色不在 Java 里写死，而是通过 {@link #getCssClass()} 对应的样式类由样式表控制，
 * 因此替换/追加样式表即可整体换肤，无需改动代码。</p>
 *
 * @author A-walking-star
 */
public enum WebDialogLevel {

    /** 普通信息，蓝色 */
    INFO("level-info", "i"),

    /** 成功，绿色 */
    SUCCESS("level-success", "✓"),

    /** 警告，橙色 */
    WARNING("level-warning", "!"),

    /** 错误，红色 */
    ERROR("level-error", "✕"),

    /** 询问，紫色 */
    QUESTION("level-question", "?");

    /** 样式类名，内置样式表中用于区分配色 */
    private final String cssClass;

    /** 图标字符 */
    private final String icon;

    WebDialogLevel(String cssClass, String icon) {
        this.cssClass = cssClass;
        this.icon = icon;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getIcon() {
        return icon;
    }
}
