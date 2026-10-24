package com.arena.gounmal.input

/**
 * ⇧(Shift) 를 "짧은 시간 안에 두 번" 눌렀는지 판별한다.
 *
 * 하드웨어 키보드의 Shift 키와 화면의 ⇧ 버튼이 같은 판별기를 공유해서
 * 두 입력 방식 모두 똑같이 "두 번 누르면 자동 교정" 으로 동작한다.
 */
class ShiftDoubleTapDetector(
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
) {

    private var lastTapAt: Long = NOT_PRESSED

    /**
     * @return true 이면 방금 입력이 "두 번째 누름"이므로 자동 교정을 실행한다.
     */
    fun onShiftPressed(now: Long = System.currentTimeMillis()): Boolean {
        val gap = now - lastTapAt
        return if (lastTapAt != NOT_PRESSED && gap in 0..windowMillis) {
            // 세 번 눌렀을 때 두 번씩 계속 실행되지 않도록 상태를 비운다.
            lastTapAt = NOT_PRESSED
            true
        } else {
            lastTapAt = now
            false
        }
    }

    fun reset() {
        lastTapAt = NOT_PRESSED
    }

    companion object {
        /** 두 번째 누름으로 인정하는 최대 간격. iOS 의 더블 탭과 비슷한 체감. */
        const val DEFAULT_WINDOW_MILLIS = 400L

        private const val NOT_PRESSED = Long.MIN_VALUE
    }
}
