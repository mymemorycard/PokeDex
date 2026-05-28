package com.example.pokedex.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class PokeDexDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
