package com.example.pokedex.components.fallbackStates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.pokedex.TestTags

@Composable
fun ErrorComponent(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            modifier = Modifier.testTag(TestTags.errorMessage)
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(16.dp)
                .testTag(TestTags.retryButton)
        ) {
            Text("Try again")
        }
    }
}