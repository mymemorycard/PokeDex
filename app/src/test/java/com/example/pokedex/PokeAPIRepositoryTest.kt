package com.example.pokedex

import app.cash.turbine.test
import com.example.pokedex.data.FavoriteDao
import com.example.pokedex.data.FavoriteEntity
import com.example.pokedex.models.PokeAPI
import com.example.pokedex.models.PokeAPIRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokeAPIRepositoryTest {

    private val api: PokeAPI = mockk()
    private val dao: FavoriteDao = mockk(relaxUnitFun = true)
    private val repository = PokeAPIRepository(api, dao)

    @Test
    fun `list forwards offset and uses page size`() = runTest {
        coEvery { api.list(any(), any()) } returns pokeList(items = emptyList())

        repository.list(offset = 100)

        coVerify { api.list(100, 50) }
    }

    @Test
    fun `getPokemon forwards by name`() = runTest {
        coEvery { api.getPokemon("mew") } returns pokemonInfo("mew")

        val result = repository.getPokemon("mew")

        assertEquals("mew", result.name)
        coVerify { api.getPokemon("mew") }
    }

    @Test
    fun `toggleFavorite inserts when missing and deletes when present`() = runTest {
        coEvery { dao.exists("mew") } returns false
        coEvery { dao.insert(any()) } just Runs
        repository.toggleFavorite("mew")
        coVerify { dao.insert(FavoriteEntity("mew")) }
        coVerify(exactly = 0) { dao.delete(any()) }

        coEvery { dao.exists("mew") } returns true
        coEvery { dao.delete(any()) } just Runs
        repository.toggleFavorite("mew")
        coVerify { dao.delete("mew") }
    }

    @Test
    fun `getFavorites maps list to set`() = runTest {
        val source = MutableStateFlow(listOf("a", "b", "a"))
        coEvery { dao.observeNames() } returns source

        repository.getFavorites().test {
            assertEquals(setOf("a", "b"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
