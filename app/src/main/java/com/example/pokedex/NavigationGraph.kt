package com.example.pokedex

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.pokedex.components.fallbackStates.ErrorComponent
import com.example.pokedex.components.fallbackStates.LoadingComponent
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
            val state by pokemonViewModel.uiState.collectAsStateWithLifecycle()
            ListScreen(
                list = state.pokemonList,
                favorites = state.favorites,
                query = state.query,
                filterMode = state.filterMode,
                loadingState = state.loading,
                onQueryChange = pokemonViewModel::setQuery,
                onFilterChange = pokemonViewModel::setFilterMode,
                onRefresh = pokemonViewModel::refresh,
                onRetry = pokemonViewModel::refresh,
                open = { name ->
                    pokemonViewModel.fetchPokemon(name)
                    navController.navigate(Screen.Details.createRoute(name))
                }
            )
        }

        composable(
            Screen.Details.route, arguments = listOf(
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name")
            val detail by pokemonViewModel.detailState.collectAsStateWithLifecycle()
            val listState by pokemonViewModel.uiState.collectAsStateWithLifecycle()

            when {
                detail.loading == LoadingState.Loading -> LoadingComponent()
                detail.loading == LoadingState.Error || detail.pokemon == null || name == null ->
                    ErrorComponent(onRetry = {
                        if (name != null) pokemonViewModel.fetchPokemon(name)
                        else navController.popBackStack()
                    })

                else -> InfoScreen(
                    pokemonInfo = detail.pokemon!!,
                    favorite = listState.favorites.contains(name),
                    onToggleFavorite = { pokemonViewModel.toggleFavorites(name) }
                )
            }
        }
    }
}
