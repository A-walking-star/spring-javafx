package com.cc3.springjavafx.enums;

/**
 * 控制器（窗口）生命周期状态。
 *
 * <p>正常流转：</p>
 * <pre>
 * INSTANCE → OPENING → OPENED → CLOSING → CLOSED → DESTROYED
 *                └→ FAILED ────────────────┘
 * </pre>
 *
 * <p>约定：</p>
 * <ul>
 *     <li>{@link #DESTROYED} 是终态，bean 已从容器移除，不允许再流转；</li>
 *     <li>{@link #isTerminal()} 为 true 表示生命周期已结束，不应再接收前端调用；</li>
 *     <li>{@link #isOpened()} 为 true 表示窗口已显示、页面已加载，可以安全交互。</li>
 * </ul>
 *
 * @author A-walking-star
 */
public enum ControllerStatusEnum {

    /** 已创建（构造完成），尚未开始初始化 */
    INSTANCE("已创建", false, false),

    /** 初始化中：创建 WebView、注册事件、显示窗口、加载页面 */
    OPENING("初始化中", false, false),

    /** 已就绪：窗口已显示、页面已加载 */
    OPENED("已打开", true, false),

    /** 关闭中：关闭子窗口、关窗、通知父窗口、销毁桥接器 */
    CLOSING("关闭中", false, false),

    /** 已关闭：窗口已隐藏、桥接器已销毁 */
    CLOSED("已关闭", false, true),

    /** 初始化失败（窗口未正常打开） */
    FAILED("初始化失败", false, true),

    /** 已销毁：bean 已从容器移除，终态 */
    DESTROYED("已销毁", false, true);

    private final String description;

    /** 是否已就绪，可直接执行前端交互 */
    private final boolean opened;

    /** 是否已结束（关闭/失败/销毁） */
    private final boolean terminal;

    ControllerStatusEnum(String description, boolean opened, boolean terminal) {
        this.description = description;
        this.opened = opened;
        this.terminal = terminal;
    }

    /**
     * 状态描述
     * @return 中文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 是否已就绪
     * @return 是否已就绪
     */
    public boolean isOpened() {
        return opened;
    }

    /**
     * 是否已结束（关闭/失败/销毁）
     * @return 是否已结束
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * 是否允许流转到目标状态
     *
     * @param next 目标状态
     * @return 是否允许
     */
    public boolean canTransitionTo(ControllerStatusEnum next) {
        if (next == null) {
            return false;
        }
        if (next == this) {
            return true;
        }
        switch (this) {
            case INSTANCE:
                return next == OPENING || next == CLOSING || next == CLOSED || next == DESTROYED;
            case OPENING:
                return next == OPENED || next == FAILED || next == CLOSING || next == DESTROYED;
            case OPENED:
                return next == CLOSING || next == FAILED || next == DESTROYED;
            case CLOSING:
                return next == CLOSED || next == DESTROYED;
            case CLOSED:
                return next == DESTROYED;
            case FAILED:
                return next == CLOSING || next == CLOSED || next == DESTROYED;
            case DESTROYED:
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return name() + "(" + description + ")";
    }
}
