package com.example.pokedex.models

import kotlinx.coroutines.flow.Flow

interface PokeRepository {
    suspend fun list(offset: Int): PokeList
    suspend fun getPokemon(name: String): PokemonInfo
    suspend fun toggleFavorite(pokemon: ApiResult)
    fun getFavorites(): Flow<Set<String>>
}