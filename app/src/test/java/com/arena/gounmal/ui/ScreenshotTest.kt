package com.arena.gounmal.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.gounmal.OnlineState
import com.arena.gounmal.spell.Issue
import com.arena.gounmal.spell.OfflineSpeller
import com.arena.gounmal.ui.theme.GounmalTheme
import com.arena.gounmal.ui.theme.LocalGounmalMotion
import com.arena.gounmal.ui.theme.LocalGounmalPalette
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * 실제 컴포저블을 JVM 에서 그려 PNG 로 남긴다.
 * (기기 없이 디자인을 확인하고, README 스크린샷을 자동 갱신하기 위한 테스트)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleText = "안녕하세요! 오늘은 몇일 만에 사무실에 나갔어요. " +
        "아침부터 정신이 없어서 인사도 제대로 못했는데, 웬지 오늘은 운이 좋은것같아요. " +
        "내일은 꼭 일찍 일어나서 숙제를 끝낼께요."

    @Test
    fun lightScreen() = capture("screen-light", dark = false)

    @Test
    fun darkScreen() = capture("screen-dark", dark = true)

    private fun capture(name: String, dark: Boolean) {
        val issues = OfflineSpeller().check(sampleText)
        val active = issues.firstOrNull { it.original == "몇일" } ?: issues.first()

        composeRule.setContent {
            GounmalTheme(darkTheme = dark) {
                androidx.compose.runtime.CompositionLocalProvider(LocalGounmalMotion provides false) {
                    PreviewScreen(
                        text = sampleText,
                        issues = issues,
                        active = active,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val outDir = File("build/screenshots")
        outDir.mkdirs()
        FileOutputStream(File(outDir, "$name.png")).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    @Composable
    private fun PreviewScreen(text: String, issues: List<Issue>, active: Issue) {
        val palette = LocalGounmalPalette.current
        val caret = active.start

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "고운글",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = palette.textPrimary,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.surface)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                HighlightEditor(
                    value = TextFieldValue(text, TextRange(caret, caret)),
                    onValueChange = {},
                    issues = issues,
                    activeIssueId = active.id,
                    flashRanges = emptyList(),
                    flashToken = 0,
                    onIssueTap = {},
                    onShiftPressed = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SuggestionCard(
                issue = active,
                totalIssues = issues.size,
                onlineState = OnlineState.DONE,
                canUndo = true,
                onApply = {},
                onApplyAll = {},
                onDismissIssue = {},
                onUndo = {},
                onClose = {},
            )

            Spacer(Modifier.height(2.dp))

            GounmalKeyboard(
                onChar = {},
                onText = {},
                onBackspace = {},
                onEnter = {},
                onShift = {},
            )
        }
    }
}
