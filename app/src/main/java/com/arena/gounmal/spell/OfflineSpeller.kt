package com.arena.gounmal.spell

/**
 * 네트워크 없이 즉시 동작하는 교정 엔진.
 *
 * 원칙: **확신할 수 있는 것만** 잡는다. 잘못 고치는 것보다 안 고치는 편이 낫기 때문에
 * 사전에 없는 단어를 무작정 의심하지 않고, 다음 세 종류만 검사한다.
 *
 *  1. 자주 틀리는 표기(오·탈자) 목록
 *  2. 확실한 띄어쓰기 규칙
 *  3. 문장 부호 주변 공백 규칙
 *
 * 온라인 정밀 검사([OnlineSpeller])는 이 결과 위에 "추가"로 얹힌다.
 */
class OfflineSpeller {

    fun check(text: String): List<Issue> {
        if (text.isBlank()) return emptyList()

        val found = ArrayList<Issue>(8)
        for (rule in RULES) {
            for (match in rule.regex.findAll(text)) {
                if (!rule.accept(match, text)) continue
                val corrected = rule.build(match)
                if (corrected == match.value) continue
                if (corrected.isEmpty() && match.value.isBlank()) continue
                found += Issue(
                    start = match.range.first,
                    end = match.range.last + 1,
                    original = match.value,
                    suggestions = listOf(corrected),
                    message = rule.message,
                    kind = rule.kind,
                    source = IssueSource.OFFLINE,
                )
            }
        }
        return normalize(found)
    }

    /** 같은 자리를 여러 규칙이 잡으면 "더 긴 것" 하나만 남긴다. */
    private fun normalize(issues: List<Issue>): List<Issue> {
        val sorted = issues.sortedWith(compareBy({ it.start }, { -(it.end - it.start) }))
        val result = ArrayList<Issue>(sorted.size)
        var lastEnd = -1
        for (issue in sorted) {
            if (issue.start < lastEnd) {
                // 앞선 항목과 겹친다. 완전히 같은 구간이면 건너뛰고, 아니면 버린다.
                continue
            }
            result += issue
            lastEnd = issue.end
        }
        return result
    }

    private class Rule(
        val regex: Regex,
        val kind: IssueKind,
        val message: String,
        val build: (MatchResult) -> String,
        val accept: (MatchResult, String) -> Boolean = { _, _ -> true },
    )

    private companion object {

        /** 한글 단어의 경계. 앞뒤로 한글이 붙어 있으면 다른 단어의 일부로 본다. */
        const val NOT_HANGUL_BEFORE = "(?<![가-힣])"
        const val NOT_HANGUL_AFTER = "(?![가-힣])"

        /** 오탈자 사전: 틀린 표기 → 바른 표기 (여러 개면 첫 번째를 자동 적용). */
        private val MISSPELLINGS: List<Triple<String, List<String>, String>> = listOf(
            Triple("됬", listOf("됐"), "‘됐’이 바른 표기입니다. (‘되었’의 준말)"),
            Triple("되요", listOf("돼요"), "‘되어요’의 준말은 ‘돼요’입니다."),
            Triple("되서", listOf("돼서"), "‘되어서’의 준말은 ‘돼서’입니다."),
            Triple("되야", listOf("돼야"), "‘되어야’의 준말은 ‘돼야’입니다."),
            Triple("되도", listOf("돼도"), "‘되어도’의 준말은 ‘돼도’입니다."),
            Triple("되냐", listOf("돼냐"), "‘되어냐’가 아니라 ‘돼냐’입니다."),
            Triple("안되요", listOf("안 돼요"), "‘되어요’의 준말은 ‘돼요’입니다. ‘안’은 띄어 씁니다."),
            Triple("안됬", listOf("안 됐"), "‘되었’의 준말은 ‘됐’입니다. ‘안’은 띄어 씁니다."),
            Triple("않되요", listOf("안 돼요"), "‘아니 되어요’의 준말은 ‘안 돼요’입니다."),
            Triple("안되", listOf("안 되"), "‘안’은 부사이므로 ‘안 되-’처럼 띄어 씁니다."),
            Triple("않되", listOf("안 되"), "‘아니 하다’의 뜻은 ‘안 되-’로 씁니다."),
            Triple("않하", listOf("안 하"), "‘아니 하다’의 뜻은 ‘안 하-’로 씁니다."),
            Triple("않해", listOf("안 해"), "‘아니 하다’의 뜻은 ‘안 해’로 씁니다."),
            Triple("않했", listOf("안 했"), "‘아니 하다’의 뜻은 ‘안 했-’으로 씁니다."),
            Triple("않할", listOf("안 할"), "‘아니 하다’의 뜻은 ‘안 할’로 씁니다."),
            Triple("금새", listOf("금세"), "‘금시에’의 준말은 ‘금세’입니다."),
            Triple("오랫만", listOf("오랜만"), "‘오래간만’의 준말은 ‘오랜만’입니다."),
            Triple("몇일", listOf("며칠"), "‘몇일’이 아니라 ‘며칠’이 바른 표기입니다."),
            Triple("웬지", listOf("왠지"), "‘왜인지’의 준말은 ‘왠지’입니다."),
            Triple("왠일", listOf("웬일"), "‘어찌 된 일’의 뜻은 ‘웬일’입니다."),
            Triple("왠만", listOf("웬만"), "‘웬만하다’가 바른 표기입니다."),
            Triple("어의없", listOf("어이없"), "‘어이없다’가 바른 표기입니다."),
            Triple("희안", listOf("희한"), "‘희한하다’가 바른 표기입니다."),
            Triple("역활", listOf("역할"), "‘역할(役割)’이 바른 표기입니다."),
            Triple("갯수", listOf("개수"), "‘개수’가 바른 표기입니다."),
            Triple("댓가", listOf("대가"), "‘대가(對價)’가 바른 표기입니다."),
            Triple("설겆", listOf("설거"), "‘설거지’가 바른 표기입니다."),
            Triple("찌게", listOf("찌개"), "‘찌개’가 바른 표기입니다."),
            Triple("떡볶기", listOf("떡볶이"), "‘떡볶이’가 바른 표기입니다."),
            Triple("오뚜기", listOf("오뚝이"), "‘오뚝이’가 바른 표기입니다."),
            Triple("뒤쳐", listOf("뒤처"), "‘뒤처지다’가 바른 표기입니다."),
            Triple("뵈요", listOf("봬요"), "‘뵈어요’의 준말은 ‘봬요’입니다."),
            Triple("햇갈리", listOf("헷갈리"), "‘헷갈리다’가 바른 표기입니다."),
            Triple("어떻해", listOf("어떡해"), "‘어떻게 해’의 준말은 ‘어떡해’입니다."),
            Triple("어떻케", listOf("어떡해"), "‘어떻게 해’의 준말은 ‘어떡해’입니다."),
            Triple("담궈", listOf("담가"), "‘담그다’의 활용형은 ‘담가’입니다."),
            Triple("치루", listOf("치르"), "‘치르다’가 바른 표기입니다."),
            Triple("아니예요", listOf("아니에요"), "‘아니에요’가 바른 표기입니다."),
            Triple("거에요", listOf("거예요"), "받침 없는 말 뒤에는 ‘-예요’를 씁니다."),
            Triple("할께", listOf("할게"), "‘-ㄹ게’로 적습니다. (‘께’는 높임 조사)"),
            Triple("갈께", listOf("갈게"), "‘-ㄹ게’로 적습니다."),
            Triple("줄께", listOf("줄게"), "‘-ㄹ게’로 적습니다."),
            Triple("볼께", listOf("볼게"), "‘-ㄹ게’로 적습니다."),
            Triple("올께", listOf("올게"), "‘-ㄹ게’로 적습니다."),
            Triple("먹을께", listOf("먹을게"), "‘-ㄹ게’로 적습니다."),
            Triple("드릴께", listOf("드릴게"), "‘-ㄹ게’로 적습니다."),
        )

        /** 조사/어미처럼 앞 단어에 붙어도 되는 표기는 경계 검사를 하지 않는다. */
        private val BOUNDARY_FREE = setOf(
            "됬", "되요", "되서", "되야", "되도", "되냐", "안되요", "안됬", "않되요",
            "안되", "않되", "않하", "않해", "않했", "않할",
            "할께", "갈께", "줄께", "볼께", "올께", "먹을께", "드릴께",
            "뒤쳐", "어의없", "희안", "오랫만", "왠만", "설겆",
            "아니예요", "거에요", "뵈요", "햇갈리", "치루", "담궈",
        )

        private fun misspellingRules(): List<Rule> = MISSPELLINGS.map { (wrong, right, note) ->
            val pattern = if (wrong in BOUNDARY_FREE) {
                Regex(Regex.escape(wrong))
            } else {
                Regex(NOT_HANGUL_BEFORE + Regex.escape(wrong) + NOT_HANGUL_AFTER)
            }
            Rule(
                regex = pattern,
                kind = IssueKind.SPELLING,
                message = note,
                build = { right.first() },
            )
        }

        /** 띄어쓰기 규칙. */
        private fun spacingRules(): List<Rule> = listOf(
            // 의존 명사 ‘수/줄/리’ — 앞말의 받침이 ㄹ일 때만 확신할 수 있다.
            // (할수있다 → 할 수 있다, 그럴줄알았다 → 그럴 줄 알았다)
            Rule(
                regex = Regex("([가-힣])(수|줄|리)(?=있|없|알|모르)"),
                kind = IssueKind.SPACING,
                message = "의존 명사는 앞말과 띄어 씁니다. (예: 할 수 있다, 그럴 줄 알았다)",
                build = { "${it.groupValues[1]} ${it.groupValues[2]} " },
                accept = { match, _ ->
                    val head = match.groupValues[1]
                    head != "별" && hasRieulTail(head.last())
                },
            ),
            // 할거야 → 할 거야, 먹을것같다 → 먹을 것 같다
            Rule(
                regex = Regex("([가-힣])(것|거)(?=같|야|예요|이에요|입니다|이다|인데|뿐|만큼|때|수|줄|리|동안|채|척)"),
                kind = IssueKind.SPACING,
                message = "의존 명사 ‘것/거’는 앞말과 띄어 씁니다.",
                build = { "${it.groupValues[1]} ${it.groupValues[2]}" },
                accept = { match, _ -> hasRieulTail(match.groupValues[1].last()) },
            ),
            // 한개 → 한 개
            Rule(
                regex = Regex("(한|두|세|네|몇|여러)개"),
                kind = IssueKind.SPACING,
                message = "단위를 나타내는 명사 ‘개’는 띄어 씁니다.",
                build = { "${it.groupValues[1]} 개" },
            ),
            // 3명 → 3 명
            Rule(
                regex = Regex("([0-9]+)(개|명|마리|권|장|잔|병|대|층|원)"),
                kind = IssueKind.SPACING,
                message = "단위 명사는 숫자와 띄어 쓰는 것이 원칙입니다.",
                build = { "${it.groupValues[1]} ${it.groupValues[2]}" },
            ),
        )

        /** 문장 부호 규칙. */
        private fun punctuationRules(): List<Rule> = listOf(
            Rule(
                regex = Regex("(?<=[가-힣])[ \t]+(?=[,.!?])"),
                kind = IssueKind.PUNCTUATION,
                message = "문장 부호는 앞말에 붙여 씁니다.",
                build = { "" },
            ),
            Rule(
                regex = Regex("[ \t]{2,}"),
                kind = IssueKind.SPACING,
                message = "공백이 연속으로 들어갔습니다.",
                build = { " " },
            ),
            Rule(
                regex = Regex("([.!?])(?=[가-힣])"),
                kind = IssueKind.PUNCTUATION,
                message = "문장 부호 뒤에는 띄어 씁니다.",
                build = { "${it.groupValues[1]} " },
            ),
            Rule(
                regex = Regex("([.!?])(?=[A-Za-z])"),
                kind = IssueKind.PUNCTUATION,
                message = "문장 부호 뒤에는 띄어 씁니다.",
                build = { "${it.groupValues[1]} " },
                accept = { match, text ->
                    // 소수점(3.14), 도메인(a.b), 약어(U.S.A) 는 건드리지 않는다.
                    val before = text.getOrNull(match.range.first - 1)
                    val after = text.getOrNull(match.range.last + 1)
                    val numeric = before?.isDigit() == true && after?.isDigit() == true
                    val dotted = before?.isLetter() == true && after?.isLowerCase() == true
                    !numeric && !dotted
                },
            ),
            Rule(
                regex = Regex("(었|았|겠)읍니다"),
                kind = IssueKind.SPELLING,
                message = "‘-읍니다’가 아니라 ‘-습니다’입니다.",
                build = { "${it.groupValues[1]}습니다" },
            ),
        )

        /** 한글 음절의 종성(받침)이 ㄹ인지. */
        private fun hasRieulTail(ch: Char): Boolean {
            val code = ch.code
            if (code < 0xAC00 || code > 0xD7A3) return false
            return (code - 0xAC00) % 28 == 8
        }

        val RULES: List<Rule> = misspellingRules() + spacingRules() + punctuationRules()
    }
}
