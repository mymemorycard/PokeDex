package com.example.pokedex.models

interface PokeRepository {
    suspend fun list(offset: Int): PokeList
    suspend fun getPokemon(name: String): PokemonInfo
}