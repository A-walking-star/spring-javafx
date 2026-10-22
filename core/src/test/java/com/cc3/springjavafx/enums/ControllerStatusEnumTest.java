package com.cc3.springjavafx.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 控制器状态机：合法流转放行、非法流转拒绝、终态不可逆。
 */
class ControllerStatusEnumTest {

    @Test
    void normalLifecycleTransitionsAreAllowed() {
        assertTrue(ControllerStatusEnum.INSTANCE.canTransitionTo(ControllerStatusEnum.OPENING));
        assertTrue(ControllerStatusEnum.OPENING.canTransitionTo(ControllerStatusEnum.OPENED));
        assertTrue(ControllerStatusEnum.OPENED.canTransitionTo(ControllerStatusEnum.CLOSING));
        assertTrue(ControllerStatusEnum.CLOSING.canTransitionTo(ControllerStatusEnum.CLOSED));
        assertTrue(ControllerStatusEnum.CLOSED.canTransitionTo(ControllerStatusEnum.DESTROYED));
    }

    @Test
    void shortcutsAreRejected() {
        assertFalse(ControllerStatusEnum.INSTANCE.canTransitionTo(ControllerStatusEnum.OPENED));
        assertFalse(ControllerStatusEnum.OPENED.canTransitionTo(ControllerStatusEnum.OPENING));
        assertFalse(ControllerStatusEnum.CLOSED.canTransitionTo(ControllerStatusEnum.OPENED));
        // 初始化中窗口仍可被关闭/销毁，这是有意的
        assertTrue(ControllerStatusEnum.OPENING.canTransitionTo(ControllerStatusEnum.CLOSING));
    }

    @Test
    void destroyedIsTerminal() {
        assertTrue(ControllerStatusEnum.DESTROYED.isTerminal());
        for (ControllerStatusEnum next : ControllerStatusEnum.values()) {
            if (next == ControllerStatusEnum.DESTROYED) {
                // 同状态流转是幂等空操作，允许（close/destroy 可能被重复触发）
                assertTrue(ControllerStatusEnum.DESTROYED.canTransitionTo(next));
                continue;
            }
            assertFalse(ControllerStatusEnum.DESTROYED.canTransitionTo(next),
                    "DESTROYED 不能再流转到 " + next);
        }
    }

    @Test
    void failedCanBeClosedOrDestroyed() {
        assertTrue(ControllerStatusEnum.FAILED.canTransitionTo(ControllerStatusEnum.DESTROYED));
        assertTrue(ControllerStatusEnum.FAILED.isTerminal());
        assertFalse(ControllerStatusEnum.FAILED.isOpened());
    }

    @Test
    void openedMeansReady() {
        assertTrue(ControllerStatusEnum.OPENED.isOpened());
        assertFalse(ControllerStatusEnum.OPENED.isTerminal());
        assertFalse(ControllerStatusEnum.OPENING.isOpened());
    }

    @Test
    void nullTransitionIsRejected() {
        assertFalse(ControllerStatusEnum.OPENED.canTransitionTo(null));
    }
}
