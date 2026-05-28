package com.example.pokedex.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT name FROM favorites")
    fun observeNames(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE name = :name)")
    suspend fun exists(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE name = :name")
    suspend fun delete(name: String)
}
