package com.arena.gounmal.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HangulComposerTest {

    private fun typeAll(jamos: String, text: String = "", caret: Int = 0): HangulComposer.Result {
        val composer = HangulComposer()
        var current = text
        var position = caret
        jamos.forEach { jamo ->
            val result = composer.type(current, position, position, jamo)
            current = result.text
            position = result.caret
        }
        return HangulComposer.Result(current, position)
    }

    @Test
    fun `자모를 모아 글자를 만든다`() {
        assertEquals("가나", typeAll("ㄱㅏㄴㅏ").text)
        assertEquals("한글", typeAll("ㅎㅏㄴㄱㅡㄹ").text)
        assertEquals("읽어", typeAll("ㅇㅣㄹㄱㅇㅓ").text)
        assertEquals("있어", typeAll("ㅇㅣㅆㅇㅓ").text)
        assertEquals("가나다", typeAll("ㄱㅏㄴㅏㄷㅏ").text)
    }

    @Test
    fun `복합 모음과 복합 받침`() {
        assertEquals("과", typeAll("ㄱㅗㅏ").text)
        assertEquals("관", typeAll("ㄱㅗㅏㄴ").text)
        assertEquals("왜", typeAll("ㅇㅗㅐ").text)
        assertEquals("의", typeAll("ㅇㅡㅣ").text)
        assertEquals("값", typeAll("ㄱㅏㅂㅅ").text)
        assertEquals("뷁", typeAll("ㅂㅜㅔㄹㄱ").text)
    }

    @Test
    fun `종성이 다음 글자의 초성으로 넘어간다`() {
        // 간 + ㅏ → 가나
        assertEquals("가나", typeAll("ㄱㅏㄴㅏ").text)
        // 값 + ㅇ + ㅓ → 값어
        assertEquals("값어", typeAll("ㄱㅏㅂㅅㅇㅓ").text)
        assertEquals("가나", typeAll("ㄱㅏㄴㅏ").text)
    }

    @Test
    fun `커서는 마지막 글자 뒤에 있다`() {
        assertEquals(1, typeAll("ㄱㅏㄴ").caret)
    }

    @Test
    fun `기존 글자 뒤에 이어서 쓴다`() {
        assertEquals("밥 ㅏ", typeAll("ㅏ", text = "밥 ", caret = 3).text)
    }

    @Test
    fun `선택 영역을 지우고 새 글자를 쓴다`() {
        val composer = HangulComposer()
        val result = composer.type("가나다", 1, 2, 'ㅁ')
        assertEquals("가ㅁ다", result.text)
    }

    @Test
    fun `조합 중인 자모를 하나씩 지운다`() {
        val composer = HangulComposer()
        composer.type("", 0, 0, 'ㄱ')
        val ga = composer.type("ㄱ", 1, 1, 'ㅏ')
        val gan = composer.type(ga.text, ga.caret, ga.caret, 'ㄴ')
        assertEquals("간", gan.text)

        val toGa = composer.backspace(gan.text, gan.caret, gan.caret)
        assertNotNull(toGa)
        assertEquals("가", toGa?.text)

        val toG = composer.backspace(toGa!!.text, toGa.caret, toGa.caret)
        assertEquals("ㄱ", toG?.text)

        val toEmpty = composer.backspace(toG!!.text, toG.caret, toG.caret)
        assertEquals("", toEmpty?.text)
        assertNull(composer.backspace("", 0, 0))
    }
}
