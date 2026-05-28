package com.example.pokedex.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pokedex.components.FallbackStates.EmptyComponent
import com.example.pokedex.components.FallbackStates.ErrorComponent
import com.example.pokedex.components.FallbackStates.LoadingComponent
import com.example.pokedex.components.ListPokemonItem
import com.example.pokedex.models.ApiResult
import com.example.pokedex.models.FilterMode
import com.example.pokedex.models.LoadingState

@Composable
fun ListScreen(
    list: List<ApiResult>,
    favorites: Set<String>,
    query: String,
    filter: FilterMode,
    loadingState: LoadingState,
    canLoadMore: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterChange: (FilterMode) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    open: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            FilterChip(
                selected = filter == FilterMode.All,
                onClick = { onFilterChange(FilterMode.All) },
                label = { Text("All") },
            )
            FilterChip(
                selected = filter == FilterMode.FavoritesOnly,
                onClick = { onFilterChange(FilterMode.FavoritesOnly) },
                label = { Text("Favorites") },
            )
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (list.isEmpty()) {
                item {
                    when (loadingState) {
                        LoadingState.Loading -> LoadingComponent()
                        LoadingState.Error -> ErrorComponent(onRetry)
                        LoadingState.Ok -> EmptyComponent()
                    }
                }
            } else {
                items(list, key = { it.name }) { pokemon ->
                    ListPokemonItem(
                        pokemon = pokemon,
                        onClick = { open(pokemon.name) },
                        favorite = favorites.contains(pokemon.name),
                    )
                }

                item {
                    when {
                        loadingState == LoadingState.Loading -> LoadingComponent()
                        loadingState == LoadingState.Error -> ErrorComponent(onRetry)
                        canLoadMore -> Button(onClick = onLoadMore) { Text("Load more") }
                    }
                }
            }
        }
    }
}
