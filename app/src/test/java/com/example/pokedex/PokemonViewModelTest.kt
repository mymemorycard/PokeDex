package com.example.pokedex

import com.example.pokedex.models.FilterMode
import com.example.pokedex.models.LoadingState
import com.example.pokedex.models.PokeRepository
import com.example.pokedex.models.PokemonViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
    private val repository: PokeRepository = mockk(relaxUnitFun = true) {
        coEvery { getFavorites() } returns favoritesFlow
    }

    private fun TestScope.viewModel(): PokemonViewModel = PokemonViewModel(repository).also { vm ->
        // Keep the WhileSubscribed StateFlows alive for the duration of the test.
        backgroundScope.launch { vm.uiState.collect {} }
    }

    @Test
    fun `initial state emits loaded list`() = runTest(mainRule.dispatcher) {
        coEvery { repository.list(0) } returns pokeList(items = listOf(apiResult("bulbasaur"), apiResult("ivysaur")))

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(LoadingState.Ok, state.loading)
        assertEquals(listOf("bulbasaur", "ivysaur"), state.pokemonList.map { it.name })
    }

    @Test
    fun `loadMore appends next page and pagination is preserved`() = runTest(mainRule.dispatcher) {
        coEvery { repository.list(0) } returns pokeList(next = "next", items = listOf(apiResult("a"), apiResult("b")))
        coEvery { repository.list(2) } returns pokeList(next = null, items = listOf(apiResult("c")))

        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.pokemonList.size)
        assertTrue(vm.uiState.value.canLoadMore)

        vm.loadMore()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("a", "b", "c"), state.pokemonList.map { it.name })
        assertFalse("canLoadMore should be false when next is null", state.canLoadMore)
        coVerify { repository.list(0) }
        coVerify { repository.list(2) }
    }

    @Test
    fun `search filter applies after debounce, query updates immediately`() = runTest(mainRule.dispatcher) {
        coEvery { repository.list(0) } returns pokeList(items = listOf(apiResult("bulbasaur"), apiResult("charmander")))

        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.pokemonList.size)

        vm.onQueryChange("char")
        // Query StateFlow updates immediately for the TextField (no debounce on UI value).
        assertEquals("char", vm.query.value)

        // Filtering hasn't applied yet (still inside debounce window).
        advanceTimeBy(100)
        assertEquals(2, vm.uiState.value.pokemonList.size)

        // After the debounce window the filtered list is emitted.
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(listOf("charmander"), vm.uiState.value.pokemonList.map { it.name })
    }

    @Test
    fun `favorites filter narrows list to favorited entries`() = runTest(mainRule.dispatcher) {
        coEvery { repository.list(0) } returns pokeList(items = listOf(apiResult("a"), apiResult("b"), apiResult("c")))

        val vm = viewModel()
        advanceUntilIdle()
        favoritesFlow.value = setOf("b")

        vm.onFilterChange(FilterMode.FavoritesOnly)
        advanceUntilIdle()

        assertEquals(listOf("b"), vm.uiState.value.pokemonList.map { it.name })
    }

    @Test
    fun `error then refresh recovers list`() = runTest(mainRule.dispatcher) {
        coEvery { repository.list(0) } throws RuntimeException("network")
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(LoadingState.Error, vm.uiState.value.loading)

        coEvery { repository.list(0) } returns pokeList(items = listOf(apiResult("pikachu")))
        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(LoadingState.Ok, state.loading)
        assertEquals(listOf("pikachu"), state.pokemonList.map { it.name })
    }

    @Test
    fun `toggleFavorite delegates to repository without touching list state`() = runTest(mainRule.dispatcher) {
        coEvery { repository.list(0) } returns pokeList(items = emptyList())
        coEvery { repository.toggleFavorite(any()) } just Runs

        val vm = viewModel()
        advanceUntilIdle()

        vm.toggleFavorite("eevee")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.toggleFavorite("eevee") }
    }
}
