package com.cc3.springjavafx.web.dialog;

import com.cc3.springjavafx.controller.BaseController;
import com.cc3.springjavafx.util.SpringUtils;
import com.cc3.springjavafx.util.StringUtils;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 对话框管理器：框架级 WebEngine 弹窗的统一入口与处理器解析中心。
 *
 * <p>职责：</p>
 * <ol>
 *     <li>解析生效的 {@link WebDialogHandler}（配置指定 → 自定义实现 → 内置实现）；</li>
 *     <li>补齐标题、按钮文案、所属窗口等上下文，交给处理器执行；</li>
 *     <li>处理器异常/不支持的场景自动回退内置实现，保证前端不会卡在"弹窗不出来"；</li>
 *     <li>统一日志与开关控制（{@code app.dialog.enabled}）。</li>
 * </ol>
 *
 * <p>框架已在 {@code BaseController#initWebEngine} 中接入 {@code alert / confirm / prompt / JS 错误}，
 * 业务代码也可以直接注入本类显示自定义级别的对话框：</p>
 *
 * <pre><code>
 * &#64;Autowired
 * private WebDialogManager dialogManager;
 *
 * boolean ok = dialogManager.confirm(controller, "确定要删除吗？");
 * dialogManager.show(controller, WebDialogRequest.builder(WebDialogType.ALERT)
 *         .level(WebDialogLevel.ERROR)
 *         .title("导入失败")
 *         .message("第 3 行格式不正确")
 *         .build());
 * </code></pre>
 *
 * @author A-walking-star
 */
@Component
public class WebDialogManager {

    private static final Logger logger = LoggerFactory.getLogger(WebDialogManager.class);

    private final WebDialogProperties properties;

    /** 全部处理器：bean 名称 -> 实现 */
    private final Map<String, WebDialogHandler> handlers;

    /** 内置实现，用于兜底 */
    private final WebDialogHandler defaultHandler;

    /** 最终生效的处理器 */
    private final WebDialogHandler handler;

    private final AtomicBoolean disabledWarned = new AtomicBoolean(false);

    public WebDialogManager(WebDialogProperties properties, Map<String, WebDialogHandler> handlers) {
        this.properties = properties;
        this.handlers = handlers;
        this.defaultHandler = handlers.get(DefaultWebDialogHandler.BEAN_NAME);
        this.handler = resolveHandler();
        logger.info("Web 对话框处理器：{}（容器内共 {} 个实现：{}）",
                handler.getName(), handlers.size(), handlers.keySet());
    }

    // ---------------------------- 供 BaseController 调用的原生事件入口 ----------------------------

    /**
     * 提示框，对应 {@code window.alert(msg)}
     * @param owner 所属窗口
     * @param message 提示内容
     */
    public void alert(BaseController owner, String message) {
        show(owner, WebDialogRequest.alert(message));
    }

    /**
     * 确认框，对应 {@code window.confirm(msg)}
     * @param owner 所属窗口
     * @param message 确认内容
     * @return 是否点击了确定
     */
    public boolean confirm(BaseController owner, String message) {
        return show(owner, WebDialogRequest.confirm(message)).isConfirmed();
    }

    /**
     * 输入框，对应 {@code window.prompt(msg, defaultValue)}
     * @param owner 所属窗口
     * @param message 提示内容
     * @param defaultValue 默认值
     * @return 输入内容，取消返回 null
     */
    public String prompt(BaseController owner, String message, String defaultValue) {
        WebDialogResult result = show(owner, WebDialogRequest.prompt(message, defaultValue));
        return result.isConfirmed() ? result.getValue() : null;
    }

    /**
     * JS 执行错误。默认只记日志；开启 {@code app.dialog.jsErrorDialog} 后额外弹错误框。
     * @param owner 所属窗口
     * @param message 错误信息
     */
    public void jsError(BaseController owner, String message) {
        logger.error("[{}] JS 执行错误：{}", sourceIdOf(owner), message);
        if (properties.isJsErrorDialog()) {
            show(owner, WebDialogRequest.builder(WebDialogType.ALERT)
                    .level(WebDialogLevel.ERROR)
                    .title("脚本错误")
                    .message(message)
                    .build());
        }
    }

    // ---------------------------- 通用入口 ----------------------------

    /**
     * 显示任意对话框（Java 侧自定义标题/级别时使用）
     *
     * @param owner 所属窗口，用于居中与模态
     * @param request 请求
     * @return 结果，永不返回 null
     */
    public WebDialogResult show(BaseController owner, WebDialogRequest request) {
        if (request == null) {
            return WebDialogResult.cancel();
        }
        WebDialogRequest normalized = normalize(owner, request);
        if (!properties.isEnabled()) {
            if (disabledWarned.compareAndSet(false, true)) {
                logger.warn("对话框组件已关闭（app.dialog.enabled=false），后续弹窗只记录日志");
            }
            logger.info("[{}] {}：{}", normalized.getSourceId(), normalized.getType(), normalized.getMessage());
            return WebDialogResult.cancel();
        }

        WebDialogResult result = invoke(handler, normalized);
        if (result != null) {
            return result;
        }
        // 处理器不支持/抛异常/返回 null → 回退内置实现，避免前端"点了没反应"
        if (handler != defaultHandler && defaultHandler != null) {
            logger.warn("回退到内置对话框处理器：{}", defaultHandler.getName());
            WebDialogResult fallback = invoke(defaultHandler, normalized);
            if (fallback != null) {
                return fallback;
            }
        }
        return WebDialogResult.cancel();
    }

    /**
     * 当前生效的处理器
     * @return 处理器
     */
    public WebDialogHandler getHandler() {
        return handler;
    }

    /**
     * 内置处理器
     * @return 内置处理器，可能为 null（容器中缺失时）
     */
    public WebDialogHandler getDefaultHandler() {
        return defaultHandler;
    }

    // ---------------------------- 内部实现 ----------------------------

    /**
     * 调用处理器；不支持或异常时返回 null 表示"需要回退"
     */
    private WebDialogResult invoke(WebDialogHandler target, WebDialogRequest request) {
        if (!target.supports(request.getType())) {
            logger.warn("处理器 {} 不支持 {}，交由内置实现处理", target.getName(), request.getType());
            return null;
        }
        try {
            WebDialogResult result = target.show(request);
            if (result == null) {
                logger.warn("处理器 {} 返回 null，按取消处理", target.getName());
                return WebDialogResult.cancel();
            }
            return result;
        } catch (Throwable e) {
            logger.error("处理器 {} 执行失败，窗口：{}", target.getName(), request.getSourceId(), e);
            return null;
        }
    }

    /**
     * 补齐标题、按钮文案、所属窗口等上下文
     */
    private WebDialogRequest normalize(BaseController owner, WebDialogRequest request) {
        WebDialogRequest.Builder builder = request.toBuilder();
        if (StringUtils.isBlank(request.getTitle())) {
            builder.title(StringUtils.isNotBlank(properties.getTitle()) ? properties.getTitle() : "提示");
        }
        if (StringUtils.isBlank(request.getConfirmText())) {
            builder.confirmText(properties.getConfirmText());
        }
        if (StringUtils.isBlank(request.getCancelText())) {
            builder.cancelText(properties.getCancelText());
        }
        Window ownerWindow = request.getOwner();
        if (ownerWindow == null && owner != null) {
            ownerWindow = owner.getStage();
        }
        builder.owner(ownerWindow);
        if (StringUtils.isBlank(request.getSourceId())) {
            builder.sourceId(sourceIdOf(owner));
        }
        return builder.build();
    }

    private String sourceIdOf(BaseController owner) {
        if (owner == null || owner.getKey() == null) {
            return "main";
        }
        return owner.getKey();
    }

    /**
     * 解析生效的处理器：配置指定 → 自定义实现 → 内置实现
     */
    private WebDialogHandler resolveHandler() {
        String configured = properties.getHandler();
        if (StringUtils.isNotBlank(configured)) {
            WebDialogHandler found = findHandler(configured.trim());
            if (found != null) {
                logger.info("按配置 app.dialog.handler={} 使用对话框处理器：{}", configured, found.getName());
                return found;
            }
            logger.error("配置 app.dialog.handler={} 未匹配到 WebDialogHandler 实现，改用自动选择", configured);
        }

        List<Map.Entry<String, WebDialogHandler>> customs = new ArrayList<>();
        for (Map.Entry<String, WebDialogHandler> entry : handlers.entrySet()) {
            if (!DefaultWebDialogHandler.BEAN_NAME.equals(entry.getKey())) {
                customs.add(entry);
            }
        }
        if (customs.isEmpty()) {
            return defaultOrDefaultInstance();
        }
        customs.sort(Comparator
                .<Map.Entry<String, WebDialogHandler>>comparingInt(entry -> orderOf(entry.getValue()))
                .thenComparing(Map.Entry::getKey));
        if (customs.size() > 1) {
            List<String> names = new ArrayList<>(customs.size());
            for (Map.Entry<String, WebDialogHandler> entry : customs) {
                names.add(entry.getKey());
            }
            logger.warn("检测到多个自定义对话框实现 {}，自动使用 {}；可用 app.dialog.handler 明确指定",
                    names, customs.get(0).getKey());
        }
        return customs.get(0).getValue();
    }

    /**
     * 按 bean 名称或全类名查找处理器
     */
    private WebDialogHandler findHandler(String name) {
        WebDialogHandler byBeanName = handlers.get(name);
        if (byBeanName != null) {
            return byBeanName;
        }
        for (WebDialogHandler candidate : handlers.values()) {
            if (candidate.getClass().getName().equals(name)) {
                return candidate;
            }
        }
        try {
            if (SpringUtils.containsBean(name)) {
                Object bean = SpringUtils.getBean(name);
                if (bean instanceof WebDialogHandler) {
                    return (WebDialogHandler) bean;
                }
            }
            Object bean = SpringUtils.getBean(Class.forName(name));
            if (bean instanceof WebDialogHandler) {
                return (WebDialogHandler) bean;
            }
        } catch (Throwable e) {
            logger.debug("按名称 {} 查找对话框处理器失败：{}", name, e.getMessage());
        }
        return null;
    }

    private WebDialogHandler defaultOrDefaultInstance() {
        if (defaultHandler != null) {
            return defaultHandler;
        }
        logger.warn("容器中未注册内置对话框处理器 {}，已创建独立实例兜底", DefaultWebDialogHandler.BEAN_NAME);
        return new DefaultWebDialogHandler(properties);
    }

    private int orderOf(WebDialogHandler candidate) {
        return candidate instanceof Ordered ? ((Ordered) candidate).getOrder() : Ordered.LOWEST_PRECEDENCE;
    }
}
