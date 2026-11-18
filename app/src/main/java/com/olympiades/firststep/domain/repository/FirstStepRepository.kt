package com.olympiades.firststep.domain.repository

import com.olympiades.firststep.data.preferences.UserPreferencesState
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.StudentProfile
import kotlinx.coroutines.flow.Flow

interface FirstStepRepository {
    suspend fun ensureInitialized()

    fun observeFilteredOffers(criteria: OfferFilterCriteria): Flow<List<Offer>>
    fun observeOfferById(offerId: Long): Flow<Offer?>
    fun observeDomains(): Flow<List<Domain>>
    fun observeLocations(): Flow<List<Location>>

    fun observeFavorites(): Flow<List<FavoriteOffer>>
    fun observeFavoriteCount(): Flow<Int>
    suspend fun toggleFavorite(offerId: Long): Boolean
    suspend fun restoreFavorite(offerId: Long)

    fun observeApplications(statusFilter: ApplicationStatus?): Flow<List<ApplicationWithDetails>>
    suspend fun submitApplication(
        offerId: Long,
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        coverLetter: String,
        cvUri: String,
        initialNote: String
    ): Result<String>

    suspend fun updateApplicationStatus(
        applicationId: Long,
        newStatus: ApplicationStatus,
        note: String
    ): Result<Unit>

    fun observeStudentProfile(): Flow<StudentProfile?>
    suspend fun updateStudentProfile(profile: StudentProfile): Result<Unit>

    fun observePreferences(): Flow<UserPreferencesState>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThemeMode(themeMode: AppThemeMode)
    suspend fun saveLastFilters(criteria: OfferFilterCriteria)
}
