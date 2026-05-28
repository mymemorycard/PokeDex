package com.example.pokedex.models

data class UiState(
    val loading: LoadingState = LoadingState.Loading,
    val pokemonList: List<ApiResult> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val canLoadMore: Boolean = false,
)

data class DetailUiState(
    val loading: LoadingState = LoadingState.Loading,
    val pokemon: PokemonInfo? = null,
    val isFavorite: Boolean = false,
)

enum class FilterMode { All, FavoritesOnly }
