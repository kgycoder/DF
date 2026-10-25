package com.arena.gounmal.spell

/** 교정 항목의 성격. 뱃지 색/문구에 쓰인다. */
enum class IssueKind(val label: String) {
    SPELLING("맞춤법"),
    SPACING("띄어쓰기"),
    GRAMMAR("문법"),
    PUNCTUATION("문장부호"),
    STYLE("표현"),
}

enum class IssueSource { OFFLINE, ONLINE }

/**
 * 텍스트 안의 한 곳에서 발견된 교정 항목.
 *
 * [start], [end] 는 원본 텍스트 기준 인덱스(끝 인덱스는 exclusive)다.
 */
data class Issue(
    val start: Int,
    val end: Int,
    val original: String,
    val suggestions: List<String>,
    val message: String,
    val kind: IssueKind = IssueKind.SPELLING,
    val source: IssueSource = IssueSource.OFFLINE,
) {
    init {
        require(start >= 0 && end >= start) { "invalid range: $start..$end" }
    }

    /** 첫 번째 대치어. 자동 교정은 항상 이 값을 쓴다. */
    val primarySuggestion: String get() = suggestions.firstOrNull() ?: original

    val id: String get() = "$source:${kind.name}:$start:$end:$primarySuggestion"

    val length: Int get() = end - start

    fun overlaps(other: Issue): Boolean = start < other.end && other.start < end

    fun overlaps(range: IntRange): Boolean = start < range.last + 1 && range.first < end

    /** "무시" 판정용 키. 같은 실수를 다시 만들었을 때도 계속 무시된다. */
    val dismissKey: String get() = "${kind.name}:${original}:${primarySuggestion}"
}

/** 텍스트의 일부 구간을 다른 문자열로 바꾸는 단위. */
data class Edit(val start: Int, val end: Int, val replacement: String) {
    val delta: Int get() = replacement.length - (end - start)

    companion object {
        fun of(issue: Issue, replacement: String = issue.primarySuggestion): Edit =
            Edit(issue.start, issue.end, replacement)
    }
}

data class ApplyResult(val text: String, val caret: Int)

object Edits {

    /**
     * 여러 편집을 한 번에 적용하고, 커서 위치를 "사람이 기대하는" 자리로 옮긴다.
     *
     * - 편집 구간이 커서보다 앞이면 커서도 같이 밀린다.
     * - 커서가 편집 구간 안에 있었으면 교정된 단어 뒤로 이동한다.
     * - 겹치는 편집은 앞의 것만 적용한다.
     */
    fun apply(text: String, edits: List<Edit>, caret: Int): ApplyResult {
        val valid = edits
            .filter { it.start >= 0 && it.end <= text.length && it.start <= it.end }
            .sortedWith(compareBy({ it.start }, { it.end }))

        val sb = StringBuilder(text)
        var shift = 0
        var newCaret = caret
        var lastEnd = -1

        for (edit in valid) {
            if (edit.start < lastEnd) continue // 겹치는 편집은 건너뛴다.
            lastEnd = edit.end

            sb.replace(edit.start, edit.end, edit.replacement)

            when {
                caret >= edit.end -> newCaret += edit.delta
                caret in edit.start until edit.end ->
                    newCaret = edit.start + shift + edit.replacement.length
            }
            shift += edit.delta
        }

        return ApplyResult(
            text = sb.toString(),
            caret = newCaret.coerceIn(0, sb.length),
        )
    }

    /** 겹치는 편집들을 정리해 "서로 겹치지 않는" 목록으로 만든다. */
    fun dedupe(edits: List<Edit>): List<Edit> {
        val sorted = edits
            .filter { it.start <= it.end }
            .sortedWith(compareBy({ it.start }, { it.end }))
        val result = mutableListOf<Edit>()
        var lastEnd = -1
        for (edit in sorted) {
            if (edit.start < lastEnd) continue
            result += edit
            lastEnd = edit.end
        }
        return result
    }
}

/** 두 텍스트의 차이를 최소 편집 목록으로 계산한다(자동 교정 되돌리기용). */
object TextDiff {

    fun editsBetween(old: String, new: String): List<Edit> {
        val prefix = commonPrefix(old, new)
        val suffix = commonSuffix(old, new, prefix)
        if (prefix == old.length && prefix == new.length) return emptyList()
        return listOf(
            Edit(
                start = prefix,
                end = old.length - suffix,
                replacement = new.substring(prefix, new.length - suffix),
            )
        )
    }

    private fun commonPrefix(a: String, b: String): Int {
        val max = minOf(a.length, b.length)
        var i = 0
        while (i < max && a[i] == b[i]) i++
        return i
    }

    private fun commonSuffix(a: String, b: String, prefix: Int): Int {
        val max = minOf(a.length, b.length) - prefix
        var i = 0
        while (i < max && a[a.length - 1 - i] == b[b.length - 1 - i]) i++
        return i
    }
}
