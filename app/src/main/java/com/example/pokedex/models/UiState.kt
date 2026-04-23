package com.example.pokedex.models

enum class FilterMode { All, FavoritesOnly }

data class UiState(
    val loading: LoadingState = LoadingState.Loading,
    val pokemonList: List<ApiResult> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val query: String = "",
    val filterMode: FilterMode = FilterMode.All
)

data class DetailState(
    val loading: LoadingState = LoadingState.Loading,
    val pokemon: PokemonInfo? = null
)

sealed class ListLoadResult {
    object Loading : ListLoadResult()
    object Error : ListLoadResult()
    data class Success(val items: List<ApiResult>) : ListLoadResult()
}
