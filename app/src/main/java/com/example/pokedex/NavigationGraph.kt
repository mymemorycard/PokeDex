package com.example.pokedex

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.pokedex.components.FallbckStates.ErrorComponent
import com.example.pokedex.components.FallbckStates.LoadingComponent
import com.example.pokedex.models.LoadingState
import com.example.pokedex.models.ViewModel
import com.example.pokedex.screens.InfoScreen
import com.example.pokedex.screens.ListScreen

sealed class Screen(val route: String) {
    object List : Screen("pokemon")
    object Details : Screen("pokemon/{name}") {
        fun createRoute(name: String) = "pokemon/$name"
    }
}


fun NavGraphBuilder.pokeApiGraph(
    navController: NavController, viewModel: ViewModel
) {
    navigation(
        startDestination = Screen.List.route, route = "root"
    ) {

        composable(Screen.List.route) {
            val state by viewModel.uiState.collectAsState()
            ListScreen(
                list = state.pokemonList,
                open = { i ->
                    viewModel.fetchPokemon(i)
                    navController.navigate(Screen.Details.createRoute(i))
                },
                favorites = state.favorites,
                onRetry = viewModel::fetchMore,
                onLoadMore = viewModel::fetchMore,
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
            val state = viewModel.uiState.collectAsState().value
            val pokemon = state.pokemonByName[name]
            if (state.loading == LoadingState.Loading) LoadingComponent()
            else if (state.loading == LoadingState.Error || pokemon == null || name == null) ErrorComponent(
                onRetry = {
                    if (name != null) viewModel.fetchPokemon(name)
                    else navController.popBackStack()
                }) else InfoScreen(
                pokemonInfo = pokemon,
                favorite = !state.favorites.contains(name),
                onToggleFavorite = {
                    viewModel.toggleFavorites(name)
                })
        }
    }
}