package com.example.pokedex.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pokedex.components.ListPokemonItem
import com.example.pokedex.components.fallbackStates.EmptyComponent
import com.example.pokedex.components.fallbackStates.ErrorComponent
import com.example.pokedex.components.fallbackStates.LoadingComponent
import com.example.pokedex.models.ApiResult
import com.example.pokedex.models.LoadingState

@Composable
fun ListScreen(
    list: List<ApiResult>,
    favorites: Set<String>,
    open: (String) -> Unit,
    loadingState: LoadingState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        if (list.isEmpty()) {
            item {
                when (loadingState) {
                    LoadingState.Loading -> LoadingComponent()
                    LoadingState.Error -> ErrorComponent(onRetry)
                    LoadingState.Ok -> EmptyComponent()
                }
            }
        } else {
            items(list.size) { i ->
                ListPokemonItem(
                    list[i],
                    { open(list[i].name) },
                    favorites.contains(list[i].name)
                )
            }

            item {
                when (loadingState) {
                    LoadingState.Error -> ErrorComponent(onRetry)
                    LoadingState.Ok -> Button(onClick = onLoadMore) { Text("Load more") }
                    LoadingState.Loading -> LoadingComponent()
                }
            }
        }
    }
}