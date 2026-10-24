package com.arena.gounmal.input

/**
 * 한글 자모 조합기.
 *
 * 화면의 한글 키보드는 자모(ㄱ, ㅏ …)를 그대로 보내 오기 때문에
 * "ㄱ + ㅏ + ㄴ + ㅏ" 를 "가나" 로 합쳐 주는 일이 필요하다.
 *
 * 조합 중인 글자는 텍스트 안에 실제 글자로 들어 있고(예: "간"),
 * 다음 자모가 오면 그 구간만 다시 그린다. 안드로이드/Compose 타입에 의존하지 않아
 * 순수 JVM 테스트로 검증할 수 있다.
 */
class HangulComposer {

    data class Result(val text: String, val caret: Int)

    private data class State(
        val initial: Char? = null,
        val medial: Char? = null,
        val tail: Char? = null,
    ) {
        val isEmpty: Boolean get() = initial == null && medial == null && tail == null
    }

    private var state = State()
    private var startIndex = -1
    private var renderedLength = 0

    val isComposing: Boolean get() = !state.isEmpty

    /** 조합 상태를 버린다(텍스트에는 이미 그려져 있으므로 그대로 둔다). */
    fun flush() {
        state = State()
        startIndex = -1
        renderedLength = 0
    }

    /**
     * 자모 하나를 입력한다.
     * 커서가 조합 중인 글자 바로 뒤가 아니면 새로 조합을 시작한다.
     */
    fun type(text: String, selectionStart: Int, selectionEnd: Int, jamo: Char): Result {
        var source = text
        var caret = selectionStart.coerceIn(0, text.length)

        // 선택 영역이 있으면 먼저 지운다.
        if (selectionStart != selectionEnd) {
            val s = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
            val e = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
            source = text.removeRange(s, e)
            caret = s
            flush()
        }

        if (startIndex < 0 || caret != startIndex + renderedLength) {
            flush()
            startIndex = caret
            renderedLength = 0
        }

        val previousRender = render(state)
        val step = nextState(state, jamo)

        return when (step) {
            is Step.Keep -> {
                val newRender = render(step.state)
                val newText = source.replaceRange(startIndex, startIndex + renderedLength, newRender)
                state = step.state
                renderedLength = newRender.length
                Result(newText, startIndex + newRender.length)
            }

            is Step.Flush -> {
                // 지금 글자는 확정하고, 새 글자를 그 뒤에 시작한다.
                val newStart = startIndex + previousRender.length
                val newRender = render(step.state)
                val newText = source.substring(0, newStart) + newRender + source.substring(startIndex + renderedLength)
                state = step.state
                startIndex = newStart
                renderedLength = newRender.length
                Result(newText, newStart + newRender.length)
            }

            is Step.Move -> {
                // 종성을 다음 글자의 초성으로 넘긴다. (간 + ㅏ → 가나)
                val leader = render(step.leader)
                val newRender = render(step.state)
                val newText = source.substring(0, startIndex) + leader + newRender +
                    source.substring(startIndex + renderedLength)
                state = step.state
                startIndex += leader.length
                renderedLength = newRender.length
                Result(newText, startIndex + newRender.length)
            }
        }
    }

    /** 조합 중인 자모를 하나 지운다. 조합 중이 아니면 null. */
    fun backspace(text: String, selectionStart: Int, selectionEnd: Int): Result? {
        if (state.isEmpty || startIndex < 0) return null
        if (selectionStart != selectionEnd) return null

        val caret = selectionEnd.coerceIn(0, text.length)
        if (caret != startIndex + renderedLength) {
            flush()
            return null
        }

        val previousRender = render(state)
        val reduced = when {
            state.tail != null -> state.copy(tail = null)
            state.medial != null -> state.copy(medial = null)
            else -> State()
        }
        val newRender = render(reduced)
        val origin = startIndex
        val newText = text.replaceRange(origin, origin + previousRender.length, newRender)
        val newCaret = origin + newRender.length

        state = reduced
        renderedLength = newRender.length
        if (reduced.isEmpty) {
            state = State()
            startIndex = -1
            renderedLength = 0
        }
        return Result(newText, newCaret)
    }

    // ------------------------------------------------------------ 조합 규칙

    private sealed interface Step {
        /** 지금 구간을 다시 그린다. (조합 계속) */
        data class Keep(val state: State) : Step

        /** 지금 구간은 그대로 두고, 그 뒤에 새 글자를 시작한다. */
        data class Flush(val state: State) : Step

        /** 지금 구간을 [leader] 로 고쳐 그리고, 그 뒤에 새 글자를 시작한다. */
        data class Move(val leader: State, val state: State) : Step
    }

    private fun nextState(current: State, jamo: Char): Step {
        val isVowel = jamo in VOWELS
        val isConsonant = jamo in CONSONANTS
        if (!isVowel && !isConsonant) return Step.Flush(State())

        // 1) 빈 상태
        if (current.isEmpty) {
            return Step.Keep(if (isVowel) State(medial = jamo) else State(initial = jamo))
        }

        // 2) 초성만 있는 상태
        if (current.initial != null && current.medial == null) {
            if (isVowel) return Step.Keep(current.copy(medial = jamo))
            return Step.Flush(State(initial = jamo))
        }

        // 3) 모음 단독
        if (current.initial == null && current.medial != null) {
            if (isVowel) {
                val compound = COMPOUND_VOWELS[current.medial to jamo]
                if (compound != null) return Step.Keep(current.copy(medial = compound))
            }
            return Step.Flush(if (isVowel) State(medial = jamo) else State(initial = jamo))
        }

        // 4) 초성 + 중성
        if (current.initial != null && current.medial != null && current.tail == null) {
            if (isVowel) {
                val compound = COMPOUND_VOWELS[current.medial to jamo]
                if (compound != null) return Step.Keep(current.copy(medial = compound))
                return Step.Flush(State(medial = jamo))
            }
            if (jamo in VALID_TAILS) return Step.Keep(current.copy(tail = jamo))
            return Step.Flush(State(initial = jamo))
        }

        // 5) 초성 + 중성 + 종성
        if (current.tail != null) {
            if (isConsonant) {
                val compound = COMPOUND_TAILS[current.tail to jamo]
                if (compound != null) return Step.Keep(current.copy(tail = compound))
                return Step.Flush(State(initial = jamo))
            }
            // 모음이 오면 종성을 다음 글자의 초성으로 넘긴다. (ex. 간 + ㅏ → 가나)
            if (isVowel) {
                val movable = current.tail?.let { CHOSEONG.indexOf(it) >= 0 } ?: false
                if (movable) {
                    return Step.Move(
                        leader = current.copy(tail = null),
                        state = State(initial = current.tail, medial = jamo),
                    )
                }
                return Step.Flush(State(medial = jamo))
            }
            return Step.Flush(State())
        }

        return Step.Flush(State())
    }

    /** 조합 상태를 실제 글자로 만든다. */
    private fun render(target: State): String {
        if (target.isEmpty) return ""
        val initial = target.initial
        val medial = target.medial
        val tail = target.tail

        if (initial == null) {
            return buildString {
                medial?.let { append(it) }
                tail?.let { append(it) }
            }
        }
        if (medial == null) {
            return buildString {
                append(initial)
                tail?.let { append(it) }
            }
        }

        val l = CHOSEONG.indexOf(initial)
        val v = JUNGSEONG.indexOf(medial)
        val t = if (tail == null) 0 else JONGSEONG.indexOf(tail.toString())
        if (l < 0 || v < 0 || t < 0) return initial.toString() + medial + (tail?.toString() ?: "")
        return ((0xAC00 + (l * 21 + v) * 28 + t).toChar()).toString()
    }

    private companion object {
        const val CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        const val JUNGSEONG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"

        val JONGSEONG: List<String> = listOf(
            "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ",
            "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ",
        )

        val CONSONANTS: Set<Char> = setOf(
            'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ',
            'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ',
            'ㅍ', 'ㅎ',
        )

        val VOWELS: Set<Char> = setOf(
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ',
            'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ',
        )

        val VALID_TAILS: Set<Char> = setOf(
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ',
            'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
        )

        val COMPOUND_VOWELS: Map<Pair<Char, Char>, Char> = mapOf(
            ('ㅗ' to 'ㅏ') to 'ㅘ',
            ('ㅗ' to 'ㅐ') to 'ㅙ',
            ('ㅗ' to 'ㅣ') to 'ㅚ',
            ('ㅜ' to 'ㅓ') to 'ㅝ',
            ('ㅜ' to 'ㅔ') to 'ㅞ',
            ('ㅜ' to 'ㅣ') to 'ㅟ',
            ('ㅡ' to 'ㅣ') to 'ㅢ',
        )

        val COMPOUND_TAILS: Map<Pair<Char, Char>, Char> = mapOf(
            ('ㄱ' to 'ㅅ') to 'ㄳ',
            ('ㄴ' to 'ㅈ') to 'ㄵ',
            ('ㄴ' to 'ㅎ') to 'ㄶ',
            ('ㄹ' to 'ㄱ') to 'ㄺ',
            ('ㄹ' to 'ㅁ') to 'ㄻ',
            ('ㄹ' to 'ㅂ') to 'ㄼ',
            ('ㄹ' to 'ㅅ') to 'ㄽ',
            ('ㄹ' to 'ㅌ') to 'ㄾ',
            ('ㄹ' to 'ㅍ') to 'ㄿ',
            ('ㄹ' to 'ㅎ') to 'ㅀ',
            ('ㅂ' to 'ㅅ') to 'ㅄ',
        )
    }
}
