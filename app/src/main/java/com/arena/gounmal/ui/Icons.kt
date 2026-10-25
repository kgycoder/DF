package com.arena.gounmal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 아이콘은 모두 직접 그린다.
 * (아이콘 라이브러리 의존성 없이, 둥근 선 두께를 원하는 대로 맞추기 위해)
 */
@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val pad = s * 0.28f
        val w = s * 0.115f
        drawLine(color, Offset(pad, pad), Offset(s - pad, s - pad), w, StrokeCap.Round)
        drawLine(color, Offset(s - pad, pad), Offset(pad, s - pad), w, StrokeCap.Round)
    }
}

@Composable
fun CheckIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.22f, s * 0.52f)
            lineTo(s * 0.42f, s * 0.72f)
            lineTo(s * 0.80f, s * 0.30f)
        }
        drawPath(path, color, style = Stroke(width = s * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ArrowRightIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.18f, s * 0.5f)
            lineTo(s * 0.80f, s * 0.5f)
        }
        drawPath(path, color, style = Stroke(width = s * 0.11f, cap = StrokeCap.Round))
        val head = Path().apply {
            moveTo(s * 0.58f, s * 0.28f)
            lineTo(s * 0.82f, s * 0.5f)
            lineTo(s * 0.58f, s * 0.72f)
        }
        drawPath(head, color, style = Stroke(width = s * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** ⇧ 모양. 하드웨어 키보드의 Shift 와 같은 실루엣. */
@Composable
fun ShiftIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp, filled: Boolean = false) {
    Canvas(modifier.size(size)) { drawShift(color, this.size.minDimension, filled) }
}

private fun DrawScope.drawShift(color: Color, s: Float, filled: Boolean) {
    val path = Path().apply {
        moveTo(s * 0.5f, s * 0.12f)
        lineTo(s * 0.93f, s * 0.55f)
        lineTo(s * 0.70f, s * 0.55f)
        lineTo(s * 0.70f, s * 0.86f)
        lineTo(s * 0.30f, s * 0.86f)
        lineTo(s * 0.30f, s * 0.55f)
        lineTo(s * 0.07f, s * 0.55f)
        close()
    }
    if (filled) drawPath(path, color) else
        drawPath(path, color, style = Stroke(width = s * 0.1f, join = StrokeJoin.Round, cap = StrokeCap.Round))
}

@Composable
fun BackspaceIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.085f
        // 본체: 왼쪽이 뾰족한 둥근 사각형
        val body = Path().apply {
            moveTo(s * 0.40f, s * 0.24f)
            lineTo(s * 0.86f, s * 0.24f)
            lineTo(s * 0.86f, s * 0.76f)
            lineTo(s * 0.40f, s * 0.76f)
            lineTo(s * 0.10f, s * 0.50f)
            close()
        }
        drawPath(body, color, style = Stroke(width = w, join = StrokeJoin.Round, cap = StrokeCap.Round))
        // ×
        drawLine(color, Offset(s * 0.48f, s * 0.40f), Offset(s * 0.67f, s * 0.60f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.67f, s * 0.40f), Offset(s * 0.48f, s * 0.60f), w, StrokeCap.Round)
    }
}

@Composable
fun EnterIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.82f, s * 0.24f)
            lineTo(s * 0.82f, s * 0.58f)
            lineTo(s * 0.24f, s * 0.58f)
        }
        drawPath(path, color, style = Stroke(width = s * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val head = Path().apply {
            moveTo(s * 0.40f, s * 0.40f)
            lineTo(s * 0.20f, s * 0.58f)
            lineTo(s * 0.40f, s * 0.76f)
        }
        drawPath(head, color, style = Stroke(width = s * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun UndoIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val topLeft = Offset(s * 0.22f, s * 0.42f)
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(s * 0.58f, s * 0.58f),
            style = Stroke(width = s * 0.11f, cap = StrokeCap.Round),
        )
        val head = Path().apply {
            moveTo(s * 0.24f, s * 0.22f)
            lineTo(s * 0.24f, s * 0.46f)
            lineTo(s * 0.46f, s * 0.42f)
            close()
        }
        drawPath(head, color)
    }
}

@Composable
fun SparkIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.05f)
            lineTo(s * 0.62f, s * 0.38f)
            lineTo(s * 0.95f, s * 0.5f)
            lineTo(s * 0.62f, s * 0.62f)
            lineTo(s * 0.5f, s * 0.95f)
            lineTo(s * 0.38f, s * 0.62f)
            lineTo(s * 0.05f, s * 0.5f)
            lineTo(s * 0.38f, s * 0.38f)
            close()
        }
        drawPath(path, color)
    }
}
