package com.olympiades.firststep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.domain.model.Company
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.domain.usecase.GetDomainsAndLocationsUseCase
import com.olympiades.firststep.domain.usecase.GetOfferDetailUseCase
import com.olympiades.firststep.domain.usecase.ManageOfferUseCase
import com.olympiades.firststep.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OfferDetailUiState {
    data object Loading : OfferDetailUiState
    data class Success(
        val offer: Offer,
        val domains: List<Domain>,
        val locations: List<Location>,
        val companies: List<Company>
    ) : OfferDetailUiState
    data object NotFound : OfferDetailUiState
    data class Error(val message: String) : OfferDetailUiState
}

sealed interface OfferDetailEvent {
    data object OfferUpdated : OfferDetailEvent
    data object OfferDeleted : OfferDetailEvent
}

class OfferDetailViewModel(
    private val offerId: Long,
    getOfferDetailUseCase: GetOfferDetailUseCase,
    getDomainsAndLocationsUseCase: GetDomainsAndLocationsUseCase,
    private val manageOfferUseCase: ManageOfferUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<OfferDetailEvent>()
    val events: SharedFlow<OfferDetailEvent> = _events.asSharedFlow()

    val uiState: StateFlow<OfferDetailUiState> = combine(
        getOfferDetailUseCase(offerId),
        getDomainsAndLocationsUseCase.domains(),
        getDomainsAndLocationsUseCase.locations(),
        getDomainsAndLocationsUseCase.companies()
    ) { offer, domains, locations, companies ->
        if (offer != null) {
            OfferDetailUiState.Success(
                offer = offer,
                domains = domains,
                locations = locations,
                companies = companies
            )
        } else {
            OfferDetailUiState.NotFound
        }
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

    fun updateOffer(
        companyId: Long,
        domainId: Long,
        locationId: Long,
        titleFr: String,
        titleAr: String,
        descriptionFr: String,
        descriptionAr: String,
        durationMonths: Int,
        isPaid: Boolean,
        monthlyStipendDzd: Int = 0,
        startDate: String = "2026-12-01",
        status: OfferStatus,
        deadline: String
    ) {
        viewModelScope.launch {
            val result = manageOfferUseCase.updateOffer(
                offerId = offerId,
                companyId = companyId,
                domainId = domainId,
                locationId = locationId,
                titleFr = titleFr,
                titleAr = titleAr,
                descriptionFr = descriptionFr,
                descriptionAr = descriptionAr,
                durationMonths = durationMonths,
                isPaid = isPaid,
                monthlyStipendDzd = if (isPaid) monthlyStipendDzd else 0,
                startDate = startDate,
                status = status,
                deadline = deadline
            )
            if (result.isSuccess) {
                _events.emit(OfferDetailEvent.OfferUpdated)
            }
        }
    }

    fun deleteOffer() {
        viewModelScope.launch {
            val result = manageOfferUseCase.deleteOffer(offerId)
            if (result.isSuccess) {
                _events.emit(OfferDetailEvent.OfferDeleted)
            }
        }
    }
}
