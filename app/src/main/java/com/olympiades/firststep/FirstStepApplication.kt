package com.olympiades.firststep

import android.app.Application
import com.olympiades.firststep.data.local.FirstStepDatabase
import com.olympiades.firststep.data.local.SeedDataLoader
import com.olympiades.firststep.data.preferences.UserPreferencesDataStore
import com.olympiades.firststep.data.repository.FirstStepRepositoryImpl
import com.olympiades.firststep.domain.repository.FirstStepRepository
import com.olympiades.firststep.domain.usecase.GetDomainsAndLocationsUseCase
import com.olympiades.firststep.domain.usecase.GetFilteredOffersUseCase
import com.olympiades.firststep.domain.usecase.GetOfferDetailUseCase
import com.olympiades.firststep.domain.usecase.StudentProfileUseCase
import com.olympiades.firststep.domain.usecase.SubmitApplicationUseCase
import com.olympiades.firststep.domain.usecase.ToggleFavoriteUseCase
import com.olympiades.firststep.domain.usecase.UpdateApplicationStatusUseCase

class FirstStepContainer(application: Application) {
    val database: FirstStepDatabase by lazy {
        FirstStepDatabase.getInstance(application)
    }

    private val seedDataLoader: SeedDataLoader by lazy {
        SeedDataLoader(application, database)
    }

    val preferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(application)
    }

    val repository: FirstStepRepository by lazy {
        FirstStepRepositoryImpl(
            database = database,
            seedDataLoader = seedDataLoader,
            preferencesDataStore = preferencesDataStore
        )
    }

    val getFilteredOffersUseCase by lazy { GetFilteredOffersUseCase(repository) }
    val getOfferDetailUseCase by lazy { GetOfferDetailUseCase(repository) }
    val getDomainsAndLocationsUseCase by lazy { GetDomainsAndLocationsUseCase(repository) }
    val toggleFavoriteUseCase by lazy { ToggleFavoriteUseCase(repository) }
    val submitApplicationUseCase by lazy { SubmitApplicationUseCase(repository) }
    val updateApplicationStatusUseCase by lazy { UpdateApplicationStatusUseCase(repository) }
    val studentProfileUseCase by lazy { StudentProfileUseCase(repository) }
}

class FirstStepApplication : Application() {
    lateinit var container: FirstStepContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = FirstStepContainer(this)
    }
}
