package com.cc3.springjavafx.util;

import com.cc3.springjavafx.model.BaseEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cc3.springjavafx.controller.BaseController;
import com.cc3.springjavafx.model.dto.BaseDto;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 参数解析工具
 * @author A-walking-star
 */
public class ParamsUtils {
    private static final String NULL_STR = "null";
    private static final String UNDEFINED_STR = "undefined";
    private static final Logger logger = LoggerFactory.getLogger(ParamsUtils.class);

    private static volatile ObjectMapper mapper;

    /**
     * 获取Spring管理的ObjectMapper（懒加载，避免Spring未初始化时获取）
     *
     * <p>双重检查 + volatile：回调可能从任意线程发起，避免并发首次调用时返回半初始化实例。</p>
     */
    private static ObjectMapper getMapper() {
        ObjectMapper local = mapper;
        if (local != null) {
            return local;
        }
        synchronized (ParamsUtils.class) {
            if (mapper == null) {
                mapper = SpringUtils.getBean(ObjectMapper.class);
            }
            return mapper;
        }
    }

    /**
     * 解析字符串
     * @param params 字符串
     * @param clazz 目标类
     */
    public static <T> T parse(String params, Class<T> clazz) throws IOException {
        if (params == null) {
            return null;
        }
        String paramsLowerCase = params.toLowerCase(Locale.ENGLISH);
        if (!clazz.equals(String.class) && (NULL_STR.equals(paramsLowerCase) || UNDEFINED_STR.equals(paramsLowerCase))) {
            return null;
        }
        return getMapper().readValue(params, clazz);
    }

    /**
     * 解析字符串
     * @param params 字符串
     */
    public static <T> T parse(String params, TypeReference<T> typeReference) throws IOException {
        if (params == null) {
            return null;
        }
        String paramsLowerCase = params.toLowerCase(Locale.ENGLISH);
        if ((NULL_STR.equals(paramsLowerCase) || UNDEFINED_STR.equals(paramsLowerCase))) {
            return null;
        }
        return getMapper().readValue(params, typeReference);
    }

    /**
     * 序列化
     * @param obj
     */
    public static String serialize(Object obj) throws IOException {
        return getMapper().writeValueAsString(obj);
    }


    /**
     * 触发回调函数
     * @param func 前端方法名
     * @param params 方法参数
     */
    public static void onCallBack(String controllerKey, String func, Object... params) throws JsonProcessingException {
        BaseController controller = ContextUtils.getController(controllerKey);
        if (controller != null) {
            onCallBack(controller, func, params);
        } else {
            logger.error("窗口id不存在，Id：{}", controllerKey);
        }
    }

    /**
     * 触发回调函数
     * @param func 前端方法名
     * @param params 方法参数
     */
    public static void onCallBack(BaseController controller, String func, Object... params) throws JsonProcessingException {
        if (controller == null) {
            logger.error("窗口不存在");
            return;
        }
        final String script = buildCallbackScript(func, params);
        Platform.runLater(() -> {
            try {
                javafx.scene.web.WebEngine engine = controller.getWebEngine();
                if (engine == null) {
                    logger.warn("窗口 [{}] 的 WebEngine 尚未就绪，回调 {} 被丢弃", controller.getKey(), func);
                    return;
                }
                engine.executeScript(script);
            } catch (Throwable e) {
                logger.warn("回调前端方法 {} 失败：{}", func, e.toString());
            }
        });
    }

    /**
     * 生成「调用前端方法」的脚本。
     *
     * <p><b>参数编码规则</b>：{@code null} → {@code null}；{@code String} → 单引号字符串（已转义）；
     * {@code Boolean} / {@code Number} → 原生字面量（此前一律被转成 {@code 'true'} / {@code '1.5'}
     * 这样的字符串，前端拿到的类型是错的）；其他对象 → JSON 字符串（用单引号包裹，前端需自行
     * {@code JSON.parse}，与旧版本行为一致）。</p>
     *
     * @param func   前端方法名
     * @param params 方法参数
     * @return 可执行脚本
     */
    static String buildCallbackScript(String func, Object... params) throws JsonProcessingException {
        String safeFunc = escapeJsString(func == null ? "" : func);
        StringBuilder script = new StringBuilder("!!window['").append(safeFunc)
                .append("'] && window['").append(safeFunc).append("'](");
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    script.append(',');
                }
                appendParam(script, params[i]);
            }
        }
        return script.append(')').toString();
    }

    /**
     * 追加单个参数
     */
    private static void appendParam(StringBuilder script, Object param) throws JsonProcessingException {
        if (param == null) {
            script.append("null");
        } else if (param instanceof String) {
            script.append('\'').append(escapeJsString((String) param)).append('\'');
        } else if (param instanceof Boolean || param instanceof Number) {
            script.append(param);
        } else {
            script.append('\'').append(escapeJsString(getMapper().writeValueAsString(param))).append('\'');
        }
    }

    public static <E extends BaseEntity, D extends BaseDto> List<D> toDto(List<E> data) {
        return (List<D>) data.stream().map(BaseEntity::buildDto).collect(Collectors.toList());
    }

    public static <E extends BaseEntity, D extends BaseDto> D toDto(E data) {
        return (D) data.buildDto();
    }

    /**
     * 转义字符串中的特殊字符，使其可以安全嵌入 JS 单引号字符串字面量
     *
     * <p>除引号/反斜杠/换行外，还要处理 {@code U+2028}/{@code U+2029} 与其它控制字符：
     * 它们在 JS 字符串字面量里属于换行符或非法字符，会让 {@code executeScript} 语法报错。</p>
     */
    static String escapeJsString(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\u2028':
                    sb.append("\\u2028");
                    break;
                case '\u2029':
                    sb.append("\\u2029");
                    break;
                default:
                    if (c < 0x20 || c == 0x7f) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}