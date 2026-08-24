package com.example.nestworth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nestworth.core.TutorialContent

// A tutorial body is written as chapters: "emoji Title\n\nParagraph text",
// with chapters separated by a blank line. Splitting on that structure lets
// us give each chapter's heading real visual weight instead of rendering the
// whole thing as one flat wall of text.
private data class TutorialChapter(val heading: String, val paragraph: String)

private fun parseChapters(body: String): List<TutorialChapter> =
    body.split(Regex("\n{3,}"))
        .mapNotNull { chunk ->
            val trimmed = chunk.trim('\n')
            if (trimmed.isBlank()) return@mapNotNull null
            val parts = trimmed.split("\n\n", limit = 2)
            if (parts.size == 2) {
                TutorialChapter(heading = parts[0].trim(), paragraph = parts[1].trim())
            } else {
                TutorialChapter(heading = "", paragraph = trimmed)
            }
        }

@Composable
fun TutorialPage(
    tutorialContent: TutorialContent,
    onBack: () -> Unit
) {
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
            // Back button
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
                text = tutorialContent.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.8f)
            )
            // Empty box to align text to center
            Box(modifier = Modifier.weight(0.1f))
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        val chapters = parseChapters(tutorialContent.body)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            chapters.forEachIndexed { index, chapter ->
                if (chapter.heading.isNotBlank()) {
                    Text(
                        text = chapter.heading,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = chapter.paragraph,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = if (chapter.heading.isNotBlank()) 6.dp else 0.dp)
                )
                if (index != chapters.lastIndex) {
                    Box(modifier = Modifier.padding(bottom = 24.dp))
                }
            }
        }
    }
}