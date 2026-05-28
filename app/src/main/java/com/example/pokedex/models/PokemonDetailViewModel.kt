package com.example.pokedex.models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pokeRepository: PokeRepository,
) : ViewModel() {

    val name: String = checkNotNull(savedStateHandle.get<String>(NAME_ARG)) {
        "Detail screen requires '$NAME_ARG' route argument"
    }

    private val retryTrigger =
        MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val pokemonState: StateFlow<PokemonLoadState> = retryTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { loadPokemon() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PokemonLoadState())

    val state: StateFlow<DetailUiState> = combine(
        pokemonState,
        pokeRepository.getFavorites(),
    ) { ps, favs ->
        DetailUiState(
            loading = ps.loading,
            pokemon = ps.pokemon,
            isFavorite = favs.contains(name),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DetailUiState())

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    fun toggleFavorite() {
        viewModelScope.launch { pokeRepository.toggleFavorite(name) }
    }

    private fun loadPokemon(): Flow<PokemonLoadState> = flow {
        emit(PokemonLoadState(loading = LoadingState.Loading))
        val loaded = try {
            pokeRepository.getPokemon(name)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            emit(PokemonLoadState(LoadingState.Error))
            return@flow
        }
        emit(PokemonLoadState(LoadingState.Ok, loaded))
    }

    private data class PokemonLoadState(
        val loading: LoadingState = LoadingState.Loading,
        val pokemon: PokemonInfo? = null,
    )

    companion object {
        const val NAME_ARG = "name"
    }
}
