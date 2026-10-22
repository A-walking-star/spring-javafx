package com.cc3.springjavafx.exception;

/**
 * 工具调用异常
 * @author A-walking-star
 */
public class UtilException extends RuntimeException {
    private static final long serialVersionUID = 8511610510869120991L;

    public UtilException() {
        super("工具调用失败！");
    }

    public UtilException(String message) {
        super(message);
    }

    public UtilException(String message, Throwable cause) {
        super(message, cause);
    }

    public UtilException(Throwable cause) {
        super(cause);
    }
}
