package com.example.pokedex.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PokemonViewModel @Inject constructor(
    private val pokeRepository: PokeRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(FilterMode.All)
    val filter: StateFlow<FilterMode> = _filter.asStateFlow()

    private val refreshTrigger =
        MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val loadMoreTrigger =
        MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val favorites: StateFlow<Set<String>> = pokeRepository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val listState: StateFlow<ListLoadState> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { paginatedListFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SHARING_TIMEOUT_MS), ListLoadState())

    private val debouncedQuery: Flow<String> = _query
        .debounce(QUERY_DEBOUNCE_MS)
        .distinctUntilChanged()
        .onStart { emit(_query.value) }

    val uiState: StateFlow<UiState> = combine(
        listState,
        debouncedQuery,
        _filter,
        favorites,
    ) { list, q, mode, favs -> buildUiState(list, q, mode, favs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SHARING_TIMEOUT_MS), UiState())

    fun onQueryChange(text: String) {
        _query.value = text
    }

    fun onFilterChange(mode: FilterMode) {
        _filter.value = mode
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    fun loadMore() {
        loadMoreTrigger.tryEmit(Unit)
    }

    fun toggleFavorite(name: String) {
        viewModelScope.launch { pokeRepository.toggleFavorite(name) }
    }

    private fun paginatedListFlow(): Flow<ListLoadState> = flow {
        var items = emptyList<ApiResult>()
        var canLoadMore = true

        emit(ListLoadState(loading = LoadingState.Loading, items = items, canLoadMore = false))
        try {
            val page = pokeRepository.list(0)
            items = page.results
            canLoadMore = page.next != null
            emit(ListLoadState(LoadingState.Ok, items, canLoadMore))
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            emit(ListLoadState(LoadingState.Error, items, false))
            return@flow
        }

        loadMoreTrigger.collect {
            if (!canLoadMore) return@collect
            emit(ListLoadState(LoadingState.Loading, items, canLoadMore))
            try {
                val page = pokeRepository.list(items.size)
                items = items + page.results
                canLoadMore = page.next != null
                emit(ListLoadState(LoadingState.Ok, items, canLoadMore))
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                emit(ListLoadState(LoadingState.Error, items, canLoadMore))
            }
        }
    }

    private fun buildUiState(
        list: ListLoadState,
        query: String,
        filter: FilterMode,
        favorites: Set<String>,
    ): UiState {
        val trimmed = query.trim()
        val filtered = list.items.asSequence()
            .filter { trimmed.isEmpty() || it.name.contains(trimmed, ignoreCase = true) }
            .filter { filter != FilterMode.FavoritesOnly || favorites.contains(it.name) }
            .toList()
        return UiState(
            loading = list.loading,
            pokemonList = filtered,
            favorites = favorites,
            canLoadMore = list.canLoadMore && trimmed.isEmpty() && filter == FilterMode.All,
        )
    }

    private data class ListLoadState(
        val loading: LoadingState = LoadingState.Loading,
        val items: List<ApiResult> = emptyList(),
        val canLoadMore: Boolean = false,
    )

    private companion object {
        const val QUERY_DEBOUNCE_MS = 300L
        const val SHARING_TIMEOUT_MS = 5_000L
    }
}
