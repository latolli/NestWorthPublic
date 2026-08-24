package com.example.nestworth.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nestworth.ui.components.XpProgressBar
import com.example.nestworth.ui.viewmodel.MainViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.nestworth.Repository.model.Profile
import com.example.nestworth.core.Constants.XP_PER_DAY
import com.example.nestworth.core.Constants.XP_PER_WEEK
import com.example.nestworth.ui.components.ApartmentView
import com.example.nestworth.ui.components.HomePageSummary
import com.example.nestworth.ui.components.LogExpenseSheet
import com.example.nestworth.ui.components.LogIncomeSheet
import com.example.nestworth.ui.components.RecentTrophiesSection
import java.time.Instant
import java.time.ZoneId


sealed class SheetType {
    data object None : SheetType()
    data object AddIncome : SheetType()
    data object AddExpense : SheetType()
}
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onProfileClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {

    var activeSheet by remember { mutableStateOf<SheetType>(SheetType.None) }
    val sheetState = rememberModalBottomSheetState()
    val currentProfile by viewModel.latestProfile.collectAsState()
    val profile = currentProfile ?: return  // local val, smart-cast works fine

    // Check daily login streak
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val lastLoginDate = Instant.ofEpochMilli(profile.lastLogin).atZone(zone).toLocalDate()
    val todayDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    val newStreak = when {
        profile.dailyStreak == 0 -> 1                                                  // first login, streak starts
        lastLoginDate == todayDate -> profile.dailyStreak                              // already logged in today, no change
        lastLoginDate.plusDays(1) == todayDate -> profile.dailyStreak + 1  // consecutive day, continue streak
        else -> 1                                                                      // missed a day+, streak resets
    }

    LaunchedEffect(profile.id, profile.lastLogin) {
        if (profile.dailyStreak != newStreak) {
            // If streak changed, update the profile with the new streak and XP values
            val newXp = when {
                newStreak % 7 == 0 -> XP_PER_WEEK
                else -> XP_PER_DAY
            }
            viewModel.updateProfile(
                profile, profile.name, (profile.xpAmount + newXp), profile.xpLevel,
                profile.achievements, newStreak, now
            )
        }
    }

    // Values needed for summary and apartment view
    val totalNW by viewModel.totalNetWorth.collectAsState()
    val networthGrowth by viewModel.netWorthGrowthLast30Days.collectAsState()
    val highestEquityAsset by viewModel.highestEquityAsset.collectAsState()
    val savingsRate by viewModel.displaySavingsRate.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        // Top section (profile icon, settings, etc)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.05f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = { onProfileClick(profile.id) }) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile"
                )
            }
            Text(text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            // TODO: Temp way to increase XP for debug purposes
            //IconButton(onClick = { TempHack(viewModel, profile, 1000) }) {
            //    Icon(Icons.Default.Settings, contentDescription = "Settings")
            IconButton(onClick = { onSettingsClick() }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        // XP bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.07f)
                .padding(horizontal = 16.dp)
        ) {
            XpProgressBar(profile)
        }

        // Apartment section
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.31f)
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            ApartmentView(profile, totalNW)
        }

        // Summary of current wealth situation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.19f)
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            HomePageSummary(totalNW, networthGrowth, highestEquityAsset, savingsRate)
        }

        // Recent trophies
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.18f)
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            RecentTrophiesSection(profile.achievements.takeLast(5).asReversed())
        }

        // Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.20f)
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            Button(
                onClick = { activeSheet = SheetType.AddIncome },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.5f)
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 10.dp)
            ) {
                Text("Log Income")
            }

            Button(
                onClick = { activeSheet = SheetType.AddExpense },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.5f)
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 10.dp)
            ) {
                Text("Log Expense")
            }
        }
    }

    // Check current active sheet
    if (activeSheet != SheetType.None) {
        when (val sheet = activeSheet) {
            is SheetType.AddIncome -> ModalBottomSheet(
                onDismissRequest = { activeSheet = SheetType.None },
                sheetState = sheetState
            ) {
                LogIncomeSheet(
                    onSave = { amount, note ->
                        viewModel.addIncome(profile, amount, note)
                        activeSheet = SheetType.None
                    },
                    onDismiss = { activeSheet = SheetType.None }
                )
            }
            is SheetType.AddExpense -> ModalBottomSheet(
                onDismissRequest = { activeSheet = SheetType.None },
                sheetState = sheetState
            ) {
                LogExpenseSheet(
                    viewModel = viewModel,
                    onSave = { amount, category, note ->
                        viewModel.addExpense(profile, amount, category, note)
                        activeSheet = SheetType.None
                    },
                    onDismiss = { activeSheet = SheetType.None }
                )
            }
            else -> {}
        }
    }
}

// TODO: TEMP function to debug stuff
fun TempHack(viewModel: MainViewModel, profile: Profile, amount: Int){
    viewModel.updateProfile(profile, profile.name, (profile.xpAmount + amount), profile.xpLevel,
        profile.achievements, profile.dailyStreak, profile.lastLogin)
    //viewModel.updateProfile(profile, profile.name, profile.xpAmount, profile.xpLevel,
    //    profile.achievements, 0, profile.lastLogin)
}
