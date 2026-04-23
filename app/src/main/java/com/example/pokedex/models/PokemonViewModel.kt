package com.example.pokedex.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PokemonViewModel @Inject constructor(
    private val pokeRepository: PokeRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow(FilterMode.All)
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    private val detailRequests = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)

    private val listLoadFlow = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest {
            flow {
                emit(ListLoadResult.Loading)
                runCatching { pokeRepository.list(0) }
                    .onSuccess { emit(ListLoadResult.Success(it.results)) }
                    .onFailure { emit(ListLoadResult.Error) }
            }
        }

    private val debouncedQuery = queryFlow
        .debounce(QUERY_DEBOUNCE_MS)
        .distinctUntilChanged()
        .onStart { emit("") }

    val uiState: StateFlow<UiState> = combine(
        listLoadFlow,
        debouncedQuery,
        filterFlow,
        pokeRepository.getFavorites()
    ) { load, query, mode, favorites ->
        when (load) {
            ListLoadResult.Loading -> UiState(
                loading = LoadingState.Loading,
                favorites = favorites,
                query = query,
                filterMode = mode
            )

            ListLoadResult.Error -> UiState(
                loading = LoadingState.Error,
                favorites = favorites,
                query = query,
                filterMode = mode
            )

            is ListLoadResult.Success -> UiState(
                loading = LoadingState.Ok,
                pokemonList = load.items
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                    .filter { mode == FilterMode.All || favorites.contains(it.name) },
                favorites = favorites,
                query = query,
                filterMode = mode
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SHARING_TIMEOUT_MS),
        initialValue = UiState()
    )

    private val _detailState = MutableStateFlow(DetailState())
    val detailState: StateFlow<DetailState> = _detailState.asStateFlow()

    init {
        viewModelScope.launch {
            detailRequests
                .distinctUntilChanged()
                .flatMapLatest { name ->
                    flow {
                        emit(DetailState(loading = LoadingState.Loading))
                        runCatching { pokeRepository.getPokemon(name) }
                            .onSuccess { emit(DetailState(LoadingState.Ok, it)) }
                            .onFailure { emit(DetailState(loading = LoadingState.Error)) }
                    }
                }
                .collect { _detailState.value = it }
        }
    }

    fun setQuery(value: String) {
        queryFlow.value = value
    }

    fun setFilterMode(mode: FilterMode) {
        filterFlow.value = mode
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    fun fetchPokemon(name: String) {
        detailRequests.tryEmit(name)
    }

    fun toggleFavorites(name: String) {
        viewModelScope.launch {
            val pokemon = uiState.value.pokemonList.find { it.name == name }
                ?: ApiResult(name, "https://pokeapi.co/api/v2/pokemon/$name/")
            pokeRepository.toggleFavorite(pokemon)
        }
    }

    companion object {
        private const val QUERY_DEBOUNCE_MS = 300L
        private const val STATE_SHARING_TIMEOUT_MS = 5_000L
    }
}
