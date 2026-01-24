package com.example.pokedex.models

data class UiState(
    val loading: LoadingState = LoadingState.Loading,
    val pokemonList: List<ApiResult> = emptyList(),
    val pokemonByName: Map<String, PokemonInfo> = emptyMap(),
    val favorites: Set<String> = emptySet()
)