package io.github.andrearandazzo.papyrus.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.ui.backup.BackupScreen
import io.github.andrearandazzo.papyrus.ui.categories.CategoriesScreen
import io.github.andrearandazzo.papyrus.ui.detail.DocumentDetailScreen
import io.github.andrearandazzo.papyrus.ui.expiring.ExpiringScreen
import io.github.andrearandazzo.papyrus.ui.expiring.ExpiringViewModel
import io.github.andrearandazzo.papyrus.ui.home.HomeScreen
import io.github.andrearandazzo.papyrus.ui.settings.SettingsScreen

private object Routes {
    const val DOCUMENT_ID = "documentId"
    const val DOCUMENT = "document/{$DOCUMENT_ID}"
    fun document(id: Long) = "document/$id"
    const val SETTINGS = "settings"
}

/** The four destinations of the bottom navigation bar. */
private enum class Tab(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    LIBRARY("library", R.string.nav_library, R.drawable.ic_folder_open, R.drawable.ic_folder_open_filled),
    EXPIRING("expiring", R.string.nav_expiring, R.drawable.ic_event_upcoming, R.drawable.ic_event_upcoming_filled),
    CATEGORIES("categories", R.string.nav_categories, R.drawable.ic_category, R.drawable.ic_category_filled),
    BACKUP("backup", R.string.nav_backup, R.drawable.ic_shield, R.drawable.ic_shield_filled),
}

/** All screens of the app, the bottom bar, and how to move between them. */
@Composable
fun PapyrusNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // Feeds the Expiring tab's badge with the number of urgent documents.
    val expiring: ExpiringViewModel = viewModel(factory = ExpiringViewModel.Factory)
    val expiringState by expiring.state.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            // The bar is for top-level screens only; document details has its own back arrow.
            if (Tab.entries.any { it.route == currentRoute }) {
                BottomBar(currentRoute, expiringState.groups.urgentCount) { tab -> navController.switchTo(tab) }
            }
        },
    ) { padding ->
        NavHost(
            navController,
            startDestination = Tab.LIBRARY.route,
            // Each screen draws its own top bar; the inset is consumed here so it isn't added twice.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Tab.LIBRARY.route) {
                HomeScreen(
                    onOpenDocument = { id -> navController.navigate(Routes.document(id)) },
                    onOpenBackup = { navController.switchTo(Tab.BACKUP) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Tab.EXPIRING.route) {
                ExpiringScreen(onOpenDocument = { id -> navController.navigate(Routes.document(id)) })
            }
            composable(Tab.CATEGORIES.route) { CategoriesScreen() }
            composable(Tab.BACKUP.route) { BackupScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(
                Routes.DOCUMENT,
                arguments = listOf(navArgument(Routes.DOCUMENT_ID) { type = NavType.LongType }),
            ) {
                DocumentDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(currentRoute: String?, urgentCount: Int, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Tab.entries.forEach { tab ->
            val selected = tab.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    val icon = @Composable {
                        Icon(painterResource(if (selected) tab.selectedIcon else tab.icon), contentDescription = null)
                    }
                    if (tab == Tab.EXPIRING && urgentCount > 0) {
                        BadgedBox(badge = { Badge { Text(if (urgentCount > 99) "99+" else urgentCount.toString()) } }) {
                            icon()
                        }
                    } else {
                        icon()
                    }
                },
                label = {
                    Text(
                        stringResource(tab.label),
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

/** Switches tab like Android's own apps: one copy of each tab, each keeping its scroll and state. */
private fun NavHostController.switchTo(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
