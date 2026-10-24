package com.arena.gounmal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.gounmal.ui.theme.LocalGounmalPalette

/**
 * 앱 안에서 바로 쓰는 키보드.
 *
 * 시스템 키보드/설정에 기대지 않아도 되도록 한글 자모를 직접 조합하고,
 * ⇧ 키를 두 번 누르면 지금 화면의 오류를 한 번에 고친다.
 */
@Composable
fun GounmalKeyboard(
    onChar: (Char) -> Unit,
    onText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onShift: () -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 46.dp,
) {
    val palette = LocalGounmalPalette.current
    var hangul by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    fun tap(action: () -> Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }

    val hangulRows = listOf(
        listOf("ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ", "ㅛ", "ㅕ", "ㅑ", "ㅐ", "ㅔ"),
        listOf("ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ"),
    )
    val latinRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    )
    val thirdRow = if (hangul) {
        listOf("ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ")
    } else {
        listOf("z", "x", "c", "v", "b", "n", "m")
    }
    val rows = if (hangul) hangulRows + listOf(thirdRow) else latinRows + listOf(thirdRow)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.backgroundElevated.copy(alpha = 0.72f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                row.forEach { label ->
                    LetterKey(
                        label = label,
                        weight = 1f,
                        height = keyHeight,
                        onClick = { tap { onChar(label[0]) } },
                    )
                }
                if (index == rows.lastIndex) {
                    FunctionKey(
                        weight = 1.5f,
                        height = keyHeight,
                        onClick = { tap(onBackspace) },
                    ) {
                        BackspaceIcon(color = palette.textSecondary)
                    }
                }
            }
        }

        // 마지막 줄: ⇧ · 한/영 · 스페이스 · 마침표 · 쉼표 · 줄바꿈
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FunctionKey(
                weight = 1.5f,
                height = keyHeight,
                emphasized = true,
                onClick = { tap(onShift) },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShiftIcon(color = palette.accent, size = 17.dp)
                    Text(
                        text = "2×",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = palette.accent.copy(alpha = 0.75f),
                    )
                }
            }
            FunctionKey(
                weight = 1.2f,
                height = keyHeight,
                onClick = { tap { hangul = !hangul } },
            ) {
                Text(
                    text = if (hangul) "한" else "A",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = palette.textSecondary,
                )
            }
            SpaceKey(
                weight = 4.4f,
                height = keyHeight,
                onClick = { tap { onText(" ") } },
            )
            LetterKey(
                label = ".",
                weight = 0.9f,
                height = keyHeight,
                onClick = { tap { onText(".") } },
            )
            LetterKey(
                label = ",",
                weight = 0.9f,
                height = keyHeight,
                onClick = { tap { onText(",") } },
            )
            FunctionKey(
                weight = 1.5f,
                height = keyHeight,
                onClick = { tap(onEnter) },
            ) {
                EnterIcon(color = palette.textSecondary)
            }
        }
    }
}

@Composable
private fun RowScope.LetterKey(
    label: String,
    weight: Float,
    height: Dp,
    onClick: () -> Unit,
) {
    val palette = LocalGounmalPalette.current
    KeySurface(weight = weight, height = height, onClick = onClick) { pressed ->
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = if (pressed) palette.accent else palette.textPrimary,
        )
    }
}

@Composable
private fun RowScope.FunctionKey(
    weight: Float,
    height: Dp,
    emphasized: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val palette = LocalGounmalPalette.current
    KeySurface(
        weight = weight,
        height = height,
        onClick = onClick,
        container = when {
            emphasized -> palette.accentSoft
            else -> palette.surfaceMuted
        },
        borderColor = if (emphasized) palette.accent.copy(alpha = 0.18f) else palette.outline,
    ) {
        content()
    }
}

@Composable
private fun RowScope.SpaceKey(weight: Float, height: Dp, onClick: () -> Unit) {
    val palette = LocalGounmalPalette.current
    KeySurface(weight = weight, height = height, onClick = onClick) { pressed ->
        Canvas(Modifier.size(width = 26.dp, height = 4.dp)) {
            drawRoundRect(
                color = if (pressed) palette.accent else palette.textTertiary.copy(alpha = 0.55f),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
            )
        }
    }
}

@Composable
private fun RowScope.KeySurface(
    weight: Float,
    height: Dp,
    onClick: () -> Unit,
    container: Color? = null,
    borderColor: Color? = null,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    val palette = LocalGounmalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "keyScale",
    )
    val background by animateColorAsState(
        targetValue = when {
            pressed -> palette.accentSoft
            container != null -> container
            else -> palette.surface
        },
        label = "keyBg",
    )
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
            .scale(scale)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor ?: palette.outline, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content(pressed)
    }
}
