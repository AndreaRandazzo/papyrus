package io.github.andrearandazzo.papyrus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.andrearandazzo.papyrus.ui.home.HomeScreen
import io.github.andrearandazzo.papyrus.ui.theme.PapyrusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PapyrusTheme {
                HomeScreen()
            }
        }
    }
}
