package com.example.pokedex.models

import app.cash.turbine.test
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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

        viewModel.uiState.test {
            assertEquals(LoadingState.Loading, awaitItem().loading)
            val loaded = awaitItem()
            assertEquals(LoadingState.Ok, loaded.loading)
            assertEquals(listOf("pikachu", "bulbasaur"), loaded.pokemonList.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, repository.listCalls)
    }

    @Test
    fun `keeps loading while request is still in progress`() = runTest {
        val deferred = CompletableDeferred<PokeList>()
        val repository = FakePokeRepository().apply {
            listResults += { deferred.await() }
        }

        val viewModel = PokemonViewModel(repository)

        viewModel.uiState.test {
            assertEquals(LoadingState.Loading, awaitItem().loading)

            runCurrent()
            expectNoEvents()

            deferred.complete(samplePokeList("pikachu"))
            val loaded = awaitItem()
            assertEquals(LoadingState.Ok, loaded.loading)
            assertEquals(listOf("pikachu"), loaded.pokemonList.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shows error when initial list request fails`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { throw IOException("boom") }
        }

        val viewModel = PokemonViewModel(repository)

        viewModel.uiState.test {
            assertEquals(LoadingState.Loading, awaitItem().loading)
            val errored = awaitItem()
            assertEquals(LoadingState.Error, errored.loading)
            assertTrue(errored.pokemonList.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repository.listCalls)
    }

    @Test
    fun `refresh after error performs a new request and recovers`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { throw IOException("boom") }
            listResults += { samplePokeList("pikachu") }
        }

        val viewModel = PokemonViewModel(repository)

        viewModel.uiState.test {
            assertEquals(LoadingState.Loading, awaitItem().loading)
            assertEquals(LoadingState.Error, awaitItem().loading)

            viewModel.refresh()

            assertEquals(LoadingState.Loading, awaitItem().loading)
            val recovered = awaitItem()
            assertEquals(LoadingState.Ok, recovered.loading)
            assertEquals(listOf("pikachu"), recovered.pokemonList.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(2, repository.listCalls)
    }

    @Test
    fun `query is debounced and filters list by name`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu", "bulbasaur", "charmander") }
        }

        val viewModel = PokemonViewModel(repository)
        keepSubscribed(viewModel.uiState)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.pokemonList.size)

        viewModel.setQuery("p")
        viewModel.setQuery("pi")
        viewModel.setQuery("pik")

        advanceTimeBy(150)
        runCurrent()
        assertEquals(3, viewModel.uiState.value.pokemonList.size)

        advanceTimeBy(300)
        runCurrent()

        assertEquals(listOf("pikachu"), viewModel.uiState.value.pokemonList.map { it.name })
    }

    @Test
    fun `filter favorites only restricts list to favorites set`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu", "bulbasaur", "charmander") }
            favoritesFlow.value = setOf("bulbasaur")
        }

        val viewModel = PokemonViewModel(repository)
        keepSubscribed(viewModel.uiState)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.pokemonList.size)

        viewModel.setFilterMode(FilterMode.FavoritesOnly)
        advanceUntilIdle()

        assertEquals(listOf("bulbasaur"), viewModel.uiState.value.pokemonList.map { it.name })
        assertEquals(FilterMode.FavoritesOnly, viewModel.uiState.value.filterMode)
    }

    @Test
    fun `favorites flow update is reflected in ui state without manual reload`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu") }
        }

        val viewModel = PokemonViewModel(repository)
        keepSubscribed(viewModel.uiState)
        advanceUntilIdle()

        assertEquals(emptySet<String>(), viewModel.uiState.value.favorites)

        repository.favoritesFlow.value = setOf("pikachu")
        advanceUntilIdle()

        assertEquals(setOf("pikachu"), viewModel.uiState.value.favorites)
    }

    @Test
    fun `fetchPokemon stores details and exposes them via detailState`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu") }
            pokemonResults["pikachu"] = samplePokemonInfo("pikachu")
        }

        val viewModel = PokemonViewModel(repository)
        keepSubscribed(viewModel.detailState)
        advanceUntilIdle()

        assertEquals(DetailState(LoadingState.Loading, null), viewModel.detailState.value)

        viewModel.fetchPokemon("pikachu")
        advanceUntilIdle()

        assertEquals(LoadingState.Ok, viewModel.detailState.value.loading)
        assertEquals(samplePokemonInfo("pikachu"), viewModel.detailState.value.pokemon)
        assertEquals(listOf("pikachu"), repository.requestedPokemonNames)
    }

    @Test
    fun `detail retry for the same name triggers a fresh request`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu") }
            pokemonResults["pikachu"] = samplePokemonInfo("pikachu")
        }

        val viewModel = PokemonViewModel(repository)
        keepSubscribed(viewModel.detailState)
        advanceUntilIdle()

        viewModel.fetchPokemon("pikachu")
        advanceUntilIdle()
        assertEquals(LoadingState.Ok, viewModel.detailState.value.loading)

        viewModel.fetchPokemon("pikachu")
        advanceUntilIdle()
        assertEquals(2, repository.requestedPokemonNames.count { it == "pikachu" })
    }

    @Test
    fun `favorites stateflow is independent from list subscription lifecycle`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu") }
            favoritesFlow.value = setOf("pikachu")
        }

        val viewModel = PokemonViewModel(repository)
        advanceUntilIdle()

        assertEquals(setOf("pikachu"), viewModel.favorites.value)

        repository.favoritesFlow.value = setOf("pikachu", "bulbasaur")
        advanceUntilIdle()

        assertEquals(setOf("pikachu", "bulbasaur"), viewModel.favorites.value)
    }

    @Test
    fun `setFilterMode does not trigger a new network request`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu", "bulbasaur") }
        }

        val viewModel = PokemonViewModel(repository)
        keepSubscribed(viewModel.uiState)
        advanceUntilIdle()

        viewModel.setFilterMode(FilterMode.FavoritesOnly)
        viewModel.setFilterMode(FilterMode.All)
        viewModel.setQuery("bul")
        advanceUntilIdle()

        assertEquals(1, repository.listCalls)
    }

    @Test
    fun `detailState exposes error when getPokemon fails`() = runTest {
        val repository = FakePokeRepository().apply {
            listResults += { samplePokeList("pikachu") }
            failPokemonFetches = true
        }

        val viewModel = PokemonViewModel(repository)
        keepSubscribed(viewModel.detailState)
        advanceUntilIdle()

        viewModel.fetchPokemon("pikachu")
        advanceUntilIdle()

        assertEquals(LoadingState.Error, viewModel.detailState.value.loading)
    }

    private fun <T> TestScope.keepSubscribed(state: StateFlow<T>) {
        backgroundScope.launch { state.collect {} }
    }

    private class FakePokeRepository : PokeRepository {
        val listResults = ArrayDeque<suspend () -> PokeList>()
        val pokemonResults = mutableMapOf<String, PokemonInfo>()
        val favoritesFlow = MutableStateFlow(emptySet<String>())
        var failPokemonFetches = false

        var listCalls = 0
        val requestedPokemonNames = mutableListOf<String>()

        override suspend fun list(offset: Int): PokeList {
            listCalls++
            return listResults.removeFirst().invoke()
        }

        override suspend fun getPokemon(name: String): PokemonInfo {
            requestedPokemonNames += name
            if (failPokemonFetches) throw IOException("boom")
            return pokemonResults.getValue(name)
        }

        override suspend fun toggleFavorite(pokemon: ApiResult) {
            favoritesFlow.value = favoritesFlow.value.toMutableSet().apply {
                if (!add(pokemon.name)) remove(pokemon.name)
            }
        }

        override fun getFavorites(): Flow<Set<String>> = favoritesFlow
    }
}
