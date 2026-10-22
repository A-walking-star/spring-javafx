package com.cc3.springjavafx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 回调脚本生成：参数编码与转义。
 *
 * <p>只测纯函数，不触碰 Spring 容器与 JavaFX 工具包，因此可以在无界面环境/CI 里直接跑。</p>
 */
class ParamsUtilsTest {

    @Test
    void stringParamIsQuotedAndEscaped() throws Exception {
        assertEquals("!!window['f'] && window['f']('a\\'b\\nc')",
                ParamsUtils.buildCallbackScript("f", "a'b\nc"));
    }

    @Test
    void booleanAndNumberStayNative() throws Exception {
        assertEquals("!!window['f'] && window['f'](true)",
                ParamsUtils.buildCallbackScript("f", Boolean.TRUE));
        assertEquals("!!window['f'] && window['f'](1.5)",
                ParamsUtils.buildCallbackScript("f", 1.5d));
        assertEquals("!!window['f'] && window['f'](42)",
                ParamsUtils.buildCallbackScript("f", 42));
    }

    @Test
    void nullParamBecomesJsNull() throws Exception {
        assertEquals("!!window['f'] && window['f'](null)",
                ParamsUtils.buildCallbackScript("f", (Object) null));
    }

    @Test
    void paramsKeepOrder() throws Exception {
        assertEquals("!!window['f'] && window['f']('k',1,false)",
                ParamsUtils.buildCallbackScript("f", "k", 1, false));
    }

    @Test
    void noParams() throws Exception {
        assertEquals("!!window['f'] && window['f']()", ParamsUtils.buildCallbackScript("f"));
    }

    @Test
    void funcNameIsEscaped() throws Exception {
        assertEquals("!!window['a\\'b'] && window['a\\'b']()",
                ParamsUtils.buildCallbackScript("a'b"));
    }

    @Test
    void lineSeparatorAndControlCharsAreEscaped() {
        assertEquals("a\\u2028b", ParamsUtils.escapeJsString("a\u2028b"));
        assertEquals("a\\u0001b", ParamsUtils.escapeJsString("a\u0001b"));
    }

    @Test
    void nullStringEscapesToEmpty() {
        assertEquals("", ParamsUtils.escapeJsString(null));
    }

    @Test
    void backslashIsEscaped() {
        assertEquals("C:\\\\tmp\\\\a", ParamsUtils.escapeJsString("C:\\tmp\\a"));
    }

    @Test
    void nullParamsParseToNull() throws Exception {
        assertNull(ParamsUtils.parse(null, String.class));
    }
}
