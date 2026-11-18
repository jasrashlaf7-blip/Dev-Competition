package com.olympiades.firststep.domain.usecase

import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.StudentProfile
import com.olympiades.firststep.domain.repository.FirstStepRepository
import kotlinx.coroutines.flow.Flow

class GetFilteredOffersUseCase(private val repository: FirstStepRepository) {
    operator fun invoke(criteria: OfferFilterCriteria): Flow<List<Offer>> =
        repository.observeFilteredOffers(criteria)
}

class GetOfferDetailUseCase(private val repository: FirstStepRepository) {
    operator fun invoke(offerId: Long): Flow<Offer?> =
        repository.observeOfferById(offerId)
}

class GetDomainsAndLocationsUseCase(private val repository: FirstStepRepository) {
    fun domains(): Flow<List<Domain>> = repository.observeDomains()
    fun locations(): Flow<List<Location>> = repository.observeLocations()
}

class ToggleFavoriteUseCase(private val repository: FirstStepRepository) {
    suspend operator fun invoke(offerId: Long): Boolean = repository.toggleFavorite(offerId)
    suspend fun restore(offerId: Long) = repository.restoreFavorite(offerId)
    fun observeFavorites(): Flow<List<FavoriteOffer>> = repository.observeFavorites()
    fun observeFavoriteCount(): Flow<Int> = repository.observeFavoriteCount()
}

class SubmitApplicationUseCase(private val repository: FirstStepRepository) {
    suspend operator fun invoke(
        offerId: Long,
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        coverLetter: String,
        cvUri: String,
        initialNote: String
    ): Result<String> = repository.submitApplication(
        offerId = offerId,
        applicantName = applicantName,
        applicantEmail = applicantEmail,
        applicantPhone = applicantPhone,
        coverLetter = coverLetter,
        cvUri = cvUri,
        initialNote = initialNote
    )
}

class UpdateApplicationStatusUseCase(private val repository: FirstStepRepository) {
    fun observeApplications(statusFilter: ApplicationStatus?): Flow<List<ApplicationWithDetails>> =
        repository.observeApplications(statusFilter)

    suspend operator fun invoke(
        applicationId: Long,
        newStatus: ApplicationStatus,
        note: String
    ): Result<Unit> = repository.updateApplicationStatus(applicationId, newStatus, note)
}

class StudentProfileUseCase(private val repository: FirstStepRepository) {
    fun observeProfile(): Flow<StudentProfile?> = repository.observeStudentProfile()
    suspend fun updateProfile(profile: StudentProfile): Result<Unit> =
        repository.updateStudentProfile(profile)
}
