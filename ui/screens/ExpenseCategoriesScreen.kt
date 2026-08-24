package com.example.nestworth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.example.nestworth.Repository.model.ExpenseCategory
import com.example.nestworth.ui.components.AddExpenseCategoryDialog
import com.example.nestworth.ui.components.LogExpenseSheet
import com.example.nestworth.ui.components.LogIncomeSheet
import com.example.nestworth.ui.utils.SurfaceGroup
import com.example.nestworth.ui.utils.SurfaceRowDivider
import com.example.nestworth.ui.utils.SurfaceValueRowClick
import com.example.nestworth.ui.viewmodel.MainViewModel

sealed class CategoriesActiveDialogType {
    data object None : AssetsActiveDialogType()
    data object AddCategory : AssetsActiveDialogType()
    data class EditCategory(val category: ExpenseCategory) : AssetsActiveDialogType()
}

@Composable
fun ExpenseCategoriesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.allExpenseCategories.collectAsState()
    var activeDialog by remember { mutableStateOf<AssetsActiveDialogType>(AssetsActiveDialogType.None) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        // Top title bar
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .weight(0.10f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            // Back button
            IconButton(
                modifier = Modifier.weight(0.1f),
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Add data point",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Asset name
            Text(
                modifier = Modifier.weight(0.8f),
                text = "Expense Categories",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            // Empty box to align text to center
            Box(modifier = Modifier.weight(0.1f))
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        // List of expense categories
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(0.8f)
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            SurfaceGroup {
                LazyColumn (
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(categories) { category ->
                        SurfaceValueRowClick(
                            label = category.name,
                            values = listOf(Pair(category.emoji, MaterialTheme.colorScheme.primary)),
                            onClick = { activeDialog = CategoriesActiveDialogType.EditCategory(category) },
                        )
                        if (category != categories.last()) {
                            SurfaceRowDivider()
                        }
                    }
                }
            }
        }

        // Add new category
        Button(
            onClick = { activeDialog = CategoriesActiveDialogType.AddCategory },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.10f)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text("Add new category")
        }
    }

    // Check current active dialog
    if (activeDialog != CategoriesActiveDialogType.None) {
        when (val dialog = activeDialog) {
            is CategoriesActiveDialogType.AddCategory -> {
                AddExpenseCategoryDialog (
                    onDismiss = { activeDialog = CategoriesActiveDialogType.None },
                    onConfirm = { name, emoji ->
                        viewModel.addExpenseCategory(name, emoji)
                        activeDialog = CategoriesActiveDialogType.None
                    },
                    onDelete = { activeDialog = CategoriesActiveDialogType.None }
                )
            }
            is CategoriesActiveDialogType.EditCategory -> {
                val category = dialog.category
                AddExpenseCategoryDialog (
                    onDismiss = { activeDialog = CategoriesActiveDialogType.None },
                    onConfirm = { name, emoji ->
                        viewModel.updateExpenseCategory(category, name, emoji)
                        activeDialog = CategoriesActiveDialogType.None
                    },
                    onDelete = {
                        viewModel.deleteExpenseCategory(category)
                        activeDialog = CategoriesActiveDialogType.None },
                    existingCategory = category
                )}
            else -> {}
        }

    }

}