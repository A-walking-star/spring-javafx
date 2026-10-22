package com.cc3.springjavafx.api;

import com.cc3.springjavafx.controller.BaseController;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MarkerIgnoringBase;

/**
 * {@link JavaApiBridge} 的通用实现，负责两件事：
 *
 * <ol>
 *     <li>维护"桥接器 → 窗口控制器"的绑定关系，窗口状态统一从 {@link #controller} 获取，避免写死某个具体窗口；</li>
 *     <li>实现 {@link Logger}：实现类可以直接调用 {@code info(...)}、{@code error(...)} 等方法，
 *     日志实际由 {@link #logger} 输出（按实现类名命名，因此 logback 里可以用
 *     {@code <logger name="...WindowApi">} 单独控制开关）。</li>
 * </ol>
 *
 * @author A-walking-star
 */
public abstract class AbstractJavaApiBridge extends MarkerIgnoringBase implements JavaApiBridge {
    private static final long serialVersionUID = 1L;

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    /** 所属窗口控制器，销毁后置空以解除引用（不参与序列化） */
    protected transient volatile BaseController controller;

    /** 是否已销毁 */
    protected transient volatile boolean destroyed = false;

    // ---------------------------- 窗口绑定 ----------------------------

    @Override
    public void bindController(BaseController controller) {
        if (controller == null) {
            throw new IllegalArgumentException(getNamespace() + " 绑定的窗口控制器不能为空");
        }
        BaseController bound = this.controller;
        if (bound != null && bound != controller) {
            // 同一实例被多个窗口使用，说明该桥接器不是 prototype 作用域，这里拒绝覆盖以免窗口串台
            this.logger.error("JavaApiBridge[{}] 已绑定窗口 [{}]，拒绝重复绑定到窗口 [{}]；" +
                            "请确认该实现类是否标注了 @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)",
                    getNamespace(), bound.getKey(), controller.getKey());
            return;
        }
        this.controller = controller;
        this.logger = controller.getLogger();
        this.destroyed = false;
    }

    /**
     * 获取所属窗口的 Stage
     * @return Stage，未绑定或已销毁时返回 null
     */
    public Stage getStage() {
        BaseController bound = controller;
        return bound != null ? bound.getStage() : null;
    }

    /**
     * 是否已销毁
     * @return 是否已销毁
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        BaseController bound = controller;
        logger.debug("JavaApiBridge[{}] 销毁！窗口id：{}", getNamespace(), bound == null ? "未绑定" : bound.getKey());
        destroyed = true;
        // 解除对窗口控制器的引用，避免已关闭窗口被过期桥接器持有
        controller = null;
    }

    // ---------------------------- Logger 实现 ----------------------------

    @Override
    public String getName() {
        return this.logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        this.logger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        this.logger.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        this.logger.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        this.logger.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        this.logger.trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        this.logger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        this.logger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        this.logger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        this.logger.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        this.logger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        this.logger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        this.logger.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        this.logger.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        this.logger.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        this.logger.info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return this.logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        this.logger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        this.logger.warn(format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        this.logger.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        this.logger.warn(format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        this.logger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        this.logger.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        this.logger.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        this.logger.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        this.logger.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        this.logger.error(msg, t);
    }
}