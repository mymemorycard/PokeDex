package com.example.pokedex.components.di

import com.example.pokedex.models.PokeAPIRepository
import com.example.pokedex.models.PokeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepository(
        impl: PokeAPIRepository
    ): PokeRepository
}