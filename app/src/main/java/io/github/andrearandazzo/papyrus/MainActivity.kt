package io.github.andrearandazzo.papyrus

import android.os.Bundle
import io.github.andrearandazzo.papyrus.ui.PapyrusNavHost
import io.github.andrearandazzo.papyrus.ui.theme.PapyrusActivity
import io.github.andrearandazzo.papyrus.ui.theme.setPapyrusContent

class MainActivity : PapyrusActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncStarterCategories()
        setPapyrusContent { PapyrusNavHost() }
    }
}
