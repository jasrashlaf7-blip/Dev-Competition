package com.olympiades.firststep.data.repository

import com.olympiades.firststep.data.local.FirstStepDatabase
import com.olympiades.firststep.data.local.SeedDataLoader
import com.olympiades.firststep.data.local.entities.ApplicationEntity
import com.olympiades.firststep.data.local.entities.FavoriteEntity
import com.olympiades.firststep.data.preferences.UserPreferencesDataStore
import com.olympiades.firststep.data.preferences.UserPreferencesState
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.DateTimeHelper
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.OfferSortOption
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.domain.model.StudentProfile
import com.olympiades.firststep.domain.repository.FirstStepRepository
import com.olympiades.firststep.domain.validation.ApplicationStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class ApplicationSubmissionError(message: String) : IllegalStateException(message) {
    data object OfferNotFound : ApplicationSubmissionError("Offer not found")
    data object OfferClosed : ApplicationSubmissionError("Offer is closed")
    data object DeadlinePassed : ApplicationSubmissionError("Offer deadline has passed")
    data object AlreadyApplied : ApplicationSubmissionError("Application already exists for this offer")
}

sealed class StatusTransitionError(message: String) : IllegalStateException(message) {
    data object ApplicationNotFound : StatusTransitionError("Application not found")
    data object FinalStateLocked : StatusTransitionError("Cannot revert or modify a finalized application")
    data object InvalidTransition : StatusTransitionError("Invalid status transition")
}

class FirstStepRepositoryImpl(
    private val database: FirstStepDatabase,
    private val seedDataLoader: SeedDataLoader,
    private val preferencesDataStore: UserPreferencesDataStore
) : FirstStepRepository {

    private val offerDao = database.offerDao()
    private val favoriteDao = database.favoriteDao()
    private val applicationDao = database.applicationDao()
    private val profileDao = database.studentProfileDao()

    override suspend fun ensureInitialized() {
        seedDataLoader.ensureSeeded()
    }

    override fun observeFilteredOffers(criteria: OfferFilterCriteria): Flow<List<Offer>> {
        val trimmedQuery = criteria.query.trim()
        val todayIso = DateTimeHelper.todayIso()
        val flow = when (criteria.sortOption) {
            OfferSortOption.NEWEST -> offerDao.getFilteredOffersOrderByNewest(
                query = trimmedQuery,
                domainId = criteria.domainId,
                locationId = criteria.locationId,
                paidOnly = criteria.paidOnly,
                openOnly = criteria.openOnly,
                todayIso = todayIso
            )
            OfferSortOption.CLOSEST_DEADLINE -> offerDao.getFilteredOffersOrderByDeadline(
                query = trimmedQuery,
                domainId = criteria.domainId,
                locationId = criteria.locationId,
                paidOnly = criteria.paidOnly,
                openOnly = criteria.openOnly,
                todayIso = todayIso
            )
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    override fun observeOfferById(offerId: Long): Flow<Offer?> =
        offerDao.observeOfferById(offerId).map { it?.toDomain() }

    override fun observeDomains(): Flow<List<Domain>> =
        offerDao.observeAllDomains().map { list -> list.map { it.toDomain() } }

    override fun observeLocations(): Flow<List<Location>> =
        offerDao.observeAllLocations().map { list -> list.map { it.toDomain() } }

    override fun observeFavorites(): Flow<List<FavoriteOffer>> =
        favoriteDao.observeAllFavorites().map { list -> list.map { it.toDomain() } }

    override fun observeFavoriteCount(): Flow<Int> =
        favoriteDao.observeFavoriteCount()

    override suspend fun toggleFavorite(offerId: Long): Boolean {
        val existing = favoriteDao.getFavoriteByOfferId(offerId)
        return if (existing != null) {
            favoriteDao.deleteFavoriteByOfferId(offerId)
            false
        } else {
            favoriteDao.insertFavorite(
                FavoriteEntity(
                    offerId = offerId,
                    addedAt = DateTimeHelper.nowTimestamp()
                )
            )
            true
        }
    }

    override suspend fun restoreFavorite(offerId: Long) {
        if (favoriteDao.getFavoriteByOfferId(offerId) == null) {
            favoriteDao.insertFavorite(
                FavoriteEntity(
                    offerId = offerId,
                    addedAt = DateTimeHelper.nowTimestamp()
                )
            )
        }
    }

    override fun observeApplications(statusFilter: ApplicationStatus?): Flow<List<ApplicationWithDetails>> =
        applicationDao.observeApplications(statusFilter).map { list -> list.map { it.toDomain() } }

    override suspend fun submitApplication(
        offerId: Long,
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        coverLetter: String,
        cvUri: String,
        initialNote: String
    ): Result<String> {
        return runCatching {
            val offerWithRel = offerDao.getOfferByIdOnce(offerId)
                ?: throw ApplicationSubmissionError.OfferNotFound

            if (offerWithRel.offer.status == OfferStatus.CLOSED) {
                throw ApplicationSubmissionError.OfferClosed
            }
            if (offerWithRel.offer.deadline < DateTimeHelper.todayIso()) {
                throw ApplicationSubmissionError.DeadlinePassed
            }
            if (applicationDao.getApplicationByOfferId(offerId) != null) {
                throw ApplicationSubmissionError.AlreadyApplied
            }

            var nextSeq = applicationDao.getMaxApplicationId() + 1L
            var reference = "FS-2026-%04d".format(nextSeq)
            while (applicationDao.referenceExists(reference)) {
                nextSeq += 1L
                reference = "FS-2026-%04d".format(nextSeq)
            }

            val timestamp = DateTimeHelper.nowTimestamp()
            val entity = ApplicationEntity(
                reference = reference,
                offerId = offerId,
                applicantName = applicantName.trim(),
                applicantEmail = applicantEmail.trim(),
                applicantPhone = applicantPhone.trim(),
                coverLetter = coverLetter.trim(),
                cvUri = cvUri.trim(),
                currentStatus = ApplicationStatus.SUBMITTED,
                appliedAt = timestamp
            )
            applicationDao.submitApplicationWithHistory(
                application = entity,
                initialNote = initialNote
            )
            reference
        }
    }

    override suspend fun updateApplicationStatus(
        applicationId: Long,
        newStatus: ApplicationStatus,
        note: String
    ): Result<Unit> {
        return runCatching {
            val existing = applicationDao.getApplicationById(applicationId)
                ?: throw StatusTransitionError.ApplicationNotFound
            val oldStatus = existing.application.currentStatus
            if (oldStatus.isFinal) {
                throw StatusTransitionError.FinalStateLocked
            }
            if (!ApplicationStateMachine.canTransition(oldStatus, newStatus)) {
                throw StatusTransitionError.InvalidTransition
            }
            applicationDao.changeStatusWithHistory(
                applicationId = applicationId,
                oldStatus = oldStatus,
                newStatus = newStatus,
                changedAt = DateTimeHelper.nowTimestamp(),
                note = note.trim()
            )
        }
    }

    override fun observeStudentProfile(): Flow<StudentProfile?> =
        profileDao.observeProfile().map { it?.toDomain() }

    override suspend fun updateStudentProfile(profile: StudentProfile): Result<Unit> {
        return runCatching {
            profileDao.upsertProfile(profile.toEntity(DateTimeHelper.nowTimestamp()))
        }
    }

    override fun observePreferences(): Flow<UserPreferencesState> =
        preferencesDataStore.preferencesFlow

    override suspend fun setLanguage(language: AppLanguage) {
        preferencesDataStore.setLanguage(language)
    }

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        preferencesDataStore.setThemeMode(themeMode)
    }

    override suspend fun saveLastFilters(criteria: OfferFilterCriteria) {
        preferencesDataStore.saveFilters(criteria)
    }
}
