package com.example.pokedex.models

import kotlinx.coroutines.flow.Flow

interface PokeRepository {
    suspend fun list(offset: Int): PokeList
    suspend fun getPokemon(name: String): PokemonInfo

    fun getFavorites(): Flow<Set<String>>
    suspend fun toggleFavorite(name: String)
}
