package com.cc3.springjavafx.api;

import com.cc3.springjavafx.controller.BaseController;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 日志api
 *
 * <p>每个窗口持有独立实例，日志会自动带上来源窗口ID，便于区分不同窗口的输出；</p>
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LoggerApi extends AbstractJavaApiBridge {

    public LoggerApi() {}

    @Override
    public void debug(String msg) {
        this.logger.debug(format(msg));
    }

    @Override
    public void info(String msg) {
        this.logger.info(format(msg));
    }

    @Override
    public void warn(String msg) {
        this.logger.warn(format(msg));
    }

    @Override
    public void error(String msg) {
        this.logger.error(format(msg));
    }

    /**
     * 拼接窗口标识，主窗口标记为 main
     * @param msg 日志内容
     * @return 带窗口标识的日志内容
     */
    private String format(String msg) {
        String key = controller != null ? controller.getKey() : null;
        return "[" + (key != null ? key : "main") + "] " + msg;
    }
}
