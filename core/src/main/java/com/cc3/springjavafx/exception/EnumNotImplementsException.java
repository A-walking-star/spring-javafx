package com.cc3.springjavafx.exception;

/**
 * 枚举未实现处理逻辑异常
 * @author A-walking-star
 */
public final class EnumNotImplementsException extends RuntimeException {
    private static final long serialVersionUID = 8247611176171014183L;

    public EnumNotImplementsException() {
        super("未实现该枚举处理逻辑");
    }

    public EnumNotImplementsException(String message) {
        super(message);
    }

    public EnumNotImplementsException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnumNotImplementsException(Throwable cause) {
        super(cause);
    }
}
