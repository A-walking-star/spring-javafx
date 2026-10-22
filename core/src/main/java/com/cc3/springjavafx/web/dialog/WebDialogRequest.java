package com.cc3.springjavafx.web.dialog;

import javafx.stage.Window;

/**
 * 对话框请求（不可变），描述"要显示什么"，不关心"怎么显示"。
 *
 * <p>由 {@link WebDialogManager} 统一补齐标题、按钮文案、所属窗口等信息后交给
 * {@link WebDialogHandler} 实现，因此自定义处理器可以只读取自己关心的字段。</p>
 *
 * <pre>{@code
 * WebDialogRequest.builder(WebDialogType.CONFIRM)
 *         .level(WebDialogLevel.WARNING)
 *         .title("删除确认")
 *         .message("确定要删除这条记录吗？")
 *         .build();
 * }</pre>
 *
 * @author A-walking-star
 */
public final class WebDialogRequest {

    private final WebDialogType type;
    private final WebDialogLevel level;
    private final String title;
    private final String message;
    private final String defaultValue;
    private final String confirmText;
    private final String cancelText;
    private final Window owner;
    private final String sourceId;

    private WebDialogRequest(Builder builder) {
        this.type = builder.type;
        this.level = builder.level;
        this.title = builder.title;
        this.message = builder.message;
        this.defaultValue = builder.defaultValue;
        this.confirmText = builder.confirmText;
        this.cancelText = builder.cancelText;
        this.owner = builder.owner;
        this.sourceId = builder.sourceId;
    }

    /**
     * 创建构建器
     * @param type 对话框类型
     * @return Builder
     */
    public static Builder builder(WebDialogType type) {
        return new Builder(type);
    }

    /**
     * 提示框请求
     * @param message 提示内容
     * @return 请求
     */
    public static WebDialogRequest alert(String message) {
        return builder(WebDialogType.ALERT).message(message).build();
    }

    /**
     * 确认框请求
     * @param message 确认内容
     * @return 请求
     */
    public static WebDialogRequest confirm(String message) {
        return builder(WebDialogType.CONFIRM).level(WebDialogLevel.QUESTION).message(message).build();
    }

    /**
     * 输入框请求
     * @param message 提示内容
     * @param defaultValue 默认值
     * @return 请求
     */
    public static WebDialogRequest prompt(String message, String defaultValue) {
        return builder(WebDialogType.PROMPT)
                .level(WebDialogLevel.QUESTION)
                .message(message)
                .defaultValue(defaultValue)
                .build();
    }

    /**
     * 基于当前请求创建构建器，便于在已有请求上做局部修改
     * @return Builder
     */
    public Builder toBuilder() {
        return new Builder(type)
                .level(level)
                .title(title)
                .message(message)
                .defaultValue(defaultValue)
                .confirmText(confirmText)
                .cancelText(cancelText)
                .owner(owner)
                .sourceId(sourceId);
    }

    public WebDialogType getType() {
        return type;
    }

    public WebDialogLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getConfirmText() {
        return confirmText;
    }

    public String getCancelText() {
        return cancelText;
    }

    /**
     * 所属窗口，用于居中与模态
     * @return Window，可能为 null
     */
    public Window getOwner() {
        return owner;
    }

    /**
     * 来源标识（窗口 key），仅用于日志排查
     * @return 来源标识
     */
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public String toString() {
        return "WebDialogRequest{type=" + type + ", level=" + level + ", sourceId=" + sourceId
                + ", message=" + message + '}';
    }

    /**
     * 请求构建器
     */
    public static final class Builder {
        private final WebDialogType type;
        private WebDialogLevel level = WebDialogLevel.INFO;
        private String title;
        private String message;
        private String defaultValue;
        private String confirmText;
        private String cancelText;
        private Window owner;
        private String sourceId;

        private Builder(WebDialogType type) {
            if (type == null) {
                throw new IllegalArgumentException("对话框类型不能为空");
            }
            this.type = type;
        }

        public Builder level(WebDialogLevel level) {
            if (level != null) {
                this.level = level;
            }
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder confirmText(String confirmText) {
            this.confirmText = confirmText;
            return this;
        }

        public Builder cancelText(String cancelText) {
            this.cancelText = cancelText;
            return this;
        }

        public Builder owner(Window owner) {
            this.owner = owner;
            return this;
        }

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public WebDialogRequest build() {
            return new WebDialogRequest(this);
        }
    }
}
