package com.example.pokedex

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.pokedex.components.FallbackStates.ErrorComponent
import com.example.pokedex.components.FallbackStates.LoadingComponent
import com.example.pokedex.models.LoadingState
import com.example.pokedex.models.PokemonDetailViewModel
import com.example.pokedex.models.PokemonViewModel
import com.example.pokedex.screens.InfoScreen
import com.example.pokedex.screens.ListScreen

sealed class Screen(val route: String) {
    object List : Screen("pokemon")
    object Details : Screen("pokemon/{name}") {
        fun createRoute(name: String) = "pokemon/$name"
    }
}

fun NavGraphBuilder.pokeApiGraph(navController: NavController) {
    navigation(startDestination = Screen.List.route, route = "root") {

        composable(Screen.List.route) {
            val viewModel: PokemonViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val query by viewModel.query.collectAsStateWithLifecycle()
            val filter by viewModel.filter.collectAsStateWithLifecycle()

            ListScreen(
                list = state.pokemonList,
                favorites = state.favorites,
                query = query,
                filter = filter,
                loadingState = state.loading,
                canLoadMore = state.canLoadMore,
                onQueryChange = viewModel::onQueryChange,
                onFilterChange = viewModel::onFilterChange,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::refresh,
                open = { name -> navController.navigate(Screen.Details.createRoute(name)) },
            )
        }

        composable(
            Screen.Details.route,
            arguments = listOf(navArgument(PokemonDetailViewModel.NAME_ARG) { type = NavType.StringType }),
        ) {
            val viewModel: PokemonDetailViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val pokemon = state.pokemon

            when {
                state.loading == LoadingState.Loading && pokemon == null -> LoadingComponent()
                state.loading == LoadingState.Error && pokemon == null -> ErrorComponent(onRetry = viewModel::retry)
                pokemon != null -> InfoScreen(
                    pokemonInfo = pokemon,
                    favorite = state.isFavorite,
                    onToggleFavorite = viewModel::toggleFavorite,
                )
            }
        }
    }
}
