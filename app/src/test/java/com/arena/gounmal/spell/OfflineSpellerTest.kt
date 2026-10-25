package com.arena.gounmal.spell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSpellerTest {

    private val speller = OfflineSpeller()

    private fun first(text: String): Issue? = speller.check(text).firstOrNull()

    @Test
    fun `몇일은 며칠로 고친다`() {
        val issue = first("몇일 만에 만났다")
        assertEquals("몇일", issue?.original)
        assertEquals("며칠", issue?.primarySuggestion)
        assertEquals(0, issue?.start)
        assertEquals(2, issue?.end)
    }

    @Test
    fun `안되요는 안 돼요로 고친다`() {
        assertEquals("안 돼요", first("그건 안되요")?.primarySuggestion)
    }

    @Test
    fun `ㄹ 받침 뒤 의존 명사 수를 띄어 쓴다`() {
        assertEquals("할 수 ", first("나는 할수있다")?.primarySuggestion)
        assertEquals("럴 줄 ", first("그럴줄알았다")?.primarySuggestion)
    }

    @Test
    fun `ㄹ 받침 뒤 거를 띄어 쓴다`() {
        assertEquals("할 거", first("내가 할거야")?.primarySuggestion)
    }

    @Test
    fun `문장 부호 뒤를 띄어 쓴다`() {
        assertEquals("! ", first("안녕하세요!오늘은")?.primarySuggestion)
    }

    @Test
    fun `연속 공백을 하나로 줄인다`() {
        assertEquals(" ", first("밥을  먹었다")?.primarySuggestion)
    }

    @Test
    fun `할께는 할게로 고친다`() {
        assertEquals("할게", first("내가 할께")?.primarySuggestion)
    }

    @Test
    fun `바른 문장은 건드리지 않는다`() {
        assertTrue(speller.check("오늘은 날씨가 좋습니다.").isEmpty())
        assertTrue(speller.check("그럴 수 있다.").isEmpty())
        assertTrue(speller.check("별수없이 그랬다.").isEmpty())
        assertTrue(speller.check("3.14는 원주율이다.").isEmpty())
    }

    @Test
    fun `오류 구간은 서로 겹치지 않는다`() {
        val issues = speller.check(EditorSample.TEXT)
        issues.zipWithNext().forEach { (a, b) ->
            assertTrue("겹침: $a / $b", a.end <= b.start)
        }
    }

    @Test
    fun `교정 후에는 같은 오류가 사라진다`() {
        val original = "몇일 만에 만난 안되요"
        val issues = speller.check(original)
        val fixed = Edits.apply(original, issues.map { Edit.of(it) }, original.length).text
        assertEquals("며칠 만에 만난 안 돼요", fixed)
        assertTrue(speller.check(fixed).isEmpty())
    }
}

/** 테스트에서 함께 쓰는 예시 문장. */
object EditorSample {
    const val TEXT = "안녕하세요! 오늘은 몇일 만에 사무실에 나갔어요. " +
        "아침부터 정신이 없어서 인사도 제대로 못했는데, 웬지 오늘은 운이 좋은것같아요. " +
        "내일은 꼭 일찍 일어나서 숙제를 끝낼께요. 그래도 지각은 안되요."
}
