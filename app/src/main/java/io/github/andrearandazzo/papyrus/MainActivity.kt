package io.github.andrearandazzo.papyrus

import android.os.Bundle
import androidx.activity.ComponentActivity
import io.github.andrearandazzo.papyrus.ui.PapyrusNavHost
import io.github.andrearandazzo.papyrus.ui.theme.setPapyrusContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPapyrusContent { PapyrusNavHost() }
    }
}
