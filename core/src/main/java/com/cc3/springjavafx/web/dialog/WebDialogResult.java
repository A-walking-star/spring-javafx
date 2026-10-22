package com.cc3.springjavafx.web.dialog;

/**
 * 对话框结果。
 *
 * <p>{@code alert} 只关心是否关闭；{@code confirm} 关心 {@link #isConfirmed()}；
 * {@code prompt} 关心 {@link #isConfirmed()} + {@link #getValue()}。</p>
 *
 * @author A-walking-star
 */
public final class WebDialogResult {

    private static final WebDialogResult CANCEL = new WebDialogResult(false, null);

    /** 是否点击了确定（或确认性按钮） */
    private final boolean confirmed;

    /** 输入内容，仅 PROMPT 有意义 */
    private final String value;

    private WebDialogResult(boolean confirmed, String value) {
        this.confirmed = confirmed;
        this.value = value;
    }

    /**
     * 确认，无返回值
     * @return 结果
     */
    public static WebDialogResult ok() {
        return new WebDialogResult(true, null);
    }

    /**
     * 确认并带回输入内容
     * @param value 输入内容
     * @return 结果
     */
    public static WebDialogResult ok(String value) {
        return new WebDialogResult(true, value);
    }

    /**
     * 取消/关闭
     * @return 结果
     */
    public static WebDialogResult cancel() {
        return CANCEL;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "WebDialogResult{confirmed=" + confirmed + ", value=" + value + '}';
    }
}
