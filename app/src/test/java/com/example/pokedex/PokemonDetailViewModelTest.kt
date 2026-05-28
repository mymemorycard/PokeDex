package com.example.pokedex

import androidx.lifecycle.SavedStateHandle
import com.example.pokedex.models.LoadingState
import com.example.pokedex.models.PokeRepository
import com.example.pokedex.models.PokemonDetailViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
    private val repository: PokeRepository = mockk(relaxUnitFun = true) {
        coEvery { getFavorites() } returns favoritesFlow
    }

    private fun savedState(name: String) = SavedStateHandle(mapOf(PokemonDetailViewModel.NAME_ARG to name))

    private fun TestScope.viewModel(name: String) =
        PokemonDetailViewModel(savedState(name), repository).also { vm ->
            backgroundScope.launch { vm.state.collect {} }
        }

    @Test
    fun `auto-loads pokemon from route argument without external trigger`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getPokemon("pikachu") } returns pokemonInfo("pikachu")

        val vm = viewModel("pikachu")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(LoadingState.Ok, state.loading)
        assertEquals("pikachu", state.pokemon?.name)
        coVerify { repository.getPokemon("pikachu") }
    }

    @Test
    fun `error then retry re-runs request even for the same name`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getPokemon("snorlax") } throws RuntimeException("boom")
        val vm = viewModel("snorlax")
        advanceUntilIdle()
        assertEquals(LoadingState.Error, vm.state.value.loading)

        coEvery { repository.getPokemon("snorlax") } returns pokemonInfo("snorlax")
        vm.retry()
        advanceUntilIdle()

        assertEquals(LoadingState.Ok, vm.state.value.loading)
        coVerify(exactly = 2) { repository.getPokemon("snorlax") }
    }

    @Test
    fun `isFavorite reflects favorites flow`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getPokemon("eevee") } returns pokemonInfo("eevee")
        val vm = viewModel("eevee")
        advanceUntilIdle()

        assertEquals(false, vm.state.value.isFavorite)
        favoritesFlow.value = setOf("eevee")
        advanceUntilIdle()
        assertTrue(vm.state.value.isFavorite)
    }

    @Test
    fun `toggleFavorite delegates to repository`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getPokemon("ditto") } returns pokemonInfo("ditto")
        coEvery { repository.toggleFavorite(any()) } just Runs
        val vm = viewModel("ditto")
        advanceUntilIdle()

        vm.toggleFavorite()
        advanceUntilIdle()

        coVerify { repository.toggleFavorite("ditto") }
    }
}
