package com.olympiades.firststep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.usecase.GetOfferDetailUseCase
import com.olympiades.firststep.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OfferDetailUiState {
    data object Loading : OfferDetailUiState
    data class Success(val offer: Offer) : OfferDetailUiState
    data object NotFound : OfferDetailUiState
    data class Error(val message: String) : OfferDetailUiState
}

class OfferDetailViewModel(
    private val offerId: Long,
    getOfferDetailUseCase: GetOfferDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    val uiState: StateFlow<OfferDetailUiState> = getOfferDetailUseCase(offerId)
        .map { offer ->
            if (offer != null) OfferDetailUiState.Success(offer) else OfferDetailUiState.NotFound
        }
        .catch { emit(OfferDetailUiState.Error(it.localizedMessage ?: "Error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OfferDetailUiState.Loading
        )

    fun toggleFavorite() {
        viewModelScope.launch {
            toggleFavoriteUseCase(offerId)
        }
    }
}
