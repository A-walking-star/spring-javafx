package com.cc3.springjavafx.web.dialog;

/**
 * 对话框类型，与 {@code WebEngine} 的原生事件一一对应。
 *
 * @author A-walking-star
 */
public enum WebDialogType {

    /** 提示框，对应 JS 的 {@code window.alert()}，只有一个确定按钮 */
    ALERT,

    /** 确认框，对应 JS 的 {@code window.confirm()}，返回是否点击了确定 */
    CONFIRM,

    /** 输入框，对应 JS 的 {@code window.prompt()}，返回输入内容，取消返回 null */
    PROMPT
}
