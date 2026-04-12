package com.example.pokedex.models

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts in loading and then exposes loaded list`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu", "bulbasaur") }
        }

        val viewModel = PokemonViewModel(repository)

        assertEquals(LoadingState.Loading, viewModel.uiState.loading)

        advanceUntilIdle()

        assertEquals(LoadingState.Ok, viewModel.uiState.loading)
        assertEquals(listOf("pikachu", "bulbasaur"), viewModel.uiState.pokemonList.map { it.name })
        assertEquals(1, repository.listCalls)
    }

    @Test
    fun `shows loading while request is still in progress`() = runTest {
        val deferred = CompletableDeferred<PokeList>()
        val repository = FakePokeRepository().apply {
            listResults += { deferred.await() }
        }

        val viewModel = PokemonViewModel(repository)

        runCurrent()

        assertEquals(LoadingState.Loading, viewModel.uiState.loading)
        assertTrue(viewModel.uiState.pokemonList.isEmpty())

        deferred.complete(samplePokeList("pikachu"))
        advanceUntilIdle()

        assertEquals(LoadingState.Ok, viewModel.uiState.loading)
        assertEquals(listOf("pikachu"), viewModel.uiState.pokemonList.map { it.name })
    }

    @Test
    fun `shows error when initial list request fails`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { throw IOException("boom") }
        }

        val viewModel = PokemonViewModel(repository)

        advanceUntilIdle()

        assertEquals(LoadingState.Error, viewModel.uiState.loading)
        assertTrue(viewModel.uiState.pokemonList.isEmpty())
        assertEquals(1, repository.listCalls)
    }

    @Test
    fun `retry after error performs a new request and recovers`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { throw IOException("boom") }
            listResults += { samplePokeList("pikachu") }
        }

        val viewModel = PokemonViewModel(repository)
        advanceUntilIdle()

        assertEquals(LoadingState.Error, viewModel.uiState.loading)

        viewModel.fetchMore()
        advanceUntilIdle()

        assertEquals(2, repository.listCalls)
        assertEquals(LoadingState.Ok, viewModel.uiState.loading)
        assertEquals(listOf("pikachu"), viewModel.uiState.pokemonList.map { it.name })
    }

    @Test
    fun `fetchPokemon stores details by requested name`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu") }
            pokemonResults["pikachu"] = samplePokemonInfo("pikachu")
        }

        val viewModel = PokemonViewModel(repository)
        advanceUntilIdle()

        viewModel.fetchPokemon("pikachu")
        advanceUntilIdle()

        assertEquals(
            samplePokemonInfo("pikachu"),
            viewModel.uiState.pokemonByName["pikachu"]
        )
        assertEquals(listOf("pikachu"), repository.requestedPokemonNames)
    }

    @Test
    fun `favorites flow updates ui state favorites`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu") }
        }

        val viewModel = PokemonViewModel(repository)
        advanceUntilIdle()

        repository.favoritesFlow.value = setOf("pikachu")
        advanceUntilIdle()

        assertEquals(setOf("pikachu"), viewModel.uiState.favorites)
    }

    private class FakePokeRepository : PokeRepository {
        val listResults = ArrayDeque<suspend () -> PokeList>()
        val pokemonResults = mutableMapOf<String, PokemonInfo>()
        val favoritesFlow = MutableStateFlow(emptySet<String>())

        var listCalls = 0
        val requestedPokemonNames = mutableListOf<String>()

        override suspend fun list(offset: Int): PokeList {
            listCalls++
            return listResults.removeFirst().invoke()
        }

        override suspend fun getPokemon(name: String): PokemonInfo {
            requestedPokemonNames += name
            return pokemonResults.getValue(name)
        }

        override suspend fun toggleFavorite(pokemon: ApiResult) {
            favoritesFlow.value = favoritesFlow.value.toMutableSet().apply {
                if (!add(pokemon.name)) {
                    remove(pokemon.name)
                }
            }
        }

        override fun getFavorites(): Flow<Set<String>> = favoritesFlow
    }
}
