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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nestworth.Repository.settings.TimeRange
import com.example.nestworth.ui.components.CustomGraph
import com.example.nestworth.ui.components.EditDialog
import com.example.nestworth.core.LocalAppSettings
import com.example.nestworth.core.formatMoney
import com.example.nestworth.ui.utils.SurfaceGroup
import com.example.nestworth.ui.utils.SurfaceRowDivider
import com.example.nestworth.ui.utils.SurfaceSectionHeader
import com.example.nestworth.ui.utils.SurfaceValueRow
import com.example.nestworth.ui.viewmodel.MainViewModel

sealed class AssetInfoActiveDialogType {
    data object None : AssetInfoActiveDialogType()
    data object EditAsset : AssetInfoActiveDialogType()
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AssetInfoScreen(
    viewModel: MainViewModel,
    assetId: Int,
    onBack: () -> Unit,
    onEditHistory: () -> Unit
) {
    val settings = LocalAppSettings.current
    var activeDialog by remember { mutableStateOf<AssetInfoActiveDialogType>(AssetInfoActiveDialogType.None) }
    val assetsWithDatapoints by viewModel.allAssetsWithDatapoints.collectAsState()
    val assetData = assetsWithDatapoints.find { it.asset.id == assetId }
    val asset = assetData?.asset

    // Invalidity check
    if (asset == null) {
        Text("Asset not found")
        return
    }

    val sortedDatapoints = assetData.datapoints.sortedByDescending { it.date }
    val latestDatapoint = sortedDatapoints.getOrNull(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
    )
    {
        // Top info bar
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .weight(0.1f),
            verticalAlignment = Alignment.CenterVertically) {
            // Back button
            IconButton(
                modifier = Modifier.weight(0.1f),
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Asset name
            Text(
                modifier = Modifier.weight(0.8f),
                text = asset.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            // Delete button
            IconButton(
                modifier = Modifier.weight(0.1f),
                onClick = { activeDialog = AssetInfoActiveDialogType.EditAsset },
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = "Edit asset",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        if (latestDatapoint != null){
            val latestValue = latestDatapoint.value
            val latestLiability = latestDatapoint.liability
            // Display graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f)
                    .padding(horizontal = 16.dp).padding(top = 8.dp)
            ) {
                val chartDatapoints = assetData.datapoints.sortedBy { it.date } // for the chart
                SurfaceGroup {
                    CustomGraph(chartDatapoints)
                }

            }

            // Display asset summary
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .weight(0.30f)
                    .background(color = MaterialTheme.colorScheme.surface),
            ) {
                SurfaceSectionHeader("Asset summary")
                SurfaceGroup {
                    SurfaceValueRow("Equity",
                        formatMoney(latestValue - latestLiability, settings.currency)
                    )
                    SurfaceRowDivider()
                    SurfaceValueRow("Value",
                        formatMoney(latestValue, settings.currency)
                    )
                    SurfaceRowDivider()
                    SurfaceValueRow("Liability",
                        formatMoney(latestLiability, settings.currency)
                    )
                }
            }

            // Button for editing history
            Button(
                onClick = { onEditHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .weight(0.10f)
            ) {
                Text("Edit history")
            }
        }
        else
        {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .weight(0.9f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center){
                Text("No data to display",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
        }

    }

    // Check current active dialog
    if (activeDialog != AssetInfoActiveDialogType.None) {
        when (val dialog = activeDialog) {
            is AssetInfoActiveDialogType.EditAsset ->
                EditDialog (
                    currentValue = asset.name,
                    title = "asset",
                    onConfirm = { name ->
                        viewModel.updateAsset(asset, name, asset.type)  // No support for asset type yet
                        activeDialog = AssetInfoActiveDialogType.None
                    },
                    onDismiss = { activeDialog = AssetInfoActiveDialogType.None },
                    onDelete = { viewModel.deleteAsset(asset)
                        activeDialog = AssetInfoActiveDialogType.None
                        onBack()},
                )
            else -> {}
        }
    }
}