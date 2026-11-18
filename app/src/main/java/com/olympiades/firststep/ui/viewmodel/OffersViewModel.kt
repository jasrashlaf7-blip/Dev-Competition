package com.olympiades.firststep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.OfferSortOption
import com.olympiades.firststep.domain.repository.FirstStepRepository
import com.olympiades.firststep.domain.usecase.GetDomainsAndLocationsUseCase
import com.olympiades.firststep.domain.usecase.GetFilteredOffersUseCase
import com.olympiades.firststep.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OffersUiState {
    data object Loading : OffersUiState
    data class Success(
        val offers: List<Offer>,
        val domains: List<Domain>,
        val locations: List<Location>,
        val criteria: OfferFilterCriteria
    ) : OffersUiState
    data class Error(val message: String) : OffersUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class OffersViewModel(
    private val repository: FirstStepRepository,
    private val getFilteredOffersUseCase: GetFilteredOffersUseCase,
    private val getDomainsAndLocationsUseCase: GetDomainsAndLocationsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val criteriaFlow = MutableStateFlow(OfferFilterCriteria())
    private val retryTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            runCatching { repository.ensureInitialized() }
            val savedPrefs = runCatching { repository.observePreferences().first() }.getOrNull()
            if (savedPrefs != null) {
                criteriaFlow.value = savedPrefs.lastFilters
            }
        }
    }

    val uiState: StateFlow<OffersUiState> = retryTrigger
        .flatMapLatest {
            criteriaFlow.flatMapLatest { criteria ->
                combine(
                    getFilteredOffersUseCase(criteria),
                    getDomainsAndLocationsUseCase.domains(),
                    getDomainsAndLocationsUseCase.locations()
                ) { offers, domains, locations ->
                    OffersUiState.Success(
                        offers = offers,
                        domains = domains,
                        locations = locations,
                        criteria = criteria
                    ) as OffersUiState
                }
            }
        }
        .catch { emit(OffersUiState.Error(it.localizedMessage ?: "Database error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OffersUiState.Loading
        )

    fun updateQuery(query: String) {
        updateCriteria { it.copy(query = query) }
    }

    fun resetToAll() {
        updateCriteria {
            OfferFilterCriteria(sortOption = it.sortOption)
        }
    }

    fun togglePaidOnly() {
        updateCriteria { it.copy(paidOnly = !it.paidOnly) }
    }

    fun toggleOpenOnly() {
        updateCriteria { it.copy(openOnly = !it.openOnly) }
    }

    fun selectDomain(domainId: Long?) {
        updateCriteria { it.copy(domainId = domainId) }
    }

    fun selectLocation(locationId: Long?) {
        updateCriteria { it.copy(locationId = locationId) }
    }

    fun selectSortOption(sortOption: OfferSortOption) {
        updateCriteria { it.copy(sortOption = sortOption) }
    }

    fun resetAllFilters() {
        updateCriteria { OfferFilterCriteria() }
    }

    fun toggleFavorite(offerId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(offerId)
        }
    }

    fun retry() {
        viewModelScope.launch {
            runCatching { repository.ensureInitialized() }
            retryTrigger.value += 1
        }
    }

    private fun updateCriteria(transform: (OfferFilterCriteria) -> OfferFilterCriteria) {
        val updated = transform(criteriaFlow.value)
        criteriaFlow.value = updated
        viewModelScope.launch {
            repository.saveLastFilters(updated)
        }
    }
}
