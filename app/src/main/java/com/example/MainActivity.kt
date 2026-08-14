package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.api.ApiClient
import com.example.data.api.NativeScraper
import com.example.data.local.AppDatabase
import com.example.data.model.Movie
import com.example.data.repository.MovieRepository
import com.example.data.repository.SettingsRepository
import com.example.ui.components.CloudMoviesBottomNav
import com.example.ui.components.NavTab
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DetailViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.WatchlistViewModel

sealed class Screen {
    data class MainTabs(val tab: NavTab = NavTab.HOME) : Screen()
    data class Detail(val slug: String) : Screen()
    data class Player(val slug: String, val server: String = "cast", val episodeSlug: String? = null) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(this)
        val settingsRepository = SettingsRepository(this)
        val apiClient = ApiClient(settingsRepository)
        val nativeScraper = NativeScraper(apiClient.okHttpClient)
        val movieRepository = MovieRepository(apiClient, database.movieDao(), nativeScraper)

        setContent {
            MyApplicationTheme {
                CloudMoviesApp(
                    movieRepository = movieRepository,
                    settingsRepository = settingsRepository,
                    apiClient = apiClient
                )
            }
        }
    }
}

@Composable
fun CloudMoviesApp(
    movieRepository: MovieRepository,
    settingsRepository: SettingsRepository,
    apiClient: ApiClient,
    modifier: Modifier = Modifier
) {
    var screenStack by remember { mutableStateOf<List<Screen>>(listOf(Screen.MainTabs(NavTab.HOME))) }
    val currentScreen = screenStack.lastOrNull() ?: Screen.MainTabs(NavTab.HOME)

    var currentTab by remember { mutableStateOf(NavTab.HOME) }

    // Global ViewModels
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(movieRepository)
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.provideFactory(movieRepository)
    )
    val watchlistViewModel: WatchlistViewModel = viewModel(
        factory = WatchlistViewModel.provideFactory(movieRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(settingsRepository, movieRepository)
    )

    fun navigateTo(screen: Screen) {
        screenStack = screenStack + screen
    }

    fun popBack(): Boolean {
        return if (screenStack.size > 1) {
            screenStack = screenStack.dropLast(1)
            true
        } else {
            false
        }
    }

    BackHandler(enabled = screenStack.size > 1) {
        popBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialBackground)
    ) {
        when (currentScreen) {
            is Screen.MainTabs -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = EditorialBackground,
                    bottomBar = {
                        CloudMoviesBottomNav(
                            currentTab = currentTab,
                            onTabSelected = { selected ->
                                currentTab = selected
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            NavTab.HOME -> HomeScreen(
                                viewModel = homeViewModel,
                                onMovieClick = { movie ->
                                    navigateTo(Screen.Detail(movie.cleanSlug))
                                },
                                onPlayMovie = { slug, server ->
                                    navigateTo(Screen.Player(slug, server ?: "cast"))
                                },
                                onContinueHistoryClick = { history ->
                                    navigateTo(Screen.Player(history.slug, history.server))
                                },
                                onSearchClick = {
                                    currentTab = NavTab.SEARCH
                                },
                                onSettingsClick = {
                                    currentTab = NavTab.SETTINGS
                                }
                            )
                            NavTab.SEARCH -> SearchScreen(
                                viewModel = searchViewModel,
                                onMovieClick = { movie ->
                                    navigateTo(Screen.Detail(movie.cleanSlug))
                                }
                            )
                            NavTab.WATCHLIST -> WatchlistScreen(
                                viewModel = watchlistViewModel,
                                onMovieClick = { movie ->
                                    navigateTo(Screen.Detail(movie.cleanSlug))
                                },
                                onPlayClick = { slug, server, epSlug ->
                                    navigateTo(Screen.Player(slug, server ?: "cast", epSlug))
                                }
                            )
                            NavTab.SETTINGS -> SettingsScreen(
                                viewModel = settingsViewModel
                            )
                        }
                    }
                }
            }

            is Screen.Detail -> {
                val detailViewModel: DetailViewModel = viewModel(
                    key = "detail_${currentScreen.slug}",
                    factory = DetailViewModel.provideFactory(movieRepository, currentScreen.slug)
                )
                DetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { popBack() },
                    onPlayClick = { slug, server, epSlug ->
                        navigateTo(Screen.Player(slug, server ?: "cast", epSlug))
                    },
                    onMovieClick = { movie ->
                        navigateTo(Screen.Detail(movie.cleanSlug))
                    }
                )
            }

            is Screen.Player -> {
                val playerViewModel: PlayerViewModel = viewModel(
                    key = "player_${currentScreen.slug}_${currentScreen.server}_${currentScreen.episodeSlug}",
                    factory = PlayerViewModel.provideFactory(
                        repository = movieRepository,
                        apiClient = apiClient,
                        slug = currentScreen.slug,
                        server = currentScreen.server,
                        episodeSlug = currentScreen.episodeSlug
                    )
                )
                PlayerScreen(
                    viewModel = playerViewModel,
                    onBackClick = { popBack() }
                )
            }
        }
    }
}
