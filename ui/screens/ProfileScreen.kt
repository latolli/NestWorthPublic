package com.example.nestworth.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.nestworth.R
import com.example.nestworth.achievement.AchievementCatalog
import com.example.nestworth.achievement.StartingStep
import com.example.nestworth.ui.components.DisplayTrophy
import com.example.nestworth.ui.components.EditDialog
import com.example.nestworth.ui.utils.SurfaceGroup
import com.example.nestworth.ui.utils.SurfaceRowDivider
import com.example.nestworth.ui.utils.SurfaceSectionHeader
import com.example.nestworth.ui.utils.SurfaceValueRow
import com.example.nestworth.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    profileId: Int,
    onBack: () -> Unit,
    onProfileDelete: () -> Unit) {

    val context = LocalContext.current
    val allProfiles by viewModel.allProfiles.collectAsState()
    val profile = (allProfiles?.find { it.id == profileId })?: return   // null check and smart-cast
    var showEditDialog by remember { mutableStateOf(false) }

    // Media picker
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = copyImageToInternalStorage(context, it, profile.id)

            viewModel.updateProfile(
                profile,
                profile.name,
                profile.xpAmount,
                profile.xpLevel,
                profile.achievements,
                profile.dailyStreak,
                profile.lastLogin,
                imageUri = savedPath,
                startingSteps = profile.startingSteps or StartingStep.PROFILE_PICTURE_CHANGED.mask
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.3f)
                .background(color = MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top info bar
            Row(modifier = Modifier
                .fillMaxWidth()
                .weight(0.33f)
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp, bottom = 24.dp),
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
                // Profile name
                Text(
                    modifier = Modifier.weight(0.8f),
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                // Edit button
                IconButton(
                    modifier = Modifier.weight(0.1f),
                    onClick = { showEditDialog = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Edit profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Profile picture
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f).aspectRatio(1f)
                    .clip(CircleShape)
                    .weight(0.67f)
                    .clickable {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
            ) {
                if (profile.imageUri != null) {
                    AsyncImage(
                        model = File(profile.imageUri), // or just profile.imageUri if it's a content:// string
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.default_profile_picture),
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(0.7f)
                .background(color = MaterialTheme.colorScheme.surface),
        ) {
            // Streak / Level / XP summary section
            SurfaceSectionHeader("Summary")
            SurfaceGroup {
                SurfaceValueRow("Level", "${profile.xpLevel}")
                SurfaceRowDivider()
                SurfaceValueRow("Total XP", "${profile.xpAmount}")
                SurfaceRowDivider()
                SurfaceValueRow("Daily streak", "${profile.dailyStreak}")
            }

            // Trophies section — give it a fixed or weighted height instead of fillMaxSize
            SurfaceSectionHeader("Achievements")
            SurfaceGroup {
                TrophyGrid(profile.achievements)
            }
        }
    }

    // Dialog for editing profile
    if (showEditDialog) {
        EditDialog (
            currentValue = profile.name,
            title = "profile",
            onConfirm = { name ->
                viewModel.updateProfile(profile, name, profile.xpAmount, profile.xpLevel,
                    profile.achievements, profile.dailyStreak, profile.lastLogin)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
            onDelete = { viewModel.deleteProfile(profile); showEditDialog = false; onProfileDelete() },
        )
    }
}

@Composable
fun TrophyGrid(
    unlockedAchievements: List<Int>
) {
    val allAchievements = AchievementCatalog.ALL
    val columns = 5
    val rows = (allAchievements.size + columns - 1) / columns

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rows) { rowId ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 1..columns) {
                    val gridId = rowId * columns + i
                    if (gridId <= allAchievements.size) {
                        val achievement = allAchievements[gridId - 1]
                        val isUnlocked = unlockedAchievements.contains(achievement.id)
                        DisplayTrophy(achievement.id, isUnlocked)
                    }
                }
            }
        }
    }
}

fun copyImageToInternalStorage(context: Context, uri: Uri, profileId: Int): String {
    val fileName = "profile_${profileId}_${System.currentTimeMillis()}.jpg" // Give unique name to the file to force refresh every time
    val file = File(context.filesDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return file.absolutePath
}

