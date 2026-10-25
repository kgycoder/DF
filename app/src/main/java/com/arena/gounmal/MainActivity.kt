package com.arena.gounmal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.gounmal.ui.EditorScreen
import com.arena.gounmal.ui.theme.GounmalTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GounmalTheme {
                EditorScreen(viewModel())
            }
        }
    }
}
