package com.arena.gounmal

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arena.gounmal.input.HangulComposer
import com.arena.gounmal.input.ShiftDoubleTapDetector
import com.arena.gounmal.spell.Edit
import com.arena.gounmal.spell.Edits
import com.arena.gounmal.spell.Issue
import com.arena.gounmal.spell.OfflineSpeller
import com.arena.gounmal.spell.OnlineSpeller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** 자동 교정 후 화면 위에 잠깐 떠오르는 알림. */
data class Banner(
    val id: Long,
    val message: String,
    val undoable: Boolean = false,
)

enum class OnlineState { IDLE, CHECKING, DONE, FAILED }

/**
 * 편집기의 상태를 들고 있는 곳.
 *
 * 타이핑 → (디바운스) → 오프라인 검사 → (조용할 때) 온라인 정밀 검사 순서로 흐른다.
 */
class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val offlineSpeller = OfflineSpeller()
    private val onlineSpeller = OnlineSpeller()
    private val composer = HangulComposer()
    private val shiftDetector = ShiftDoubleTapDetector()

    private var checkJob: Job? = null
    private var shiftJob: Job? = null
    private var bannerJob: Job? = null
    private var undoSnapshot: TextFieldValue? = null
    private var bannerCounter = 0L
    private val dismissed = LinkedHashSet<String>()

    var text by mutableStateOf(TextFieldValue(SAMPLE_TEXT, TextRange(SAMPLE_TEXT.length, SAMPLE_TEXT.length)))
        private set

    var issues by mutableStateOf<List<Issue>>(emptyList())
        private set

    var activeIssueId by mutableStateOf<String?>(null)
        private set

    var bubbleSuppressed by mutableStateOf(false)
        private set

    var isChecking by mutableStateOf(false)
        private set

    var onlineEnabled by mutableStateOf(prefs.getBoolean(KEY_ONLINE, true))
        private set

    var onlineState by mutableStateOf(OnlineState.IDLE)
        private set

    var onlineError by mutableStateOf<String?>(null)
        private set

    var banner by mutableStateOf<Banner?>(null)
        private set

    var flashToken by mutableStateOf(0)
        private set

    var flashRanges by mutableStateOf<List<IntRange>>(emptyList())
        private set

    /** 화면에 보여줄 "지금 손댈 곳". 직접 고른 항목이 없으면 커서와 가까운 항목. */
    val activeIssue: Issue?
        get() {
            if (bubbleSuppressed) return null
            if (issues.isEmpty()) return null
            activeIssueId?.let { id -> issues.firstOrNull { it.id == id }?.let { return it } }
            val caret = text.selection.end
            return issues.minByOrNull { issue ->
                when {
                    caret in issue.start..issue.end -> -1
                    else -> abs(issue.start - caret)
                }
            }
        }

    val issueCount: Int get() = issues.size

    val canUndo: Boolean get() = undoSnapshot != null

    init {
        scheduleChecks(immediate = true)
    }

    // ---------------------------------------------------------------- 입력

    fun onTextChange(new: TextFieldValue) {
        composer.flush()
        commit(new, resetBubble = new.text != text.text, debounce = true)
    }

    /** 한글 자모 한 글자. 앱 키보드에서 들어온다. */
    fun typeJamo(jamo: Char) {
        val current = text
        val result = composer.type(
            text = current.text,
            selectionStart = current.selection.min,
            selectionEnd = current.selection.max,
            jamo = jamo,
        )
        commit(
            TextFieldValue(result.text, TextRange(result.caret, result.caret)),
            resetBubble = true,
            debounce = true,
        )
    }

    /** 그 밖의 문자(스페이스, 영문, 문장부호, 줄바꿈). */
    fun keyboardText(value: String) {
        composer.flush()
        val current = text
        val start = current.selection.min
        val end = current.selection.max
        val newText = current.text.replaceRange(start, end, value)
        commit(TextFieldValue(newText, TextRange(start + value.length, start + value.length)), resetBubble = true, debounce = true)
    }

    fun keyboardBackspace() {
        val beforeDelete = text
        composer.backspace(
            text = beforeDelete.text,
            selectionStart = beforeDelete.selection.min,
            selectionEnd = beforeDelete.selection.max,
        )?.let { result ->
            commit(
                TextFieldValue(result.text, TextRange(result.caret, result.caret)),
                resetBubble = true,
                debounce = true,
            )
            return
        }
        val current = text
        if (!current.selection.collapsed) {
            val start = current.selection.min
            val end = current.selection.max
            val newText = current.text.removeRange(start, end)
            commit(TextFieldValue(newText, TextRange(start, start)), resetBubble = true, debounce = true)
            return
        }
        val caret = current.selection.end
        if (caret <= 0) return
        val newText = current.text.removeRange(caret - 1, caret)
        commit(TextFieldValue(newText, TextRange(caret - 1, caret - 1)), resetBubble = true, debounce = true)
    }

    private fun commit(value: TextFieldValue, resetBubble: Boolean, debounce: Boolean = true) {
        text = value
        if (resetBubble) {
            bubbleSuppressed = false
            activeIssueId = null
        }
        scheduleChecks(debounce = debounce)
    }

    // ---------------------------------------------------------------- ⇧ 두 번

    /**
     * ⇧ 한 번: 다음 오류로 커서/카드 이동.
     * ⇧ 두 번(400ms 이내): 모든 오류를 자동 교정.
     *
     * 두 번 눌렀을 때 "한 번" 동작이 따라 실행되지 않도록,
     * 한 번 동작은 창이 지난 뒤에 실행한다.
     */
    fun onShiftTap() {
        if (shiftDetector.onShiftPressed()) {
            shiftJob?.cancel()
            shiftJob = null
            applyAllIssues()
            return
        }
        shiftJob?.cancel()
        shiftJob = viewModelScope.launch {
            delay(ShiftDoubleTapDetector.DEFAULT_WINDOW_MILLIS)
            moveToNextIssue()
        }
    }

    private fun moveToNextIssue() {
        val list = issues
        if (list.isEmpty()) {
            showBanner("지금은 고칠 곳이 없어요")
            return
        }
        val currentIndex = list.indexOfFirst { it.id == activeIssueId }
        val next = list[(currentIndex + 1) % list.size]
        bubbleSuppressed = false
        activeIssueId = next.id
        val target = TextRange(next.end, next.end)
        text = text.copy(selection = target)
    }

    fun selectIssue(issue: Issue) {
        bubbleSuppressed = false
        activeIssueId = if (activeIssueId == issue.id) null else issue.id
    }

    fun suppressBubble() {
        bubbleSuppressed = true
        activeIssueId = null
    }

    fun showBubble() {
        bubbleSuppressed = false
    }

    // ---------------------------------------------------------------- 교정

    /** 하이라이트 하나를 지정한 대치어로 바꾼다. */
    fun applyIssue(issue: Issue, replacement: String = issue.primarySuggestion) {
        if (issue.start < 0 || issue.end > text.text.length) return
        val current = text
        val result = Edits.apply(current.text, listOf(Edit.of(issue, replacement)), current.selection.end)
        undoSnapshot = current
        text = TextFieldValue(result.text, TextRange(result.caret, result.caret))
        flashRanges = listOf(result.caret - replacement.length until result.caret)
        flashToken++
        activeIssueId = null
        bubbleSuppressed = false
        showBanner(bannerText(1), undoable = true)
        scheduleChecks(immediate = true)
    }

    /** 지금 화면에 있는 모든 오류를 한 번에 교정한다. (⇧ 두 번과 같은 동작) */
    fun applyAllIssues() {
        val current = text
        val targets = issues.filter { it.primarySuggestion != it.original }
        if (targets.isEmpty()) {
            showBanner("고칠 곳이 없어요")
            return
        }
        val edits = Edits.dedupe(targets.map { Edit.of(it) })
        if (edits.isEmpty()) return

        val result = Edits.apply(current.text, edits, current.selection.end)
        undoSnapshot = current
        text = TextFieldValue(result.text, TextRange(result.caret, result.caret))

        val ranges = ArrayList<IntRange>(edits.size)
        var shift = 0
        for (edit in edits) {
            val start = edit.start + shift
            ranges += start until (start + edit.replacement.length)
            shift += edit.delta
        }
        flashRanges = ranges
        flashToken++
        activeIssueId = null
        bubbleSuppressed = false
        showBanner(bannerText(edits.size), undoable = true)
        scheduleChecks(immediate = true)
    }

    fun undo() {
        val snapshot = undoSnapshot ?: return
        undoSnapshot = null
        text = snapshot
        activeIssueId = null
        bubbleSuppressed = false
        flashRanges = emptyList()
        showBanner("되돌렸어요")
        scheduleChecks(immediate = true)
    }

    fun dismissIssue(issue: Issue) {
        dismissed += issue.dismissKey
        issues = issues.filterNot { it.id == issue.id }
        if (activeIssueId == issue.id) activeIssueId = null
    }

    // ---------------------------------------------------------------- 검사

    fun toggleOnline() {
        onlineEnabled = !onlineEnabled
        prefs.edit().putBoolean(KEY_ONLINE, onlineEnabled).apply()
        if (onlineEnabled) {
            scheduleChecks(immediate = true)
        } else {
            onlineState = OnlineState.IDLE
            onlineError = null
            scheduleChecks(immediate = true)
        }
    }

    fun retryOnlineCheck() {
        onlineEnabled = true
        prefs.edit().putBoolean(KEY_ONLINE, true).apply()
        scheduleChecks(immediate = true)
    }

    private fun scheduleChecks(immediate: Boolean = false, debounce: Boolean = true) {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            if (!immediate && debounce) delay(OFFLINE_DEBOUNCE_MS)
            isChecking = true
            runOfflineCheck()
            isChecking = false

            if (onlineEnabled) {
                delay(ONLINE_DEBOUNCE_MS)
                runOnlineCheck()
            }
        }
    }

    private suspend fun runOfflineCheck() {
        val snapshot = text.text
        val found = withContext(Dispatchers.Default) { offlineSpeller.check(snapshot) }
        if (snapshot != text.text) return
        publish(find(found))
    }

    private suspend fun runOnlineCheck() {
        val snapshot = text.text
        if (snapshot.isBlank()) {
            onlineState = OnlineState.IDLE
            return
        }
        onlineState = OnlineState.CHECKING
        val result = onlineSpeller.check(snapshot)
        if (snapshot != text.text) return
        if (!result.isSuccess) {
            onlineState = OnlineState.FAILED
            onlineError = result.error
            return
        }
        onlineState = OnlineState.DONE
        onlineError = null
        val local = find(offlineSpeller.check(snapshot))
        val extra = result.issues.filter { online ->
            online.original.isNotBlank() &&
                online.primarySuggestion != online.original &&
                local.none { it.overlaps(online) }
        }
        publish(find(local + extra))
    }

    private fun find(found: List<Issue>): List<Issue> = found
        .filter { it.original.isNotBlank() && it.primarySuggestion != it.original }
        .filterNot { it.dismissKey in dismissed }
        .sortedWith(compareBy({ it.start }, { -it.length }))

    /** 겹치는 항목은 앞의 것만 남긴다. 하이라이트가 겹쳐 그려지면 지저분해진다. */
    private fun dropOverlaps(sorted: List<Issue>): List<Issue> {
        val result = ArrayList<Issue>(sorted.size)
        var lastEnd = -1
        for (issue in sorted) {
            if (issue.start < lastEnd) continue
            result += issue
            lastEnd = issue.end
        }
        return result
    }

    private fun publish(newIssues: List<Issue>) {
        val cleaned = dropOverlaps(newIssues)
        issues = cleaned
        val ids = cleaned.map { it.id }.toSet()
        if (activeIssueId != null && activeIssueId !in ids) activeIssueId = null
    }

    private fun bannerText(count: Int) = if (count == 1) "1곳을 고쳤어요" else "${count}곳을 고쳤어요"

    private fun showBanner(message: String, undoable: Boolean = false) {
        bannerCounter++
        banner = Banner(bannerCounter, message, undoable)
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            delay(3800)
            banner = null
        }
    }

    fun dismissBanner() {
        bannerJob?.cancel()
        banner = null
    }

    // ---------------------------------------------------------------- 도구

    fun insertSample() {
        dismissed.clear()
        undoSnapshot = text
        text = TextFieldValue(SAMPLE_TEXT, TextRange(SAMPLE_TEXT.length, SAMPLE_TEXT.length))
        activeIssueId = null
        bubbleSuppressed = false
        showBanner("예시 문장을 넣었어요")
        scheduleChecks(immediate = true)
    }

    fun clearText() {
        undoSnapshot = text
        text = TextFieldValue("", TextRange.Zero)
        issues = emptyList()
        activeIssueId = null
        bubbleSuppressed = false
        flashRanges = emptyList()
        onlineState = OnlineState.IDLE
        showBanner("모두 지웠어요", undoable = true)
    }

    companion object {
        private const val PREFS = "gounmal_prefs"
        private const val KEY_ONLINE = "online_check_enabled"

        private const val OFFLINE_DEBOUNCE_MS = 240L
        private const val ONLINE_DEBOUNCE_MS = 900L

        const val SAMPLE_TEXT =
            "안녕하세요! 오늘은 몇일 만에 사무실에 나갔어요. " +
                "아침부터 정신이 없어서 인사도 제대로 못했는데, 웬지 오늘은 운이 좋은것같아요. " +
                "내일은 꼭 일찍 일어나서 숙제를 끝낼께요. 그래도 지각은 안되요."
    }
}
