package com.example.pokedex.di

import android.content.Context
import androidx.room.Room
import com.example.pokedex.data.FavoriteDao
import com.example.pokedex.data.PokeDexDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PokeDexDatabase =
        Room.databaseBuilder(context, PokeDexDatabase::class.java, "pokedex.db").build()

    @Provides
    fun provideFavoriteDao(db: PokeDexDatabase): FavoriteDao = db.favoriteDao()
}
