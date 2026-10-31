package io.github.andrearandazzo.papyrus.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.andrearandazzo.papyrus.ui.categories.CategoriesScreen
import io.github.andrearandazzo.papyrus.ui.detail.DocumentDetailScreen
import io.github.andrearandazzo.papyrus.ui.home.HomeScreen

private object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val DOCUMENT_ID = "documentId"
    const val DOCUMENT = "document/{$DOCUMENT_ID}"
    fun document(id: Long) = "document/$id"
}

/** All screens of the app and how to move between them. */
@Composable
fun PapyrusNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenDocument = { id -> navController.navigate(Routes.document(id)) },
                onManageCategories = { navController.navigate(Routes.CATEGORIES) },
            )
        }
        composable(
            Routes.DOCUMENT,
            arguments = listOf(navArgument(Routes.DOCUMENT_ID) { type = NavType.LongType }),
        ) {
            DocumentDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
    }
}
