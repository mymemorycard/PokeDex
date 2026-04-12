package com.example.pokedex.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import coil3.compose.SubcomposeAsyncImage
import com.example.pokedex.TestTags
import com.example.pokedex.components.DetailsRow
import com.example.pokedex.models.PokemonInfo

@Composable
fun InfoScreen(
    pokemonInfo: PokemonInfo,
    favorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.detailsScreen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        item {
            Row {
                SubcomposeAsyncImage(
                    model = pokemonInfo.sprites.frontDefault,
                    "Front default"
                )
                SubcomposeAsyncImage(
                    model = pokemonInfo.sprites.backDefault,
                    "Back default"
                )
                SubcomposeAsyncImage(
                    model = pokemonInfo.sprites.frontShiny,
                    "Front shiny"
                )
                SubcomposeAsyncImage(
                    model = pokemonInfo.sprites.backShiny,
                    "Back shiny"
                )
            }
        }
        item {
            Text(
                text = pokemonInfo.name,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.testTag(TestTags.detailsName)
            )
        }
        item { ElevatedButton(onToggleFavorite) { Text(if (favorite) "❤️" else "🤮") } }
        item { DetailsRow("Height", pokemonInfo.height.toString()) }
        item { DetailsRow("Weight", pokemonInfo.weight.toString()) }

        items(pokemonInfo.stats.size) { i ->
            val stat = pokemonInfo.stats[i]
            DetailsRow(stat.stat.name, stat.baseStat.toString())

        }
    }
}