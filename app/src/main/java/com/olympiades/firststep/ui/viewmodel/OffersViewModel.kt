package com.olympiades.firststep.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.domain.model.Company
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.OfferSortOption
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.domain.repository.FirstStepRepository
import com.olympiades.firststep.domain.usecase.GetDomainsAndLocationsUseCase
import com.olympiades.firststep.domain.usecase.GetFilteredOffersUseCase
import com.olympiades.firststep.domain.usecase.ManageOfferUseCase
import com.olympiades.firststep.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OffersUiState {
    data object Loading : OffersUiState
    data class Success(
        val offers: List<Offer>,
        val domains: List<Domain>,
        val locations: List<Location>,
        val companies: List<Company>,
        val criteria: OfferFilterCriteria
    ) : OffersUiState
    data class Error(val message: String) : OffersUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class OffersViewModel(
    private val repository: FirstStepRepository,
    private val getFilteredOffersUseCase: GetFilteredOffersUseCase,
    private val getDomainsAndLocationsUseCase: GetDomainsAndLocationsUseCase,
    private val manageOfferUseCase: ManageOfferUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val criteriaFlow = MutableStateFlow(OfferFilterCriteria())
    private val retryTrigger = MutableStateFlow(0)
    private var saveFiltersJob: Job? = null
    private var userHasInteracted = false

    private val _offerCreatedEvents = MutableSharedFlow<Unit>()
    val offerCreatedEvents: SharedFlow<Unit> = _offerCreatedEvents.asSharedFlow()

    // Synchronous Compose Snapshot state for TextField to guarantee zero IME cursor reset
    var searchQuery by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            runCatching { repository.ensureInitialized() }
            val savedPrefs = runCatching { repository.observePreferences().first() }.getOrNull()
            if (savedPrefs != null && !userHasInteracted) {
                criteriaFlow.value = savedPrefs.lastFilters
                searchQuery = savedPrefs.lastFilters.query
            }
        }
    }

    val uiState: StateFlow<OffersUiState> = retryTrigger
        .flatMapLatest {
            criteriaFlow.flatMapLatest { criteria ->
                combine(
                    getFilteredOffersUseCase(criteria),
                    getDomainsAndLocationsUseCase.domains(),
                    getDomainsAndLocationsUseCase.locations(),
                    getDomainsAndLocationsUseCase.companies()
                ) { offers, domains, locations, companies ->
                    OffersUiState.Success(
                        offers = offers,
                        domains = domains,
                        locations = locations,
                        companies = companies,
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
        userHasInteracted = true
        searchQuery = query
        updateCriteria { it.copy(query = query) }
    }

    fun resetToAll() {
        userHasInteracted = true
        searchQuery = ""
        updateCriteria {
            OfferFilterCriteria(sortOption = it.sortOption)
        }
    }

    fun togglePaidOnly() {
        userHasInteracted = true
        updateCriteria { it.copy(paidOnly = !it.paidOnly) }
    }

    fun toggleOpenOnly() {
        userHasInteracted = true
        updateCriteria { it.copy(openOnly = !it.openOnly) }
    }

    fun toggleMinStipendFilter(threshold: Int = 25_000) {
        userHasInteracted = true
        updateCriteria {
            val next = if (it.minStipendDzd == threshold) null else threshold
            it.copy(minStipendDzd = next)
        }
    }

    fun selectDomain(domainId: Long?) {
        userHasInteracted = true
        updateCriteria { it.copy(domainId = domainId) }
    }

    fun selectLocation(locationId: Long?) {
        userHasInteracted = true
        updateCriteria { it.copy(locationId = locationId) }
    }

    fun selectSortOption(sortOption: OfferSortOption) {
        userHasInteracted = true
        updateCriteria { it.copy(sortOption = sortOption) }
    }

    fun resetAllFilters() {
        userHasInteracted = true
        searchQuery = ""
        updateCriteria { OfferFilterCriteria() }
    }

    fun toggleFavorite(offerId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(offerId)
        }
    }

    fun createOffer(
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
            val result = manageOfferUseCase.createOffer(
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
                _offerCreatedEvents.emit(Unit)
            }
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
        saveFiltersJob?.cancel()
        saveFiltersJob = viewModelScope.launch {
            delay(200)
            repository.saveLastFilters(updated)
        }
    }
}
