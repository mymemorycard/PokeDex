package com.example.pokedex.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.pokedex.TestTags
import com.example.pokedex.models.ApiResult

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListPokemonItem(
    pokemon: ApiResult,
    onClick: () -> Unit,
    favorite: Boolean
) {
    ListItem(
        onClick = onClick,
        modifier = Modifier.testTag(TestTags.pokemonName(pokemon.name))
    ) {
        Row {
            if (favorite) Text(text = "❤️")
            Text(text = pokemon.name)
        }

    }
}