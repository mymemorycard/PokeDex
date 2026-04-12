package com.example.pokedex.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pokedex.models.FavoritePokemon
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePokemonDao {

    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoritePokemon>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToFavorites(pokemon: FavoritePokemon): Long

    @Delete
    suspend fun removeFromFavorites(pokemon: FavoritePokemon): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE name = :name)")
    suspend fun isFavorite(name: String): Boolean
}