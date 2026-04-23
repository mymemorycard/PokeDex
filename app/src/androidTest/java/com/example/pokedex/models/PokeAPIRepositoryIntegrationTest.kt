package com.example.pokedex.models

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.pokedex.dao.FavoritePokemonDao
import com.example.pokedex.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class PokeAPIRepositoryIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FavoritePokemonDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.favoritePokemonDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `repository toggles favorites and persists them in room`() = runTest {
        val repository = PokeAPIRepository(FakePokeApi(), dao)
        val pokemon = sampleApiResult("pikachu")

        repository.toggleFavorite(pokemon)
        assertEquals(setOf("pikachu"), repository.getFavorites().first())

        repository.toggleFavorite(pokemon)
        assertEquals(emptySet<String>(), repository.getFavorites().first())
    }

    @Test
    fun `repository combines fake api data with room backed favorites`() = runTest {
        val repository = PokeAPIRepository(
            pokeAPI = FakePokeApi(
                listResult = samplePokeList("pikachu", "eevee"),
                pokemonResults = mapOf("eevee" to samplePokemonInfo("eevee"))
            ),
            favoriteDao = dao
        )

        assertEquals(listOf("pikachu", "eevee"), repository.list(0).results.map { it.name })
        assertEquals("eevee", repository.getPokemon("eevee").name)

        repository.toggleFavorite(sampleApiResult("eevee"))

        assertEquals(setOf("eevee"), repository.getFavorites().first())
    }

    private class FakePokeApi(
        private val listResult: PokeList = samplePokeList("pikachu"),
        private val pokemonResults: Map<String, PokemonInfo> = mapOf(
            "pikachu" to samplePokemonInfo("pikachu")
        )
    ) : PokeAPI {
        override suspend fun list(offset: Int, limit: Int): PokeList = listResult

        override suspend fun getPokemon(name: String): PokemonInfo = pokemonResults.getValue(name)
    }
}
