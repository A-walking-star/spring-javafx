package com.cc3.springjavafx.api;

import com.cc3.springjavafx.util.SpringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WindowApi} 未绑定窗口时的空安全行为。
 *
 * <p>这些场景在真实应用中很常见：页面重新加载、窗口刚关闭但前端仍有一次未完成的调用、
 * Spring 容器尚未就绪等。以前 {@code getWidth()} 会因 {@code Double} 自动拆箱抛 NPE，
 * 这里把安全默认值锁住，防止回归。</p>
 *
 * <p>测试不启动 JavaFX 工具包（只读取未绑定的桥接器），因此可在无界面环境/CI 运行。</p>
 */
class WindowApiTest {

    private final WindowApi api = new WindowApi();

    /** 保证不残留其它测试的 Spring 容器，使"无窗口"场景可重复 */
    @BeforeEach
    void clearSpringContext() {
        SpringUtils.clear();
    }

    @Test
    void unboundWindowReturnsSafeDefaults() {
        assertNull(api.getStage());
        assertNull(api.getKey());
        assertNull(api.getTitle());
        assertNull(api.getParentKey());
        assertTrue(api.isMainWindow());
        assertFalse(api.isOpened());
        assertFalse(api.isShowing());
        assertFalse(api.isIconified());
        assertFalse(api.isMaximized());
        assertFalse(api.isFullScreen());
        assertFalse(api.isAlwaysOnTop());
        assertFalse(api.getResizable());

        assertEquals(0d, api.getWidth());
        assertEquals(0d, api.getHeight());
        assertEquals(0d, api.getMinWidth());
        assertEquals(0d, api.getMinHeight());
        assertEquals(0d, api.getX());
        assertEquals(0d, api.getY());
        assertEquals(1d, api.getOpacity());
        assertEquals("DESTROYED(已销毁)", api.getStatus());

        assertArrayEquals(new String[0], api.getChildKeys());
        assertArrayEquals(new String[0], api.getAllKeys());
        assertArrayEquals(new String[0], api.getChildKeysByControllerKey(null));
        assertEquals(0, api.getWindowCount());
        assertEquals(0, api.closeChildWindows());

        assertFalse(api.hasWindow(null));
        assertNull(api.getStatusByControllerKey(null));
        assertFalse(api.isOpenedByControllerKey(null));
        assertNull(api.getWidthByControllerKey(null));
        assertNull(api.isShowingByControllerKey(null));
    }

    @Test
    void settersOnUnboundWindowAreNoOpsAndNeverThrow() {
        assertDoesNotThrow(() -> {
            api.setTitle("ignored");
            api.setWidth(Double.NaN);
            api.setHeight(Double.POSITIVE_INFINITY);
            api.setSize(-1, 0);
            api.setMinWidth(-1);
            api.setMinHeight(Double.NaN);
            api.setMinSize(-1, -1);
            api.setResizable(true);
            api.setX(Double.NaN);
            api.setY(Double.NEGATIVE_INFINITY);
            api.setPosition(Double.NaN, 0);
            api.setOpacity(2.5);
            api.minimizeWindow();
            api.hiddenWindow();
            api.restoreWindow();
            api.showWindow();
            api.setMaximized(true);
            api.toggleMaximized();
            api.setFullScreen(true);
            api.toggleFullScreen();
            api.setAlwaysOnTop(true);
            api.toFront();
            api.centerOnScreen();
            api.requestCloseWindow();
            api.closeWindow();
            api.hiddenWindowByControllerKey(null);
            api.closeWindowByControllerKey(null);
        });

        // 所有写操作都未生效，读操作仍返回安全默认值
        assertEquals(0d, api.getWidth());
        assertEquals(1d, api.getOpacity());
    }

    @Test
    void byControllerKeySettersReportMissingWindow() {
        assertFalse(api.setTitleByControllerKey(null, "ignored"));
        assertFalse(api.setWidthByControllerKey(null, 100));
        assertFalse(api.setHeightByControllerKey(null, 100));
        assertFalse(api.setSizeByControllerKey(null, 100, 100));
        assertFalse(api.setMinWidthByControllerKey(null, 10));
        assertFalse(api.setMinHeightByControllerKey(null, 10));
        assertFalse(api.setMinSizeByControllerKey(null, 10, 10));
        assertFalse(api.setResizableByControllerKey(null, true));
        assertFalse(api.setXByControllerKey(null, 10));
        assertFalse(api.setYByControllerKey(null, 10));
        assertFalse(api.setPositionByControllerKey(null, 10, 10));
        assertFalse(api.centerOnScreenByControllerKey(null));
        assertFalse(api.setOpacityByControllerKey(null, 0.5));
        assertFalse(api.minimizeWindowByControllerKey(null));
        assertFalse(api.restoreWindowByControllerKey(null));
        assertFalse(api.showWindowByControllerKey(null));
        assertFalse(api.setMaximizedByControllerKey(null, true));
        assertFalse(api.setFullScreenByControllerKey(null, true));
        assertFalse(api.setAlwaysOnTopByControllerKey(null, true));
        assertFalse(api.toFrontByControllerKey(null));
        assertFalse(api.executeJsByControllerKey(null, "1 + 1"));
    }
}
