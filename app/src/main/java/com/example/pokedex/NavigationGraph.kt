package com.example.pokedex

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.pokedex.components.FallbackStates.ErrorComponent
import com.example.pokedex.components.FallbackStates.LoadingComponent
import com.example.pokedex.models.LoadingState
import com.example.pokedex.models.PokemonViewModel
import com.example.pokedex.screens.InfoScreen
import com.example.pokedex.screens.ListScreen

sealed class Screen(val route: String) {
    object List : Screen("pokemon")
    object Details : Screen("pokemon/{name}") {
        fun createRoute(name: String) = "pokemon/$name"
    }
}


fun NavGraphBuilder.pokeApiGraph(
    navController: NavController, pokemonViewModel: PokemonViewModel
) {
    navigation(
        startDestination = Screen.List.route, route = "root"
    ) {

        composable(Screen.List.route) {
            val state = pokemonViewModel.uiState
            ListScreen(
                list = state.pokemonList,
                open = { i ->
                    pokemonViewModel.fetchPokemon(i)
                    navController.navigate(Screen.Details.createRoute(i))
                },
                favorites = state.favorites,
                onRetry = pokemonViewModel::fetchMore,
                onLoadMore = pokemonViewModel::fetchMore,
                loadingState = state.loading
            )
        }

        composable(
            Screen.Details.route, arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                })
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name")
            val state = pokemonViewModel.uiState
            val pokemon = state.pokemonByName[name]
            if (state.loading == LoadingState.Loading) LoadingComponent()
            else if (state.loading == LoadingState.Error || pokemon == null || name == null) ErrorComponent(
                onRetry = {
                    if (name != null) pokemonViewModel.fetchPokemon(name)
                    else navController.popBackStack()
                }) else InfoScreen(
                pokemonInfo = pokemon,
                favorite = !state.favorites.contains(name),
                onToggleFavorite = {
                    pokemonViewModel.toggleFavorites(name)
                })
        }
    }
}