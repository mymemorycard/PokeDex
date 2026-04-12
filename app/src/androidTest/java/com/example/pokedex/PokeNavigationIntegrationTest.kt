package com.example.pokedex

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.pokedex.models.ApiResult
import com.example.pokedex.models.PokeList
import com.example.pokedex.models.PokeRepository
import com.example.pokedex.models.PokemonInfo
import com.example.pokedex.models.PokemonViewModel
import com.example.pokedex.models.samplePokeList
import com.example.pokedex.models.samplePokemonInfo
import com.example.pokedex.ui.theme.PokeDexTheme
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PokeNavigationIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `list item click opens details for the requested pokemon`() {
        val repository = FakeUiRepository().apply {
            listResults += { samplePokeList("pikachu", "bulbasaur") }
            pokemonResults["bulbasaur"] = samplePokemonInfo("bulbasaur")
        }

        val viewModel = PokemonViewModel(repository)

        composeRule.setContent {
            TestApp(viewModel)
        }

        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(TestTags.pokemonName("bulbasaur")).fetchSemanticsNode()
            }.isSuccess
        }

        composeRule.onNodeWithTag(TestTags.pokemonName("bulbasaur")).performClick()

        composeRule.waitUntil(5_000) {
            repository.requestedPokemonNames.contains("bulbasaur")
        }

        composeRule.onNodeWithTag(TestTags.detailsScreen).fetchSemanticsNode()
        composeRule.onNodeWithTag(TestTags.detailsName).assertTextEquals("bulbasaur")
        assertEquals(listOf("bulbasaur"), repository.requestedPokemonNames)
    }

    @Test
    fun `retry button reloads list after initial error`() {
        val repository = FakeUiRepository().apply {
            listResults += { throw IOException("network failed") }
            listResults += { samplePokeList("pikachu") }
        }

        val viewModel = PokemonViewModel(repository)

        composeRule.setContent {
            TestApp(viewModel)
        }

        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(TestTags.retryButton).fetchSemanticsNode()
            }.isSuccess
        }

        composeRule.onNodeWithTag(TestTags.retryButton).performClick()

        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(TestTags.pokemonName("pikachu")).fetchSemanticsNode()
            }.isSuccess
        }

        composeRule.onNodeWithTag(TestTags.listScreen).fetchSemanticsNode()
        composeRule.onNodeWithTag(TestTags.pokemonName("pikachu")).fetchSemanticsNode()
        assertEquals(2, repository.listCalls)
    }

    @Composable
    private fun TestApp(viewModel: PokemonViewModel) {
        PokeDexTheme {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "root"
            ) {
                pokeApiGraph(
                    navController = navController,
                    pokemonViewModel = viewModel
                )
            }
        }
    }

    private class FakeUiRepository : PokeRepository {
        val listResults = ArrayDeque<suspend () -> PokeList>()
        val pokemonResults = mutableMapOf<String, PokemonInfo>()
        private val favorites = MutableStateFlow(emptySet<String>())

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
            favorites.value = favorites.value.toMutableSet().apply {
                if (!add(pokemon.name)) {
                    remove(pokemon.name)
                }
            }
        }

        override fun getFavorites(): Flow<Set<String>> = favorites
    }
}
