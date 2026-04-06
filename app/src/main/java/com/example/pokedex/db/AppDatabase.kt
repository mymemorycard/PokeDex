package com.example.pokedex.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pokedex.dao.FavoritePokemonDao
import com.example.pokedex.models.FavoritePokemon

@Database(
    entities = [FavoritePokemon::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritePokemonDao(): FavoritePokemonDao
}