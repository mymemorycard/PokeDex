package com.example.pokedex.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PokemonViewModel @Inject constructor(
    private val pokeRepository: PokeRepository
) : ViewModel() {

    var uiState by mutableStateOf(UiState())
        private set

    init {
        fetchMore()
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            pokeRepository.getFavorites().collect { favorites ->
                uiState = uiState.copy(favorites = favorites)
            }
        }
    }

    fun toggleFavorites(name: String) {
        viewModelScope.launch {
            val pokemon = uiState.pokemonList.find { it.name == name }
            if (pokemon != null) {
                pokeRepository.toggleFavorite(pokemon)
            }
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