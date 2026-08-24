package com.example.nestworth.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nestworth.Repository.model.Asset
import com.example.nestworth.Repository.settings.TimeRange
import com.example.nestworth.achievement.StartingStep
import com.example.nestworth.core.LocalAppSettings
import com.example.nestworth.ui.components.AddAssetDatapoint
import com.example.nestworth.ui.components.AddAssetSheet
import com.example.nestworth.ui.components.AssetCard
import com.example.nestworth.ui.components.AssetsSummary
import com.example.nestworth.ui.utils.SurfaceGroup
import com.example.nestworth.ui.utils.SurfaceRowDivider
import com.example.nestworth.ui.viewmodel.MainViewModel
import com.example.nestworth.ui.viewmodel.SettingsViewModel

sealed class AssetsActiveDialogType {
    data object None : AssetsActiveDialogType()
    data object AddAsset : AssetsActiveDialogType()
    data class AddData(val asset: Asset) : AssetsActiveDialogType()
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onAssetClick: (Asset) -> Unit = {}
) {

    val settings = LocalAppSettings.current
    var activeDialog by remember { mutableStateOf<AssetsActiveDialogType>(AssetsActiveDialogType.None) }
    val assetsWithDatapoints by viewModel.allAssetsWithDatapoints.collectAsState()
    val totalNW by viewModel.totalNetWorth.collectAsState()
    val timeRangeNWGrowth by viewModel.timeRangeNWGrowth.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val currentProfile by viewModel.latestProfile.collectAsState()
    val profile = currentProfile ?: return  // local val, smart-cast works fine
    val timeRanges = TimeRange.entries

    // Checker for the first ever asset
    val firstAssetAdded = when {
        profile.startingSteps and StartingStep.FIRST_ASSET_CREATED.mask != 0 -> true
        else -> false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        // Top summary section
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.3f)
                .padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp)
        ) {
            SurfaceGroup {
                AssetsSummary(assetsWithDatapoints, totalNW, timeRangeNWGrowth)
            }
        }

        // Asset list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(0.6f)
        ) {
            SurfaceGroup {
                // Buttons for choosing the time range
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    timeRanges.forEach { timeRange ->
                        FilterChip(
                            selected = settings.timeRange == timeRange,
                            onClick = {
                                settingsViewModel.setTimeRange(timeRange)
                            },
                            label = {
                                Text(timeRange.label)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                SurfaceRowDivider()
                // List of assets
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(assetsWithDatapoints) { assetWithDatapoints ->
                        // Apply time range filter
                        val cutOffTime = settings.timeRange.cutoffTime(System.currentTimeMillis())
                        val sortedDatapoints = assetWithDatapoints.datapoints
                            .filter { it.date >= cutOffTime }
                            .sortedByDescending { it.date }

                        // Calculate equity and growth
                        val latestDatapoint = sortedDatapoints.getOrNull(0)
                        val latestEquity = if (latestDatapoint != null) {
                            latestDatapoint.value - latestDatapoint.liability
                        } else 0.0
                        val firstDatapoint =
                            sortedDatapoints.lastOrNull() // list is descending so last = oldest
                        val firstEquity = if (firstDatapoint != null) {
                            firstDatapoint.value - firstDatapoint.liability
                        } else 0.0

                        // Display asset data
                        AssetCard(
                            asset = assetWithDatapoints.asset,
                            startEq = firstEquity ?: 0.0,
                            currentEq = latestEquity ?: 0.0,
                            onCardClick = { onAssetClick(assetWithDatapoints.asset) },
                            onAddClick = {
                                activeDialog =
                                    AssetsActiveDialogType.AddData(assetWithDatapoints.asset)
                            }
                        )

                        if (assetWithDatapoints != assetsWithDatapoints.last()) {
                            SurfaceRowDivider()
                        }
                    }
                }
            }
        }

        // Add new asset
        Button(
            onClick = { activeDialog = AssetsActiveDialogType.AddAsset },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.10f)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text("Add new asset")
        }
    }

    // Check current active dialog
    if (activeDialog != AssetsActiveDialogType.None) {
        when (val dialog = activeDialog) {
            is AssetsActiveDialogType.AddAsset -> ModalBottomSheet(
                onDismissRequest = { activeDialog = AssetsActiveDialogType.None },
                sheetState = sheetState
            ) {
                AddAssetSheet(
                onSave = { name, value, liability ->
                    viewModel.addAssetWithDatapoint(profile, name, "Other", value, liability)
                    activeDialog = AssetsActiveDialogType.None
                    if (!firstAssetAdded) {
                        viewModel.updateProfile(
                            profile,
                            profile.name,
                            profile.xpAmount,
                            profile.xpLevel,
                            profile.achievements,
                            profile.dailyStreak,
                            profile.lastLogin,
                            startingSteps = profile.startingSteps or StartingStep.FIRST_ASSET_CREATED.mask
                        )
                    }
                },
                onDismiss = { activeDialog = AssetsActiveDialogType.None }
                )
            }
            is AssetsActiveDialogType.AddData -> AddAssetDatapoint(
                asset = dialog.asset,
                onDismiss = { activeDialog = AssetsActiveDialogType.None },
                onConfirm = { value, liability, date ->
                    viewModel.addDatapoint(profile, dialog.asset, value, liability, date)
                    activeDialog = AssetsActiveDialogType.None
                }
            )
            else -> {}
        }
    }
}
