package com.arena.gounmal.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arena.gounmal.spell.Issue
import com.arena.gounmal.ui.theme.LocalGounmalPalette
import com.arena.gounmal.ui.theme.editorTextStyle

/**
 * 맞춤법 오류를 iOS 선택 영역처럼 부드럽게 감싸는 편집기.
 *
 * - 하이라이트: 반투명 푸른색 그라디언트 + 둥근 모서리
 * - 하이라이트 양 끝: 조금 더 짙은 파란색의 둥근 "커서" 두 개
 * - Active 항목은 테두리와 커서가 한 단계 더 진해진다.
 */
@Composable
fun HighlightEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    issues: List<Issue>,
    activeIssueId: String?,
    flashRanges: List<IntRange>,
    flashToken: Int,
    onIssueTap: (Issue) -> Unit,
    onShiftPressed: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "여기에 글을 써 보세요.",
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val palette = LocalGounmalPalette.current
    val style = editorTextStyle(palette.textPrimary)
    val keyboardController = LocalSoftwareKeyboardController.current

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // 자동 교정 직후의 "반짝" 효과
    val flash = remember { Animatable(0f) }
    LaunchedEffect(flashToken) {
        if (flashToken > 0) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(durationMillis = 850, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
    }

    Box(modifier) {
        if (value.text.isEmpty()) {
            Text(text = placeholder, style = style.copy(color = palette.textTertiary))
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        // 시스템 키보드 대신 앱 안의 키보드를 쓴다.
                        keyboardController?.hide()
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) {
                        if (event.type == KeyEventType.KeyDown) onShiftPressed()
                    }
                    false
                }
                .pointerInput(issues) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            ?: return@awaitEachGesture
                        val layout = layoutResult ?: return@awaitEachGesture
                        val offset = layout.getOffsetForPosition(up.position)
                        issues.firstOrNull { offset >= it.start && offset < it.end }
                            ?.let(onIssueTap)
                    }
                },
            textStyle = style,
            cursorBrush = SolidColor(palette.caret),
            keyboardOptions = KeyboardOptions.Default,
            onTextLayout = { layoutResult = it },
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    HighlightCanvas(
                        layout = layoutResult,
                        issues = issues,
                        activeIssueId = activeIssueId,
                        flashRanges = flashRanges,
                        flashAlpha = flash.value,
                        modifier = Modifier.matchParentSize(),
                    )
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun HighlightCanvas(
    layout: TextLayoutResult?,
    issues: List<Issue>,
    activeIssueId: String?,
    flashRanges: List<IntRange>,
    flashAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGounmalPalette.current

    // 아주 느린 숨쉬기 효과 — 정지 화면처럼 보이지 않게.
    val breathe = rememberInfiniteTransition(label = "highlight")
    val pulse by breathe.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0),
        ),
        label = "pulse",
    )

    Canvas(modifier) {
        val textLayout = layout ?: return@Canvas

        if (flashAlpha > 0.01f) {
            flashRanges.forEach { range ->
                val boxes = boxesFor(textLayout, range.first, range.last + 1)
                boxes.forEach { rect ->
                    drawRoundedFill(
                        rect = rect,
                        color = palette.accent.copy(alpha = 0.34f * flashAlpha),
                        radius = 6.dp.toPx(),
                        topRadius = 6.dp.toPx(),
                        bottomRadius = 6.dp.toPx(),
                    )
                }
            }
        }

        issues.forEach { issue ->
            val active = issue.id == activeIssueId
            drawIssueHighlight(
                textLayout = textLayout,
                issue = issue,
                active = active,
                pulse = pulse,
                fillTop = palette.highlightFillTop,
                fillBottom = palette.highlightFillBottom,
                stroke = palette.highlightStroke,
                border = palette.highlightBorder,
                caret = palette.caret,
                caretGlow = palette.caretGlow,
            )
        }
    }
}

private fun DrawScope.drawIssueHighlight(
    textLayout: TextLayoutResult,
    issue: Issue,
    active: Boolean,
    pulse: Float,
    fillTop: Color,
    fillBottom: Color,
    stroke: Color,
    border: Color,
    caret: Color,
    caretGlow: Color,
) {
    val boxes = boxesFor(textLayout, issue.start, issue.end)
    if (boxes.isEmpty()) return

    val radius = 6.dp.toPx()
    val tight = 2.0f.dp.toPx()
    val fillAlpha = if (active) 1f else 0.82f

    boxes.forEachIndexed { index, rect ->
        val isFirst = index == 0
        val isLast = index == boxes.lastIndex
        val topRadius = if (isFirst) radius else radius * 0.3f
        val bottomRadius = if (isLast) radius else radius * 0.3f

        drawRoundedFill(
            rect = Rect(rect.left - tight, rect.top + 1.5f.dp.toPx(), rect.right + tight, rect.bottom - 1.5f.dp.toPx()),
            brush = Brush.verticalGradient(
                colors = listOf(
                    fillTop.copy(alpha = fillTop.alpha * fillAlpha),
                    fillBottom.copy(alpha = fillBottom.alpha * fillAlpha),
                ),
                startY = rect.top,
                endY = rect.bottom,
            ),
            radius = radius,
            topRadius = topRadius,
            bottomRadius = bottomRadius,
            strokeColor = if (active) border.copy(alpha = 0.9f) else stroke,
            strokeWidth = if (active) 1.4f.dp.toPx() else 1f.dp.toPx(),
        )
    }

    // 양 끝의 짙은 커서 — iOS 선택 영역의 손잡이를 닮은 둥근 막대.
    val first = boxes.first()
    val last = boxes.last()
    val caretAlpha = if (active) 1f else 0.8f * (0.9f + 0.1f * pulse)

    drawCaretHandle(
        x = first.left - tight * 0.2f,
        top = first.top,
        bottom = first.bottom,
        color = caret.copy(alpha = caretAlpha),
        glow = caretGlow.copy(alpha = caretGlow.alpha * caretAlpha),
    )
    drawCaretHandle(
        x = last.right + tight * 0.2f,
        top = last.top,
        bottom = last.bottom,
        color = caret.copy(alpha = caretAlpha),
        glow = caretGlow.copy(alpha = caretGlow.alpha * caretAlpha),
    )
}

private fun DrawScope.drawCaretHandle(
    x: Float,
    top: Float,
    bottom: Float,
    color: Color,
    glow: Color,
) {
    val width = 3f.dp.toPx()
    val inset = 2f.dp.toPx()
    val t = top + inset
    val b = bottom - inset
    if (b <= t) return

    // 부드러운 번짐
    drawRoundRect(
        color = glow,
        topLeft = Offset(x - width, t - 1.5f.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(width * 2f, (b - t) + 3f.dp.toPx()),
        cornerRadius = CornerRadius(width),
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(x - width / 2f, t),
        size = androidx.compose.ui.geometry.Size(width, b - t),
        cornerRadius = CornerRadius(width / 2f),
    )
}

private fun DrawScope.drawRoundedFill(
    rect: Rect,
    radius: Float,
    topRadius: Float,
    bottomRadius: Float,
    brush: Brush? = null,
    color: Color? = null,
    strokeColor: Color? = null,
    strokeWidth: Float = 0f,
) {
    if (rect.right <= rect.left) return
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = rect,
                topLeft = CornerRadius(topRadius),
                topRight = CornerRadius(topRadius),
                bottomRight = CornerRadius(bottomRadius),
                bottomLeft = CornerRadius(bottomRadius),
            )
        )
    }
    if (brush != null) drawPath(path, brush) else if (color != null) drawPath(path, color)
    if (strokeColor != null && strokeWidth > 0f) {
        drawPath(path, strokeColor, style = Stroke(width = strokeWidth))
    }
}

/** [start, endExclusive) 구간이 차지하는 줄별 사각형. */
private fun boxesFor(layout: TextLayoutResult, start: Int, endExclusive: Int): List<Rect> {
    val length = layout.layoutInput.text.length
    if (length == 0 || start >= endExclusive) return emptyList()
    val from = start.coerceIn(0, length)
    val to = endExclusive.coerceIn(0, length)
    if (from >= to) return emptyList()

    val boxes = ArrayList<Rect>(2)
    var index = from
    while (index < to) {
        val line = layout.getLineForOffset(index)
        val lineEnd = layout.getLineEnd(line, visibleEnd = true).coerceAtLeast(index + 1)
        val segmentEnd = minOf(to, lineEnd)
        val first = layout.getBoundingBox(index)
        val last = layout.getBoundingBox(segmentEnd - 1)
        boxes += Rect(
            left = minOf(first.left, last.left),
            top = minOf(first.top, last.top),
            right = maxOf(first.right, last.right),
            bottom = maxOf(first.bottom, last.bottom),
        )
        index = segmentEnd
    }
    return boxes
}

/** 외부에서 편집기로 커서를 옮기고 포커스를 줄 때 쓴다. */
fun TextFieldValue.placeCaretAtEnd(): TextFieldValue = copy(selection = TextRange(text.length))
