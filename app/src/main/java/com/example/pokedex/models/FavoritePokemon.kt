package com.example.pokedex.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoritePokemon(
    @PrimaryKey
    val name: String,
    val url: String
)