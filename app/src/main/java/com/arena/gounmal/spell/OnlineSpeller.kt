package com.arena.gounmal.spell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 온라인 정밀 검사.
 *
 * 공개된 한국어 맞춤법/문법 검사기 웹 페이지(부산대 계열, nara-speller)를 그대로 이용한다.
 * 별도의 API 키나 계정이 필요 없어서 "설정 없이 바로" 쓸 수 있다.
 *
 * - 텍스트를 줄 단위로 잘라서 보내므로, 응답의 위치 값을 원문 위치로 정확히 되돌릴 수 있다.
 * - 네트워크가 없거나 서버가 응답하지 않으면 조용히 실패하고 오프라인 결과만 남는다.
 */
class OnlineSpeller(
    private val client: OkHttpClient = defaultClient(),
    private val endpoints: List<String> = DEFAULT_ENDPOINTS,
) {

    data class Result(
        val issues: List<Issue>,
        val chunkCount: Int,
        val error: String? = null,
    ) {
        val isSuccess: Boolean get() = error == null
    }

    suspend fun check(text: String, maxChunks: Int = 6): Result = withContext(Dispatchers.IO) {
        val chunks = chunkUp(text, maxChunks)
        if (chunks.isEmpty()) return@withContext Result(emptyList(), 0, null)

        val issues = ArrayList<Issue>()
        var lastError: String? = null
        var checked = 0

        for ((index, chunk) in chunks.withIndex()) {
            if (index > 0) delay(220)
            try {
                val parsed = checkChunk(chunk.second)
                parsed.forEach { issue ->
                    issues += issue.copy(
                        start = issue.start + chunk.first,
                        end = issue.end + chunk.first,
                    )
                }
                checked++
            } catch (e: Exception) {
                lastError = e.message ?: "온라인 검사에 연결할 수 없습니다."
                break
            }
        }

        Result(
            issues = issues,
            chunkCount = checked,
            error = if (checked == 0) lastError else null,
        )
    }

    private suspend fun checkChunk(chunk: String): List<Issue> {
        var lastFailure: Exception? = null
        for (base in endpoints) {
            val url = base + RESPONSE_PATH
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", DESKTOP_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Referer", base + "/")
                    .post(FormBody.Builder().add("text1", chunk).build())
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastFailure = IllegalStateException("검사 서버 응답 ${response.code}")
                        return@use
                    }
                    val html = response.body?.string().orEmpty()
                    return PusanResponseParser.parse(html)
                }
            } catch (e: Exception) {
                lastFailure = e
            }
        }
        throw lastFailure ?: IllegalStateException("검사 서버에 연결할 수 없습니다.")
    }

    /** 줄 단위로 잘라 서버가 감당할 만한 크기(약 300어절 이하)로 묶는다. */
    private fun chunkUp(text: String, maxChunks: Int): List<Pair<Int, String>> {
        if (text.isBlank()) return emptyList()
        val result = ArrayList<Pair<Int, String>>()
        var start = 0
        val buffer = StringBuilder()
        var bufferStart = -1

        fun flush() {
            if (bufferStart >= 0 && buffer.isNotBlank()) {
                result += bufferStart to buffer.toString()
            }
            buffer.setLength(0)
            bufferStart = -1
        }

        var i = 0
        while (i <= text.length) {
            val atEnd = i == text.length
            val isBreak = atEnd || text[i] == '\n'
            if (isBreak) {
                val line = text.substring(start, i)
                if (line.isNotBlank()) {
                    if (bufferStart < 0) bufferStart = start
                    if (buffer.isNotEmpty()) buffer.append('\n')
                    buffer.append(line)
                    if (buffer.length >= CHUNK_LIMIT) flush()
                }
                start = i + 1
            }
            i++
        }
        flush()

        return result.take(maxChunks)
    }

    companion object {
        private const val CHUNK_LIMIT = 220

        private const val RESPONSE_PATH = "/old_speller/results"

        private val DEFAULT_ENDPOINTS = listOf(
            "https://nara-speller.co.kr",
        )

        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(18, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

/**
 * 검사기 웹 페이지 응답에서 교정 데이터만 뽑아내는 파서.
 *
 * 응답은 대략 이런 모양이다.
 * ```
 * <script>... data = [{"str":"...","errInfo":[{ "orgStr":"되요", "candWord":"돼요",
 *   "help":"...", "start":3, "end":5, ... }]}]; ...</script>
 * ```
 * 형식이 조금씩 달라도(배열/객체, 앞뒤 공백) 동작하도록 균형 잡힌 JSON 덩어리를 찾아낸다.
 */
object PusanResponseParser {

    fun parse(html: String, baseOffset: Int = 0): List<Issue> {
        val json = extractJson(html) ?: return emptyList()
        val root = runCatching { JSONObject(json) }.getOrElse {
            val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
            if (array.length() == 0) return emptyList()
            array.optJSONObject(0) ?: return emptyList()
        }
        val errInfo = root.optJSONArray("errInfo") ?: return emptyList()

        val issues = ArrayList<Issue>(errInfo.length())
        for (i in 0 until errInfo.length()) {
            val item = errInfo.optJSONObject(i) ?: continue
            val issue = toIssue(item, baseOffset) ?: continue
            issues += issue
        }
        return issues
    }

    private fun toIssue(item: JSONObject, baseOffset: Int): Issue? {
        val original = item.optString("orgStr").ifBlank { return null }
        val start = item.optInt("start", -1)
        val end = item.optInt("end", -1)
        if (start < 0 || end <= start) return null

        val suggestions = item.optString("candWord")
            .split('|')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != original }
            .distinct()
            .take(3)

        val help = cleanHelp(item.optString("help"))
        return Issue(
            start = baseOffset + start,
            end = baseOffset + end,
            original = original,
            suggestions = suggestions.ifEmpty { listOf(original) },
            message = help.ifBlank { "더 자연스러운 표현이 있습니다." },
            kind = kindOf(help),
            source = IssueSource.ONLINE,
        )
    }

    private fun kindOf(help: String): IssueKind = when {
        help.contains("띄어") || help.contains("붙여") -> IssueKind.SPACING
        help.contains("문장 부호") || help.contains("온점") || help.contains("쉼표") ->
            IssueKind.PUNCTUATION
        help.contains("조사") || help.contains("어미") || help.contains("문법") ->
            IssueKind.GRAMMAR
        help.contains("표현") -> IssueKind.STYLE
        else -> IssueKind.SPELLING
    }

    private fun cleanHelp(raw: String): String = raw
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** `data = ` 뒤에 오는 "균형이 맞는" JSON 덩어리를 잘라낸다. */
    fun extractJson(html: String): String? {
        val anchor = html.indexOf("data = ").takeIf { it >= 0 }
            ?: html.indexOf("data=").takeIf { it >= 0 }
            ?: return null
        var i = anchor
        while (i < html.length && html[i] != '{' && html[i] != '[') i++
        if (i >= html.length) return null

        val opener = html[i]
        val closer = if (opener == '{') '}' else ']'
        var depth = 0
        var inString = false
        var escaped = false
        val sb = StringBuilder()

        while (i < html.length) {
            val c = html[i]
            sb.append(c)
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == opener -> depth++
                !inString && c == closer -> {
                    depth--
                    if (depth == 0) return sb.toString()
                }
            }
            i++
        }
        return null
    }
}
