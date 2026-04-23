package com.example.pokedex

object TestTags {
    const val listScreen = "list_screen"
    const val detailsScreen = "details_screen"
    const val detailsName = "details_name"
    const val errorMessage = "error_message"
    const val retryButton = "retry_button"
    const val searchField = "search_field"
    const val refreshButton = "refresh_button"
    const val filterAll = "filter_all"
    const val filterFavorites = "filter_favorites"
    const val emptyMessage = "empty_message"

    fun pokemonName(name: String): String = "pokemon_name_$name"
}
