package com.example.nestworth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nestworth.ui.viewmodel.MainViewModel

@Composable
fun SignUpScreen(
    onConfirm: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: MainViewModel
)
{
    var profileName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Create new profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = profileName,
            onValueChange = { profileName = it },
            label = { Text("Profile name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.addProfile(profileName)
                onConfirm()
            },
            enabled = profileName.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(50.dp)
        ) {
            Text("Confirm")
        }
        Button(
            onClick = {
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(50.dp)
        ) {
            Text("Cancel")
        }
    }

}