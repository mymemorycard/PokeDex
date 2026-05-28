package com.example.pokedex.models

import com.example.pokedex.data.FavoriteDao
import com.example.pokedex.data.FavoriteEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PokeAPIRepository @Inject constructor(
    private val pokeAPI: PokeAPI,
    private val favoriteDao: FavoriteDao,
) : PokeRepository {

    override suspend fun list(offset: Int): PokeList = pokeAPI.list(offset, PAGE_SIZE)

    override suspend fun getPokemon(name: String): PokemonInfo = pokeAPI.getPokemon(name)

    override fun getFavorites(): Flow<Set<String>> =
        favoriteDao.observeNames().map { it.toSet() }

    override suspend fun toggleFavorite(name: String) {
        if (favoriteDao.exists(name)) {
            favoriteDao.delete(name)
        } else {
            favoriteDao.insert(FavoriteEntity(name))
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
