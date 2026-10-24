package com.arena.gounmal.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftDoubleTapDetectorTest {

    @Test
    fun `짧은 간격으로 두 번 누르면 자동 교정`() {
        val detector = ShiftDoubleTapDetector(windowMillis = 400)
        assertFalse(detector.onShiftPressed(1_000))
        assertTrue(detector.onShiftPressed(1_300))
    }

    @Test
    fun `세 번째 누름은 새 시작으로 본다`() {
        val detector = ShiftDoubleTapDetector(windowMillis = 400)
        detector.onShiftPressed(1_000)
        assertTrue(detector.onShiftPressed(1_200))
        assertFalse(detector.onShiftPressed(1_400))
        assertTrue(detector.onShiftPressed(1_500))
    }

    @Test
    fun `간격이 너무 길면 두 번으로 보지 않는다`() {
        val detector = ShiftDoubleTapDetector(windowMillis = 400)
        detector.onShiftPressed(1_000)
        assertFalse(detector.onShiftPressed(2_000))
    }
}
