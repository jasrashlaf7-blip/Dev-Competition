package com.olympiades.firststep.domain.usecase

import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.Company
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.OfferStatus
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
    fun companies(): Flow<List<Company>> = repository.observeCompanies()
}

class ManageOfferUseCase(private val repository: FirstStepRepository) {
    suspend fun createOffer(
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
    ): Result<Long> = repository.createOffer(
        companyId = companyId,
        domainId = domainId,
        locationId = locationId,
        titleFr = titleFr,
        titleAr = titleAr,
        descriptionFr = descriptionFr,
        descriptionAr = descriptionAr,
        durationMonths = durationMonths,
        isPaid = isPaid,
        monthlyStipendDzd = monthlyStipendDzd,
        startDate = startDate,
        status = status,
        deadline = deadline
    )

    suspend fun updateOffer(
        offerId: Long,
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
    ): Result<Unit> = repository.updateOffer(
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
        monthlyStipendDzd = monthlyStipendDzd,
        startDate = startDate,
        status = status,
        deadline = deadline
    )

    suspend fun deleteOffer(offerId: Long): Result<Unit> = repository.deleteOffer(offerId)
}

class ToggleFavoriteUseCase(private val repository: FirstStepRepository) {
    suspend operator fun invoke(offerId: Long): Boolean = repository.toggleFavorite(offerId)
    suspend fun restore(offerId: Long) = repository.restoreFavorite(offerId)
    suspend fun updatePersonalNote(offerId: Long, note: String) = repository.updateFavoriteNote(offerId, note)
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

    suspend fun updateDetails(
        applicationId: Long,
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        coverLetter: String,
        cvUri: String,
        editNote: String
    ): Result<Unit> = repository.updateApplicationDetails(
        applicationId = applicationId,
        applicantName = applicantName,
        applicantEmail = applicantEmail,
        applicantPhone = applicantPhone,
        coverLetter = coverLetter,
        cvUri = cvUri,
        editNote = editNote
    )
}

class StudentProfileUseCase(private val repository: FirstStepRepository) {
    fun observeProfile(): Flow<StudentProfile?> = repository.observeStudentProfile()
    suspend fun updateProfile(profile: StudentProfile): Result<Unit> =
        repository.updateStudentProfile(profile)
}
