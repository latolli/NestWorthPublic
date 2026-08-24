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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nestworth.R
import com.example.nestworth.Repository.model.Expense
import com.example.nestworth.core.LocalAppSettings
import com.example.nestworth.ui.components.EditExpenseDialog
import com.example.nestworth.core.formatMoney
import com.example.nestworth.ui.utils.SurfaceGroup
import com.example.nestworth.ui.utils.SurfaceRowDivider
import com.example.nestworth.ui.utils.SurfaceValueRowClick
import com.example.nestworth.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventHistoryScreen(viewModel: MainViewModel)
{
    val allExpenses = viewModel.allExpenses.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    val currentProfile by viewModel.latestProfile.collectAsState()
    val profile = currentProfile ?: return  // local val, smart-cast works fine
    val settings = LocalAppSettings.current

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
            Text(
                text = "Event History",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        if (allExpenses.value.isEmpty())
        {
            Box(modifier = Modifier.fillMaxWidth().weight(0.9f), contentAlignment = Alignment.Center)
            {
                Text(text = "No data to display",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
            }
        }
        else{
            // List of expenses
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .weight(0.9f)
                    .background(color = MaterialTheme.colorScheme.surface)
            ) {
                SurfaceGroup {
                    LazyColumn (
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        items(allExpenses.value) { expense ->
                            // Get all data to display
                            val isIncome = expense.isIncome
                            val displayCategory = when {
                                isIncome -> "Income"
                                else -> expense.category
                            }
                            val colorRes = if (isIncome){
                                colorResource(id = R.color.gain_green)
                            } else colorResource(id = R.color.loss_red)
                            val displayDate = remember(expense.date) {
                                Instant.ofEpochMilli(expense.date)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            }
                            SurfaceValueRowClick(
                                label = "$displayDate",
                                values = listOf(
                                    Pair(displayCategory, MaterialTheme.colorScheme.primary),
                                    Pair(formatMoney(expense.amount, settings.currency), colorRes)),
                                onClick = { showEditDialog = true
                                    selectedExpense = expense }
                            )
                            if (expense != allExpenses.value.last()) {
                                SurfaceRowDivider()
                            }
                        }
                    }
                }
            }

        }
    }

    // Dialog for editing expense
    if (showEditDialog) {
        EditExpenseDialog(
            viewModel = viewModel,
            onSave = { amount, category, note, date ->
                viewModel.updateExpense(profile, selectedExpense!!, amount, category, note, date)
                showEditDialog = false
                selectedExpense = null
            },
            onDismiss = { showEditDialog = false
                selectedExpense = null },
            onDelete = { viewModel.deleteExpense(selectedExpense!!); showEditDialog = false},
            selectedExpense = selectedExpense!!
        )
    }
}