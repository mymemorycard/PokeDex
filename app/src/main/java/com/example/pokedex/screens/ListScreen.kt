package com.example.pokedex.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.pokedex.TestTags
import com.example.pokedex.components.ListPokemonItem
import com.example.pokedex.components.fallbackStates.EmptyComponent
import com.example.pokedex.components.fallbackStates.ErrorComponent
import com.example.pokedex.components.fallbackStates.LoadingComponent
import com.example.pokedex.models.ApiResult
import com.example.pokedex.models.FilterMode
import com.example.pokedex.models.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    list: List<ApiResult>,
    favorites: Set<String>,
    query: String,
    filterMode: FilterMode,
    loadingState: LoadingState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (FilterMode) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    open: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.searchField)
            )
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag(TestTags.refreshButton)
            ) {
                Text("⟳")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterMode == FilterMode.All,
                onClick = { onFilterChange(FilterMode.All) },
                label = { Text("All") },
                modifier = Modifier.testTag(TestTags.filterAll)
            )
            FilterChip(
                selected = filterMode == FilterMode.FavoritesOnly,
                onClick = { onFilterChange(FilterMode.FavoritesOnly) },
                label = { Text("Favorites") },
                modifier = Modifier.testTag(TestTags.filterFavorites)
            )
        }

        LazyColumn(
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(TestTags.listScreen)
        ) {
            if (list.isEmpty()) {
                item {
                    when (loadingState) {
                        LoadingState.Loading -> LoadingComponent()
                        LoadingState.Error -> ErrorComponent(onRetry)
                        LoadingState.Ok -> EmptyComponent(
                            message = if (query.isNotBlank() || filterMode == FilterMode.FavoritesOnly)
                                "No matches"
                            else
                                "Nothing here"
                        )
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
            }
        }
    }
}
