package com.arena.gounmal.spell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PusanResponseParserTest {

    private val html = """
        <!DOCTYPE html>
        <html>
        <head>
        <script type="text/javascript">
        $(document).ready(function () { data = [{"str":"한국어 맞춤법 틀리면 어떡해 되나요?","errInfo":[
          {"errorIdx":0,"correctMethod":2,"orgStr":"되나요","candWord":"되나요|돼나요","help":"‘되어’의 준말은 ‘돼’입니다.","start":14,"end":17}
        ]}]; });
        </script>
        </head>
        </html>
    """.trimIndent()

    @Test
    fun `응답에서 교정 목록을 읽는다`() {
        val issues = PusanResponseParser.parse(html)
        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("되나요", issue.original)
        assertEquals("돼나요", issue.primarySuggestion)
        assertEquals(14, issue.start)
        assertEquals(17, issue.end)
        assertEquals(IssueSource.ONLINE, issue.source)
    }

    @Test
    fun `문자열 안의 대괄호에 흔들리지 않는다`() {
        val tricky = """<script>data = [{"str":"[대괄호] 들어간 문장","errInfo":[]}];</script>"""
        val json = PusanResponseParser.extractJson(tricky)
        assertEquals("""[{"str":"[대괄호] 들어간 문장","errInfo":[]}]""", json)
        assertTrue(PusanResponseParser.parse(tricky).isEmpty())
    }

    @Test
    fun `응답 형식이 다르면 조용히 빈 목록을 준다`() {
        assertNull(PusanResponseParser.extractJson("<html>검사 실패</html>"))
        assertTrue(PusanResponseParser.parse("<html>없음</html>").isEmpty())
    }

    @Test
    fun `offset 을 더해 원문 위치로 되돌린다`() {
        val issues = PusanResponseParser.parse(html, baseOffset = 100)
        assertEquals(114, issues.first().start)
        assertEquals(117, issues.first().end)
    }
}
