package com.example.nestworth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nestworth.Repository.settings.Currency
import com.example.nestworth.Repository.settings.ThemeMode
import com.example.nestworth.achievement.StartingStep
import com.example.nestworth.core.LocalAppSettings
import com.example.nestworth.core.TutorialEnum
import com.example.nestworth.ui.utils.SurfaceActionRow
import com.example.nestworth.ui.utils.SurfaceGroup
import com.example.nestworth.ui.utils.SurfaceRowDivider
import com.example.nestworth.ui.utils.SurfaceSectionHeader
import com.example.nestworth.ui.utils.SurfaceValueRowClick
import com.example.nestworth.ui.viewmodel.MainViewModel
import com.example.nestworth.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    openTutorial: (TutorialEnum) -> Unit,
    openExpenseCategories: () -> Unit
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val versionName = context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName
    val currentProfile by mainViewModel.latestProfile.collectAsState()
    val profile = currentProfile ?: return  // local val, smart-cast works fine
    val categories by mainViewModel.allExpenseCategories.collectAsState()
    val firstEmojisText =
        if (categories.isNotEmpty()) (categories.take(3).joinToString(" / ") { it.emoji })
        else "❓"

    var currencyExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        // Top title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                modifier = Modifier.weight(0.1f),
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.8f)
            )
            Box(modifier = Modifier.weight(0.1f))
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // Preferences
            SurfaceSectionHeader("Preferences")
            SurfaceGroup {
                Box {
                    SurfaceValueRowClick(
                        label = "App theme",
                        values = listOf(Pair(settings.themeMode.name, MaterialTheme.colorScheme.primary)),
                        onClick = { themeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        ThemeMode.entries.forEach { themeMode ->
                            DropdownMenuItem(
                                text = { Text(themeMode.name) },
                                onClick = {
                                    settingsViewModel.setTheme(themeMode)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }
                SurfaceRowDivider()
                Box {
                    SurfaceValueRowClick(
                        label = "Currency",
                        values = listOf(Pair(settings.currency.name, MaterialTheme.colorScheme.primary)),
                        onClick = { currencyExpanded = true }
                    )
                    DropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        Currency.entries.forEach { currency ->
                            if (currency != Currency.PERCENTAGE) {
                                DropdownMenuItem(
                                    text = { Text(currency.name) },
                                    onClick = {
                                        settingsViewModel.setCurrency(currency)
                                        currencyExpanded = false
                                        // Unlock achievement the first time currency is chosen
                                        if (profile.startingSteps and StartingStep.CURRENCY_SELECTED.mask == 0) {
                                            mainViewModel.updateProfile(
                                                profile,
                                                profile.name,
                                                profile.xpAmount,
                                                profile.xpLevel,
                                                profile.achievements,
                                                profile.dailyStreak,
                                                profile.lastLogin,
                                                startingSteps = profile.startingSteps or StartingStep.CURRENCY_SELECTED.mask
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                SurfaceRowDivider()
                SurfaceValueRowClick(
                    label = "Expense categories",
                    values = listOf(
                        Pair(firstEmojisText, MaterialTheme.colorScheme.primary)
                    ),
                    onClick = { openExpenseCategories() }
                )
            }

            // Guides
            SurfaceSectionHeader("Guides")
            SurfaceGroup {
                SurfaceActionRow(
                    label = "Home",
                    onClick = { openTutorial(TutorialEnum.HOME) }
                )
                SurfaceRowDivider()
                SurfaceActionRow(
                    label = "Assets",
                    onClick = { openTutorial(TutorialEnum.ASSETS) }
                )
                SurfaceRowDivider()
                SurfaceActionRow(
                    label = "Expenses / income",
                    onClick = { openTutorial(TutorialEnum.INCOME_EXPENSES) }
                )
            }

            // About
            SurfaceSectionHeader("About")
            SurfaceGroup {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Version",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingContent = {
                        Text(
                            text = versionName!!,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        }
    }
}
