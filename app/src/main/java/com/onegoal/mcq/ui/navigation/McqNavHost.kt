package com.onegoal.mcq.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onegoal.mcq.ads.InterstitialAds
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.ui.AppViewModel
import com.onegoal.mcq.ui.appContainer
import com.onegoal.mcq.ui.bookmarks.BookmarksScreen
import com.onegoal.mcq.ui.home.HomeScreen
import com.onegoal.mcq.ui.list.ContentListScreen
import com.onegoal.mcq.ui.list.ContentListViewModel
import com.onegoal.mcq.ui.pdf.PdfViewerScreen
import com.onegoal.mcq.ui.pdf.PdfViewerViewModel
import com.onegoal.mcq.ui.splash.SplashScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val BOOKMARKS = "bookmarks"
    const val LIST = "list/{item}"
    const val PDF = "pdf/{item}?section={section}&page={page}"

    fun list(itemJson: String) = "list/${Uri.encode(itemJson)}"
    fun pdf(itemJson: String, section: Int, page: Int) = "pdf/${Uri.encode(itemJson)}?section=$section&page=$page"
}

@Composable
fun McqNavHost(
    appViewModel: AppViewModel,
    interstitialAds: InterstitialAds,
    onExit: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val container = appContainer()
    val json: Json = container.json
    val scope = rememberCoroutineScope()
    val config by appViewModel.config.collectAsStateWithLifecycle()
    val bookmarkCount by appViewModel.bookmarkCount.collectAsStateWithLifecycle()
    val lastRead by appViewModel.lastRead.collectAsStateWithLifecycle()

    fun encode(item: ContentItem) = json.encodeToString(ContentItem.serializer(), item)

    fun openPdf(item: ContentItem, section: Int = 0, page: Int = 0) {
        scope.launch {
            val navigate = { navController.navigate(Routes.pdf(encode(item), section, page)) }
            if (appViewModel.registerPdfOpen()) interstitialAds.show(navigate) else navigate()
        }
    }

    fun openItem(item: ContentItem) {
        when {
            item.hasChildren -> navController.navigate(Routes.list(encode(item)))
            item.isPdf -> openPdf(item)
        }
    }

    fun goHome() {
        navController.popBackStack(Routes.HOME, inclusive = false)
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(config = config) {
                interstitialAds.preload(config.interstitialAdUnitId)
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        composable(Routes.HOME) {
            val viewModel: ContentListViewModel = viewModel(
                factory = viewModelFactory { initializer { ContentListViewModel(container.repository, parent = null) } },
            )
            HomeScreen(
                viewModel = viewModel,
                config = config,
                bookmarkCount = bookmarkCount,
                lastRead = lastRead,
                isLastReadHintShown = appViewModel::isLastReadHintShown,
                onLastReadHintShown = appViewModel::markLastReadHintShown,
                onOpenItem = ::openItem,
                onResume = { position -> openPdf(position.item, position.sectionIndex, position.page) },
                onBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                onExit = onExit,
            )
        }

        composable(Routes.BOOKMARKS) {
            val bookmarks by container.repository.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())
            BookmarksScreen(
                bookmarks = bookmarks,
                config = config,
                onOpen = { bookmark ->
                    container.repository.decodeItem(bookmark.itemJson)?.let { item ->
                        openPdf(item, bookmark.sectionIndex, bookmark.page)
                    }
                },
                onDelete = { bookmark -> scope.launch { container.repository.deleteBookmark(bookmark.id) } },
                onHome = ::goHome,
            )
        }

        composable(
            route = Routes.LIST,
            arguments = listOf(navArgument("item") { type = NavType.StringType }),
        ) { entry ->
            val item = entry.arguments?.getString("item")?.let { json.decodeFromString(ContentItem.serializer(), it) }
                ?: return@composable
            val viewModel: ContentListViewModel = viewModel(
                factory = viewModelFactory { initializer { ContentListViewModel(container.repository, parent = item) } },
            )
            ContentListScreen(
                parent = item,
                viewModel = viewModel,
                config = config,
                onBack = { navController.popBackStack() },
                onHome = ::goHome,
                onOpenItem = ::openItem,
            )
        }

        composable(
            route = Routes.PDF,
            arguments = listOf(
                navArgument("item") { type = NavType.StringType },
                navArgument("section") { type = NavType.IntType; defaultValue = 0 },
                navArgument("page") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { entry ->
            val args = entry.arguments
            val item = args?.getString("item")?.let { json.decodeFromString(ContentItem.serializer(), it) }
                ?: return@composable
            val section = args.getInt("section")
            val page = args.getInt("page")
            val viewModel: PdfViewerViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        PdfViewerViewModel(
                            item = item,
                            initialSection = section,
                            initialPage = page,
                            repository = container.repository,
                            preferences = container.preferences,
                            fileCache = container.pdfFileCache,
                        )
                    }
                },
            )
            PdfViewerScreen(
                viewModel = viewModel,
                config = config,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
