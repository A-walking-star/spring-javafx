package com.cc3.springjavafx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字符串工具（框架内多处用于判空与路径处理）。
 */
class StringUtilsTest {

    @Test
    void blankChecks() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank("  \t\n"));
        assertFalse(StringUtils.isBlank("a"));
        assertTrue(StringUtils.isNotBlank(" a "));
    }

    @Test
    void cleanPathNormalizesSeparators() {
        assertEquals("a/b", StringUtils.cleanPath("a\\b"));
        assertEquals("a/b", StringUtils.cleanPath("a/./b"));
        assertEquals("b", StringUtils.cleanPath("a/../b"));
        assertEquals("..", StringUtils.cleanPath("a/../.."));
    }

    @Test
    void filenameHelpers() {
        assertEquals("c.html", StringUtils.getFilename("web/c.html"));
        assertNull(StringUtils.getFilename(null));
        assertEquals("html", StringUtils.getFilenameExtension("web/c.html"));
        assertEquals("web/c", StringUtils.stripFilenameExtension("web/c.html"));
    }
}
