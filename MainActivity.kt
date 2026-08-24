package com.example.nestworth

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nestworth.ui.screens.HomeScreen
import com.example.nestworth.ui.screens.IntroScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nestworth.Repository.settings.ThemeMode
import com.example.nestworth.core.LocalAppSettings
import com.example.nestworth.core.tutorialPages
import com.example.nestworth.ui.components.BottomNavBar
import com.example.nestworth.ui.screens.AssetHistoryScreen
import com.example.nestworth.ui.screens.AssetInfoScreen
import com.example.nestworth.ui.screens.AssetsScreen
import com.example.nestworth.ui.screens.EventHistoryScreen
import com.example.nestworth.ui.screens.ExpenseCategoriesScreen
import com.example.nestworth.ui.screens.ProfileScreen
import com.example.nestworth.ui.screens.SettingsScreen
import com.example.nestworth.ui.screens.SignUpScreen
import com.example.nestworth.ui.screens.TutorialPage
import com.example.nestworth.ui.viewmodel.MainViewModel
import com.example.nestworth.ui.viewmodel.SettingsViewModel
import com.yourname.nestworth.ui.theme.NestWorthTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as NestWorthApp

            val mainViewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(app.database,
                            settingsRepository = app.settingsRepository) as T
                    }
                }
            )

            val settingsViewModel: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SettingsViewModel(app.settingsRepository) as T
                    }
                }
            )

            val settings by settingsViewModel.settings.collectAsState()

            CompositionLocalProvider(
                LocalAppSettings provides settings
            ) {
                NestWorthTheme(
                    darkTheme = when (settings.themeMode) {
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                    }
                ) {
                    AppNavigation(
                        viewModel = mainViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(viewModel: MainViewModel, settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val newAchievements by viewModel.achievementUnlockedEvent.collectAsState()

    if (allProfiles == null) {
        return
    }

    val startScreen = if (allProfiles!!.isEmpty()) "intro" else "home"

    LaunchedEffect(newAchievements) {
        newAchievements?.let { ids ->
            val message = if (ids.size == 1) "Achievement unlocked!" else "${ids.size} achievements unlocked!"
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.consumeAchievementEvent()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startScreen,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("intro") {
                    IntroScreen(
                        onSignUp = { navController.navigate("signUp") }
                    )
                }
                composable("signUp") {
                    SignUpScreen(
                        onConfirm = {
                            navController.navigate("home") {
                                popUpTo("intro") { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() },
                        viewModel = viewModel
                    )
                }
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onProfileClick = { profileId ->
                            navController.navigate("profile/$profileId")
                        },
                        onSettingsClick = { navController.navigate("settings") }
                    )
                }
                composable("profile/{profileId}") {
                    val profileId = it.arguments?.getString("profileId")?.toIntOrNull()
                    if (profileId != null) {
                        ProfileScreen(
                            viewModel = viewModel,
                            profileId = profileId,
                            onBack = { navController.navigate("home") },
                            onProfileDelete = { navController.navigate("intro") }
                        )
                    }
                }
                composable("assets") {
                    AssetsScreen(
                        viewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        onAssetClick = { asset -> navController.navigate("asset/${asset.id}") }
                    )
                }
                composable("asset/{assetId}") { backStackEntry ->
                    val assetId = backStackEntry.arguments?.getString("assetId")?.toIntOrNull()
                    if (assetId != null) {
                        AssetInfoScreen(
                            viewModel = viewModel,
                            assetId = assetId,
                            onBack = { navController.navigate("assets") },
                            onEditHistory = { navController.navigate("asset/${assetId}/history") }
                        )
                    }
                }
                composable("asset/{assetId}/history") { backStackEntry ->
                    val assetId = backStackEntry.arguments?.getString("assetId")?.toIntOrNull()
                    if (assetId != null) {
                        AssetHistoryScreen(
                            viewModel = viewModel,
                            assetId = assetId,
                            onBack = { navController.navigate("asset/${assetId}") }
                        )
                    }
                }
                composable("eventHistory") {
                    EventHistoryScreen(
                        viewModel = viewModel
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        mainViewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        onBack = { navController.navigate("home") },
                        openTutorial = { tutorial ->
                            navController.navigate("tutorials/${tutorial.id}")
                        },
                        openExpenseCategories = { navController.navigate("expenseCategories") }
                    )
                }
                composable("tutorials/{tutorialId}") {
                    val tutorialId = it.arguments?.getString("tutorialId")?.toIntOrNull()
                    val tutorial = tutorialPages.find { it.id == tutorialId }
                    TutorialPage(tutorial!!,
                        onBack = { navController.navigate("settings") })
                }
                composable("expenseCategories") {
                    ExpenseCategoriesScreen(
                        viewModel = viewModel,
                        onBack = { navController.navigate("settings") }
                    )
                }
            }
        }

        // Now this Box IS the scope, so .align works
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        )
    }
}