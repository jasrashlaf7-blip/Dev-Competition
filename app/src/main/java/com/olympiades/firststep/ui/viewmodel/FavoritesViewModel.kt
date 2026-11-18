package com.olympiades.firststep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data class Success(val favorites: List<FavoriteOffer>) : FavoritesUiState
    data class Error(val message: String) : FavoritesUiState
}

class FavoritesViewModel(
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = toggleFavoriteUseCase.observeFavorites()
        .map { FavoritesUiState.Success(it) as FavoritesUiState }
        .catch { emit(FavoritesUiState.Error(it.localizedMessage ?: "Database error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState.Loading
        )

    private val _removedOfferEvents = MutableSharedFlow<Long>()
    val removedOfferEvents: SharedFlow<Long> = _removedOfferEvents.asSharedFlow()

    fun removeFavorite(offerId: Long) {
        viewModelScope.launch {
            val stillFavorite = toggleFavoriteUseCase(offerId)
            if (!stillFavorite) {
                _removedOfferEvents.emit(offerId)
            }
        }
    }

    fun undoRemove(offerId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase.restore(offerId)
        }
    }
}
