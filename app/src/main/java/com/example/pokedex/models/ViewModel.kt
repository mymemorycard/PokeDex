package com.example.pokedex.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        fetchMore()
    }

    fun toggleFavorites(name: String) {
        if (_uiState.value.favorites.contains(name)) {
            _uiState.update {
                _uiState.value.copy(favorites = _uiState.value.favorites - setOf(name))
            }

        } else {
            _uiState.update {
                _uiState.value.copy(favorites = _uiState.value.favorites + setOf(name))
            }
        }
    }

    fun fetchMore() {
        viewModelScope.launch {
            _uiState.update {
                _uiState.value.copy(loading = LoadingState.Loading)
            }
            try {
                val listResult = pokeApiService.list(uiState.value.pokemonList.size, 100)
                _uiState.update {
                    _uiState.value.copy(
                        loading = LoadingState.Ok,
                        pokemonList = _uiState.value.pokemonList + listResult.results
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    _uiState.value.copy(
                        loading = LoadingState.Error,
                    )
                }

            }
        }
    }

    fun fetchPokemon(name: String) {
        viewModelScope.launch {
            _uiState.update {
                _uiState.value.copy(loading = LoadingState.Loading)
            }
            try {
                val result = pokeApiService.getPokemon(name)
                _uiState.update {
                    _uiState.value.copy(
                        loading = LoadingState.Ok,
                        pokemonByName = _uiState.value.pokemonByName + mapOf(Pair(name, result)),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    _uiState.value.copy(
                        loading = LoadingState.Error,
                    )
                }

            }
        }
    }
}