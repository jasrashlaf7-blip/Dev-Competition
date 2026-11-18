package com.olympiades.firststep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.olympiades.firststep.FirstStepContainer

class FirstStepViewModelFactory(
    private val container: FirstStepContainer,
    private val offerId: Long? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppPreferencesViewModel::class.java) -> {
                AppPreferencesViewModel(container.repository) as T
            }
            modelClass.isAssignableFrom(OffersViewModel::class.java) -> {
                OffersViewModel(
                    repository = container.repository,
                    getFilteredOffersUseCase = container.getFilteredOffersUseCase,
                    getDomainsAndLocationsUseCase = container.getDomainsAndLocationsUseCase,
                    manageOfferUseCase = container.manageOfferUseCase,
                    toggleFavoriteUseCase = container.toggleFavoriteUseCase
                ) as T
            }
            modelClass.isAssignableFrom(OfferDetailViewModel::class.java) -> {
                OfferDetailViewModel(
                    offerId = requireNotNull(offerId),
                    getOfferDetailUseCase = container.getOfferDetailUseCase,
                    getDomainsAndLocationsUseCase = container.getDomainsAndLocationsUseCase,
                    manageOfferUseCase = container.manageOfferUseCase,
                    toggleFavoriteUseCase = container.toggleFavoriteUseCase
                ) as T
            }
            modelClass.isAssignableFrom(ApplicationFormViewModel::class.java) -> {
                ApplicationFormViewModel(
                    offerId = requireNotNull(offerId),
                    getOfferDetailUseCase = container.getOfferDetailUseCase,
                    studentProfileUseCase = container.studentProfileUseCase,
                    submitApplicationUseCase = container.submitApplicationUseCase
                ) as T
            }
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> {
                FavoritesViewModel(container.toggleFavoriteUseCase) as T
            }
            modelClass.isAssignableFrom(ApplicationsViewModel::class.java) -> {
                ApplicationsViewModel(container.updateApplicationStatusUseCase) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(
                    repository = container.repository,
                    studentProfileUseCase = container.studentProfileUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
