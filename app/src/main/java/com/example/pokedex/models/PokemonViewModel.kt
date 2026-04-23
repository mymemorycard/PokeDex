package com.example.pokedex.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Реактивная композиция четырёх независимых источников:
 * 1. [queryFlow]     — ввод поиска (UI)
 * 2. [filterFlow]    — режим фильтра All / FavoritesOnly (UI)
 * 3. [refreshTrigger]— поток событий Refresh / Retry (UI events)
 * 4. избранное из Room через [PokeRepository.getFavorites] (data layer)
 *
 * Источники объединяются `combine`-ом в один [UiState].
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PokemonViewModel @Inject constructor(
    private val pokeRepository: PokeRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow(FilterMode.All)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val detailRequests = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private val listLoadFlow: Flow<ListLoadResult> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { loadList() }

    private val debouncedQuery: Flow<String> = queryFlow
        .debounce(QUERY_DEBOUNCE_MS)
        .distinctUntilChanged()
        .onStart { emit("") }

    /**
     * Избранное живёт независимо от подписки на список — экран деталей и
     * списка должны видеть актуальное "сердечко" даже когда список не активен.
     */
    val favorites: StateFlow<Set<String>> = pokeRepository.getFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    val uiState: StateFlow<UiState> = combine(
        listLoadFlow,
        debouncedQuery,
        filterFlow,
        favorites
    ) { load, query, mode, favs ->
        buildUiState(load, query, mode, favs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SHARING_TIMEOUT_MS),
        initialValue = UiState()
    )

    val detailState: StateFlow<DetailState> = detailRequests
        .flatMapLatest { name -> loadDetails(name) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DetailState()
        )

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
                ?: ApiResult(name, "$POKEAPI_BASE_URL/$name/")
            pokeRepository.toggleFavorite(pokemon)
        }
    }

    private fun loadList(): Flow<ListLoadResult> = flow {
        emit(ListLoadResult.Loading)
        runCatching { pokeRepository.list(0) }
            .onSuccess { emit(ListLoadResult.Success(it.results)) }
            .onFailure { emit(ListLoadResult.Error) }
    }

    private fun loadDetails(name: String): Flow<DetailState> = flow {
        emit(DetailState(loading = LoadingState.Loading))
        runCatching { pokeRepository.getPokemon(name) }
            .onSuccess { emit(DetailState(LoadingState.Ok, it)) }
            .onFailure { emit(DetailState(loading = LoadingState.Error)) }
    }

    private fun buildUiState(
        load: ListLoadResult,
        query: String,
        mode: FilterMode,
        favorites: Set<String>
    ): UiState {
        val base = UiState(
            favorites = favorites,
            query = query,
            filterMode = mode
        )
        return when (load) {
            ListLoadResult.Loading -> base.copy(loading = LoadingState.Loading)
            ListLoadResult.Error -> base.copy(loading = LoadingState.Error)
            is ListLoadResult.Success -> base.copy(
                loading = LoadingState.Ok,
                pokemonList = load.items
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                    .filter { mode == FilterMode.All || favorites.contains(it.name) }
            )
        }
    }

    private companion object {
        const val QUERY_DEBOUNCE_MS = 300L
        const val STATE_SHARING_TIMEOUT_MS = 5_000L
        const val POKEAPI_BASE_URL = "https://pokeapi.co/api/v2/pokemon"
    }
}
