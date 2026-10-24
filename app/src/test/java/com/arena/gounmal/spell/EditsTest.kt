package com.arena.gounmal.spell

import org.junit.Assert.assertEquals
import org.junit.Test

class EditsTest {

    @Test
    fun `여러 편집을 한 번에 적용한다`() {
        val result = Edits.apply(
            text = "몇일 만에 안되요",
            edits = listOf(Edit(0, 2, "며칠"), Edit(6, 9, "안 돼요")),
            caret = 9,
        )
        assertEquals("며칠 만에 안 돼요", result.text)
        assertEquals(10, result.caret)
    }

    @Test
    fun `커서가 편집 구간 안에 있으면 교정된 단어 뒤로 간다`() {
        val result = Edits.apply("몇일", listOf(Edit(0, 2, "며칠")), caret = 1)
        assertEquals("며칠", result.text)
        assertEquals(2, result.caret)
    }

    @Test
    fun `커서가 편집 구간 앞이면 움직이지 않는다`() {
        val result = Edits.apply("몇일 만에", listOf(Edit(3, 5, "안 돼요")), caret = 1)
        assertEquals(1, result.caret)
    }

    @Test
    fun `겹치는 편집은 앞의 것만 적용한다`() {
        val deduped = Edits.dedupe(listOf(Edit(0, 3, "a"), Edit(1, 2, "b")))
        assertEquals(1, deduped.size)
        assertEquals(Edit(0, 3, "a"), deduped.first())
    }

    @Test
    fun `두 텍스트의 차이를 계산한다`() {
        assertEquals(listOf(Edit(0, 2, "며칠")), TextDiff.editsBetween("몇일", "며칠"))
        assertEquals(emptyList<Edit>(), TextDiff.editsBetween("같다", "같다"))
    }

    @Test
    fun `되돌리기에 쓰는 차이 계산이 원문을 복원한다`() {
        val before = "오늘은 몇일 만에 나갔다"
        val after = "오늘은 며칠 만에 나갔다"
        // 되돌리기 = "지금 상태"에서 "이전 상태"로 가는 차이
        val edits = TextDiff.editsBetween(after, before)
        assertEquals(before, Edits.apply(after, edits, after.length).text)
    }
}
