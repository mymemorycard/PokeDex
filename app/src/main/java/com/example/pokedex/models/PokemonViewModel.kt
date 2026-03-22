package com.example.pokedex.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PokemonViewModel(
    private val pokeRepository: PokeRepository
) : ViewModel() {
    var uiState by mutableStateOf(UiState())

    init {
        fetchMore()
    }

    fun toggleFavorites(name: String) {
        if (uiState.favorites.contains(name)) {
            uiState = uiState.copy(favorites = uiState.favorites - setOf(name))
        } else {
            uiState = uiState.copy(favorites = uiState.favorites + setOf(name))
        }
    }

    fun fetchMore() {
        viewModelScope.launch {
            uiState = uiState.copy(loading = LoadingState.Loading)

            try {
                val listResult = pokeRepository.list(uiState.pokemonList.size)
                uiState = uiState.copy(
                    loading = LoadingState.Ok,
                    pokemonList = uiState.pokemonList + listResult.results
                )
            } catch (e: Exception) {
                e.printStackTrace()
                uiState = uiState.copy(loading = LoadingState.Error)
            }
        }
    }

    fun fetchPokemon(name: String) {
        viewModelScope.launch {
            uiState = uiState.copy(loading = LoadingState.Loading)

            try {
                val result = pokeRepository.getPokemon(name)
                uiState = uiState.copy(
                    loading = LoadingState.Ok,
                    pokemonByName = uiState.pokemonByName + mapOf(Pair(name, result))
                )
            } catch (e: Exception) {
                e.printStackTrace()
                uiState = uiState.copy(loading = LoadingState.Error)
            }
        }
    }
}