package com.example.pokedex.models

import app.cash.turbine.test
import com.example.pokedex.dao.FavoritePokemonDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokeAPIRepositoryTest {

    @Test
    fun `toggleFavorite adds pokemon when it is not favorite yet`() = runTest {
        val dao = FakeFavoritePokemonDao()
        val repository = PokeAPIRepository(FakePokeApi(), dao)
        val pokemon = sampleApiResult("pikachu")

        repository.toggleFavorite(pokemon)

        assertTrue(dao.isFavorite("pikachu"))
        assertEquals(setOf("pikachu"), repository.getFavorites().first())
    }

    @Test
    fun `toggleFavorite removes pokemon when it is already favorite`() = runTest {
        val dao = FakeFavoritePokemonDao(
            listOf(FavoritePokemon(name = "pikachu", url = "https://pokeapi.co/api/v2/pokemon/pikachu"))
        )
        val repository = PokeAPIRepository(FakePokeApi(), dao)

        repository.toggleFavorite(sampleApiResult("pikachu"))

        assertFalse(dao.isFavorite("pikachu"))
        assertEquals(emptySet<String>(), repository.getFavorites().first())
    }

    @Test
    fun `getFavorites emits full mapped sequence without duplicates`() = runTest {
        val dao = FakeFavoritePokemonDao()
        val repository = PokeAPIRepository(FakePokeApi(), dao)

        repository.getFavorites().test {
            assertEquals(emptySet<String>(), awaitItem())

            dao.replaceFavorites(
                listOf(
                    FavoritePokemon("pikachu", "https://pokeapi.co/api/v2/pokemon/pikachu"),
                    FavoritePokemon("pikachu", "https://pokeapi.co/api/v2/pokemon/pikachu"),
                    FavoritePokemon("bulbasaur", "https://pokeapi.co/api/v2/pokemon/bulbasaur")
                )
            )
            assertEquals(setOf("pikachu", "bulbasaur"), awaitItem())

            dao.replaceFavorites(emptyList())
            assertEquals(emptySet<String>(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `new subscriber gets current favorites snapshot immediately`() = runTest {
        val dao = FakeFavoritePokemonDao()
        val repository = PokeAPIRepository(FakePokeApi(), dao)

        dao.replaceFavorites(
            listOf(FavoritePokemon("eevee", "https://pokeapi.co/api/v2/pokemon/eevee"))
        )

        assertEquals(setOf("eevee"), repository.getFavorites().first())
    }

    private class FakePokeApi : PokeAPI {
        override suspend fun list(offset: Int, limit: Int): PokeList = samplePokeList("pikachu")

        override suspend fun getPokemon(name: String): PokemonInfo = samplePokemonInfo(name)
    }

    private class FakeFavoritePokemonDao(
        initialFavorites: List<FavoritePokemon> = emptyList()
    ) : FavoritePokemonDao {
        private val favorites = MutableStateFlow(initialFavorites)

        override fun getAllFavorites(): Flow<List<FavoritePokemon>> = favorites

        override suspend fun addToFavorites(pokemon: FavoritePokemon): Long {
            if (favorites.value.any { it.name == pokemon.name }) {
                return -1
            }

            favorites.value = favorites.value + pokemon
            return 1
        }

        override suspend fun removeFromFavorites(pokemon: FavoritePokemon): Int {
            val hadPokemon = favorites.value.any { it.name == pokemon.name }
            favorites.value = favorites.value.filterNot { it.name == pokemon.name }
            return if (hadPokemon) 1 else 0
        }

        override suspend fun isFavorite(name: String): Boolean {
            return favorites.value.any { it.name == name }
        }

        fun replaceFavorites(newFavorites: List<FavoritePokemon>) {
            favorites.value = newFavorites
        }
    }
}
