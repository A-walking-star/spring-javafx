package com.cc3.springjavafx.constant;

/**
 * @author A-walking-star
 */
public class AppConstant {

    /**
     * web资源目录
     */
    public static final String WEB_RESOURCE = "web/";

    /**
     * 前端入口
     */
    public static final String INDEX_HTML_RESOURCE = WEB_RESOURCE + "index.html";

    /**
     * 前端全局controllerKey名称
     */
    public static final String WEB_GLOBAL_CONTROLLER_ID_NAME = "$controllerKey";

    /**
     * 前端加载完成回调方法名
     */
    public static final String WEB_READY_CALLBACK = "$$_apiReady";

    /**
     * 主stage名称
     */
    public static final String PRIMARY_STAGE = "__primaryStage";

    /**
     * 主窗口控制器 bean 名称
     */
    public static final String MAIN_CONTROLLER = "__mainController";

    /**
     * controllerKey前缀
     */
    public static final String CONTROLLER_KEY_PREFIX = "__controllerKey:";

    /**
     * 前端全局父窗口controllerKey名称
     */
    public static final String WEB_GLOBAL_PARENT_CONTROLLER_ID_NAME = "$parentControllerKey";

    /**
     * 子窗口就绪回调方法名（父窗口接收通知）
     */
    public static final String WEB_CHILD_READY_CALLBACK = "$$_onChildReady";

    /**
     * 子窗口关闭回调方法名（父窗口接收通知）
     */
    public static final String WEB_CHILD_CLOSED_CALLBACK = "$$_onChildClosed";
}