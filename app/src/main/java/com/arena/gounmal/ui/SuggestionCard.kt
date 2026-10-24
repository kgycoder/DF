package com.arena.gounmal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.gounmal.OnlineState
import com.arena.gounmal.spell.Issue
import com.arena.gounmal.spell.IssueKind
import com.arena.gounmal.spell.IssueSource
import com.arena.gounmal.ui.theme.LocalGounmalPalette

/**
 * "맞춤법 교정 내용 박스".
 *
 * 하이라이트된 곳을 누르거나 커서가 근처에 가면 화면 아래에서 부드럽게 올라와
 * 무엇이 문제이고, 무엇으로 고치면 되는지, 왜 그런지를 한 번에 보여 준다.
 */
@Composable
fun SuggestionCard(
    issue: Issue,
    totalIssues: Int,
    onlineState: OnlineState,
    canUndo: Boolean,
    onApply: (String) -> Unit,
    onApplyAll: () -> Unit,
    onDismissIssue: () -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGounmalPalette.current
    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = palette.accent.copy(alpha = 0.28f),
                spotColor = palette.accent.copy(alpha = 0.28f),
            )
            .clip(shape)
            .background(palette.bubble)
            .border(1.dp, palette.outline.copy(alpha = 0.9f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        // ---------------------------------------------------------- 헤더
        Row(verticalAlignment = Alignment.CenterVertically) {
            KindPill(issue.kind)
            if (issue.source == IssueSource.ONLINE) {
                Spacer(Modifier.width(6.dp))
                SourcePill()
            }
            Spacer(Modifier.weight(1f))
            if (onlineState == OnlineState.CHECKING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.8.dp,
                    color = palette.accent.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(8.dp))
            }
            SoftIconButton(onClick = onClose) {
                CloseIcon(color = palette.textSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------------------------------------------------------- 원문 → 대치어
        Row(verticalAlignment = Alignment.CenterVertically) {
            WordChip(
                text = issue.original,
                container = palette.danger.copy(alpha = if (palette.isDark) 0.14f else 0.07f),
                border = palette.danger.copy(alpha = 0.22f),
                content = palette.danger,
                strike = true,
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(palette.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                ArrowRightIcon(color = palette.accent)
            }
            WordChip(
                text = issue.primarySuggestion,
                container = palette.accent.copy(alpha = if (palette.isDark) 0.18f else 0.10f),
                border = palette.accent.copy(alpha = 0.34f),
                content = palette.accentStrong,
                bold = true,
            )
        }

        // ---------------------------------------------------------- 다른 후보
        val alternatives = issue.suggestions.drop(1)
        if (alternatives.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "다른 후보",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                )
                alternatives.forEach { candidate ->
                    SmallChip(text = candidate, onClick = { onApply(candidate) })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------------------------------------------------------- 설명
        Text(
            text = issue.message,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))

        // ---------------------------------------------------------- 동작
        Row(verticalAlignment = Alignment.CenterVertically) {
            GhostAction(text = "무시", onClick = onDismissIssue)
            if (canUndo) {
                Spacer(Modifier.width(6.dp))
                GhostAction(text = "되돌리기", onClick = onUndo, icon = {
                    UndoIcon(color = palette.textSecondary, size = 14.dp)
                })
            }
            Spacer(Modifier.weight(1f))
            PrimaryAction(
                text = "고치기",
                onClick = { onApply(issue.primarySuggestion) },
            )
        }

        if (totalIssues > 1) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShiftHint()
                Spacer(Modifier.weight(1f))
                GhostAction(
                    text = "모두 고치기 ($totalIssues)",
                    onClick = onApplyAll,
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun KindPill(kind: IssueKind) {
    val palette = LocalGounmalPalette.current
    val color = when (kind) {
        IssueKind.SPELLING -> palette.accent
        IssueKind.SPACING -> Color(0xFF3F8F7A)
        IssueKind.GRAMMAR -> Color(0xFF8A63D2)
        IssueKind.PUNCTUATION -> Color(0xFFCE7A2E)
        IssueKind.STYLE -> Color(0xFF3E86A8)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = if (palette.isDark) 0.22f else 0.12f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = kind.label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            ),
            color = color,
        )
    }
}

@Composable
private fun SourcePill() {
    val palette = LocalGounmalPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surfaceMuted)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SparkIcon(color = palette.textTertiary, size = 10.dp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = "정밀 검사",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = palette.textTertiary,
        )
    }
}

@Composable
private fun WordChip(
    text: String,
    container: Color,
    border: Color,
    content: Color,
    strike: Boolean = false,
    bold: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(container)
            .border(1.dp, border, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
                textDecoration = if (strike) TextDecoration.LineThrough else TextDecoration.None,
                fontSize = 17.sp,
            ),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SmallChip(text: String, onClick: () -> Unit) {
    val palette = LocalGounmalPalette.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(palette.surfaceMuted)
            .border(1.dp, palette.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = palette.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun SoftIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val palette = LocalGounmalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconScale",
    )
    Box(
        modifier = Modifier
            .size(30.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(palette.surfaceMuted)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    val palette = LocalGounmalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "primaryScale",
    )
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.accent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckIcon(color = Color.White, size = 15.dp)
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            color = Color.White,
        )
    }
}

@Composable
private fun GhostAction(
    text: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    val palette = LocalGounmalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container by animateColorAsState(
        targetValue = when {
            pressed -> palette.accent.copy(alpha = 0.14f)
            emphasized -> palette.accentSoft
            else -> Color.Transparent
        },
        label = "ghostBg",
    )
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (emphasized) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            it()
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = if (emphasized) palette.accent else palette.textSecondary,
        )
    }
}

/** "⇧ 두 번이면 한 번에" 안내. */
@Composable
private fun ShiftHint() {
    val palette = LocalGounmalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(palette.accentSoft)
                .border(1.dp, palette.accent.copy(alpha = 0.24f), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ShiftIcon(color = palette.accent, size = 12.dp)
        }
        Spacer(Modifier.width(2.dp))
        Text(
            text = "×",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = palette.textTertiary,
        )
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(palette.accentSoft)
                .border(1.dp, palette.accent.copy(alpha = 0.24f), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ShiftIcon(color = palette.accent, size = 12.dp)
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = "두 번 누르면 자동 교정",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = palette.textTertiary,
        )
    }
}
