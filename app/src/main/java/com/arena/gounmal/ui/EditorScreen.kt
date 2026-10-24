package com.arena.gounmal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.gounmal.EditorViewModel
import com.arena.gounmal.OnlineState
import com.arena.gounmal.ui.theme.LocalGounmalPalette

@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    val palette = LocalGounmalPalette.current
    val focusRequester = remember { FocusRequester() }

    // 시작하자마자 바로 타이핑할 수 있게.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.background(palette.background),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.background,
                            if (palette.isDark) palette.background else palette.backgroundElevated.copy(alpha = 0.85f),
                        )
                    )
                )
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(Modifier.fillMaxSize()) {
                EditorHeader(
                    viewModel = viewModel,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )

                EditorCard(
                    viewModel = viewModel,
                    focusRequester = focusRequester,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                )

                Spacer(Modifier.height(10.dp))

                SuggestionArea(
                    viewModel = viewModel,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )

                Spacer(Modifier.height(6.dp))

                GounmalKeyboard(
                    onChar = viewModel::typeJamo,
                    onText = viewModel::keyboardText,
                    onBackspace = viewModel::keyboardBackspace,
                    onEnter = { viewModel.keyboardText("\n") },
                    onShift = viewModel::onShiftTap,
                )
            }

            BannerHost(viewModel = viewModel, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

// ------------------------------------------------------------------ 헤더

@Composable
private fun EditorHeader(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val palette = LocalGounmalPalette.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark()
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "고운글",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = palette.textPrimary,
            )
            Text(
                text = "타이핑하는 순간, 맞춤법이 고쳐집니다",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = palette.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.weight(1f))
        StatusPill(viewModel)
        Spacer(Modifier.width(8.dp))
        OnlineToggle(enabled = viewModel.onlineEnabled, onClick = viewModel::toggleOnline)
    }
}

@Composable
private fun AppMark() {
    val palette = LocalGounmalPalette.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(10.dp, RoundedCornerShape(12.dp), ambientColor = palette.accent.copy(alpha = 0.4f), spotColor = palette.accent.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4F8DFF), Color(0xFF1B4BE0)))),
        contentAlignment = Alignment.Center,
    ) {
        CheckIcon(color = Color.White, size = 18.dp)
    }
}

@Composable
private fun StatusPill(viewModel: EditorViewModel) {
    val palette = LocalGounmalPalette.current
    val count = viewModel.issueCount
    val checking = viewModel.isChecking || viewModel.onlineState == OnlineState.CHECKING

    val color = when {
        count > 0 -> palette.accent
        checking -> palette.textSecondary
        else -> palette.success
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = if (palette.isDark) 0.18f else 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checking) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.6.dp,
                color = color,
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = when {
                count > 0 -> "${count}곳"
                checking -> "검사 중"
                else -> "이상 없음"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
private fun OnlineToggle(enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalGounmalPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) palette.accentSoft else palette.surfaceMuted)
            .border(
                1.dp,
                if (enabled) palette.accent.copy(alpha = 0.25f) else palette.outline,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SparkIcon(color = if (enabled) palette.accent else palette.textTertiary, size = 11.dp)
        Spacer(Modifier.width(5.dp))
        Text(
            text = "정밀",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) palette.accent else palette.textTertiary,
        )
    }
}

// ------------------------------------------------------------------ 본문

@Composable
private fun EditorCard(
    viewModel: EditorViewModel,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGounmalPalette.current
    val shape = RoundedCornerShape(24.dp)
    val scroll = rememberScrollState()

    // 카드 안쪽 여백은 텍스트 레이아웃 좌표에 영향을 주지 않도록 바깥에서 준다.
    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = palette.bubbleShadow,
                spotColor = palette.bubbleShadow,
            )
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.outline.copy(alpha = 0.8f), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "본문",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                    ),
                    color = palette.textTertiary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${viewModel.text.text.length}자",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll),
            ) {
                HighlightEditor(
                    value = viewModel.text,
                    onValueChange = viewModel::onTextChange,
                    issues = viewModel.issues,
                    activeIssueId = viewModel.activeIssue?.id,
                    flashRanges = viewModel.flashRanges,
                    flashToken = viewModel.flashToken,
                    onIssueTap = viewModel::selectIssue,
                    onShiftPressed = viewModel::onShiftTap,
                    focusRequester = focusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .padding(bottom = 6.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostText(text = "예시 넣기", onClick = viewModel::insertSample)
                Spacer(Modifier.width(4.dp))
                GhostText(text = "지우기", onClick = viewModel::clearText)
                Spacer(Modifier.weight(1f))
                if (viewModel.canUndo) {
                    GhostText(
                        text = "되돌리기",
                        onClick = viewModel::undo,
                        icon = { UndoIcon(color = palette.textTertiary, size = 12.dp) },
                    )
                } else {
                    Text(
                        text = "⇧ 두 번 = 자동 교정",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textTertiary.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GhostText(
    text: String,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    val palette = LocalGounmalPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            it()
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = palette.textSecondary,
        )
    }
}

// ------------------------------------------------------------------ 교정 박스

@Composable
private fun SuggestionArea(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val palette = LocalGounmalPalette.current
    val issue = viewModel.activeIssue

    Box(modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = issue != null,
            enter = slideInVertically(animationSpec = tween(260)) { it / 3 } + fadeIn(tween(200)),
            exit = slideOutVertically(animationSpec = tween(200)) { it / 4 } + fadeOut(tween(150)),
        ) {
            issue?.let {
                SuggestionCard(
                    issue = it,
                    totalIssues = viewModel.issueCount,
                    onlineState = viewModel.onlineState,
                    canUndo = viewModel.canUndo,
                    onApply = { replacement -> viewModel.applyIssue(it, replacement) },
                    onApplyAll = viewModel::applyAllIssues,
                    onDismissIssue = { viewModel.dismissIssue(it) },
                    onUndo = viewModel::undo,
                    onClose = viewModel::suppressBubble,
                )
            }
        }

        AnimatedVisibility(
            visible = issue == null,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(120)),
        ) {
            IdleCard(viewModel = viewModel)
        }
    }
}

@Composable
private fun IdleCard(viewModel: EditorViewModel) {
    val palette = LocalGounmalPalette.current
    val shape = RoundedCornerShape(22.dp)
    val failed = viewModel.onlineState == OnlineState.FAILED
    val checking = viewModel.onlineState == OnlineState.CHECKING || viewModel.isChecking

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                12.dp,
                shape,
                ambientColor = palette.bubbleShadow,
                spotColor = palette.bubbleShadow,
            )
            .clip(shape)
            .background(palette.bubble)
            .border(1.dp, palette.outline.copy(alpha = 0.9f), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    (if (failed) palette.textTertiary else palette.success).copy(alpha = 0.14f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                checking -> CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.7.dp,
                    color = palette.success,
                )
                else -> CheckIcon(color = palette.success, size = 14.dp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = when {
                    checking -> "정밀 검사 중…"
                    failed -> "온라인 검사에 연결하지 못했어요"
                    else -> "고칠 곳이 없어요"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = palette.textPrimary,
            )
            Text(
                text = when {
                    checking -> "잠시만 기다려 주세요"
                    failed -> "오프라인 검사는 계속 동작합니다"
                    else -> "문장을 더 입력하면 바로 확인해 드릴게요"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = palette.textTertiary,
            )
        }
        if (failed) {
            GhostText(text = "다시 시도", onClick = viewModel::retryOnlineCheck)
        } else if (!viewModel.onlineEnabled) {
            GhostText(text = "정밀 검사 켜기", onClick = viewModel::retryOnlineCheck)
        }
    }
}

// ------------------------------------------------------------------ 알림

@Composable
private fun BannerHost(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val palette = LocalGounmalPalette.current
    val banner = viewModel.banner

    Box(modifier = modifier.padding(top = 64.dp)) {
        AnimatedVisibility(
            visible = banner != null,
            enter = slideInVertically(tween(260)) { -it / 2 } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(150)),
        ) {
            banner?.let { current ->
                Row(
                    modifier = Modifier
                        .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = palette.bubbleShadow, spotColor = palette.bubbleShadow)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.accentStrong)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CheckIcon(color = Color.White, size = 14.dp)
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = Color.White,
                    )
                    if (current.undoable) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable {
                                    viewModel.undo()
                                    viewModel.dismissBanner()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "되돌리기",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
