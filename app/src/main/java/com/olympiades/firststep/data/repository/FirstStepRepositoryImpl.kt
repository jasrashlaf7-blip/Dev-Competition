package com.olympiades.firststep.data.repository

import com.olympiades.firststep.data.local.FirstStepDatabase
import com.olympiades.firststep.data.local.SeedDataLoader
import com.olympiades.firststep.data.local.entities.ApplicationEntity
import com.olympiades.firststep.data.local.entities.FavoriteEntity
import com.olympiades.firststep.data.local.entities.OfferEntity
import com.olympiades.firststep.data.preferences.UserPreferencesDataStore
import com.olympiades.firststep.data.preferences.UserPreferencesState
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.Company
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
import java.text.Normalizer
import java.util.Locale
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
        val searchTokens = tokenizeQuery(criteria.query)
        val todayIso = DateTimeHelper.todayIso()
        val flow = when (criteria.sortOption) {
            OfferSortOption.NEWEST -> offerDao.getFilteredOffersOrderByNewest(
                query = "",
                domainId = criteria.domainId,
                locationId = criteria.locationId,
                paidOnly = criteria.paidOnly,
                openOnly = criteria.openOnly,
                todayIso = todayIso
            )
            OfferSortOption.CLOSEST_DEADLINE -> offerDao.getFilteredOffersOrderByDeadline(
                query = "",
                domainId = criteria.domainId,
                locationId = criteria.locationId,
                paidOnly = criteria.paidOnly,
                openOnly = criteria.openOnly,
                todayIso = todayIso
            )
        }
        return flow.map { list ->
            val domainOffers = list.map { it.toDomain() }
            if (searchTokens.isEmpty()) {
                domainOffers
            } else {
                domainOffers
                    .mapNotNull { offer ->
                        val tier = offer.searchMatchTier(searchTokens)
                        if (tier >= 0) offer to tier else null
                    }
                    .sortedWith(compareBy { it.second })
                    .map { it.first }
            }
        }
    }

    override fun observeOfferById(offerId: Long): Flow<Offer?> =
        offerDao.observeOfferById(offerId).map { it?.toDomain() }

    override fun observeDomains(): Flow<List<Domain>> =
        offerDao.observeAllDomains().map { list -> list.map { it.toDomain() } }

    override fun observeLocations(): Flow<List<Location>> =
        offerDao.observeAllLocations().map { list -> list.map { it.toDomain() } }

    override fun observeCompanies(): Flow<List<Company>> =
        offerDao.observeAllCompanies().map { list -> list.map { it.toDomain() } }

    override suspend fun createOffer(
        companyId: Long,
        domainId: Long,
        locationId: Long,
        titleFr: String,
        titleAr: String,
        descriptionFr: String,
        descriptionAr: String,
        durationMonths: Int,
        isPaid: Boolean,
        status: OfferStatus,
        deadline: String
    ): Result<Long> {
        return runCatching {
            val cleanTitleFr = titleFr.trim()
            val cleanTitleAr = titleAr.trim().ifEmpty { cleanTitleFr }
            val cleanDescFr = descriptionFr.trim()
            val cleanDescAr = descriptionAr.trim().ifEmpty { cleanDescFr }
            offerDao.insertOffer(
                OfferEntity(
                    companyId = companyId,
                    domainId = domainId,
                    locationId = locationId,
                    titleFr = cleanTitleFr,
                    titleAr = cleanTitleAr,
                    descriptionFr = cleanDescFr,
                    descriptionAr = cleanDescAr,
                    durationMonths = durationMonths.coerceIn(1, 24),
                    isPaid = isPaid,
                    status = status,
                    deadline = deadline.trim(),
                    createdAt = DateTimeHelper.nowTimestamp()
                )
            )
        }
    }

    override suspend fun updateOffer(
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
        status: OfferStatus,
        deadline: String
    ): Result<Unit> {
        return runCatching {
            val cleanTitleFr = titleFr.trim()
            val cleanTitleAr = titleAr.trim().ifEmpty { cleanTitleFr }
            val cleanDescFr = descriptionFr.trim()
            val cleanDescAr = descriptionAr.trim().ifEmpty { cleanDescFr }
            offerDao.updateOffer(
                offerId = offerId,
                companyId = companyId,
                domainId = domainId,
                locationId = locationId,
                titleFr = cleanTitleFr,
                titleAr = cleanTitleAr,
                descriptionFr = cleanDescFr,
                descriptionAr = cleanDescAr,
                durationMonths = durationMonths.coerceIn(1, 24),
                isPaid = isPaid,
                status = status.name,
                deadline = deadline.trim()
            )
        }
    }

    override suspend fun deleteOffer(offerId: Long): Result<Unit> {
        return runCatching {
            offerDao.deleteOfferById(offerId)
        }
    }

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

    override suspend fun updateApplicationDetails(
        applicationId: Long,
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        coverLetter: String,
        cvUri: String,
        editNote: String
    ): Result<Unit> {
        return runCatching {
            val existing = applicationDao.getApplicationById(applicationId)
                ?: throw StatusTransitionError.ApplicationNotFound
            val currentStatus = existing.application.currentStatus
            if (currentStatus.isFinal) {
                throw StatusTransitionError.FinalStateLocked
            }
            applicationDao.updateApplicationDetailsWithHistory(
                applicationId = applicationId,
                applicantName = applicantName.trim(),
                applicantEmail = applicantEmail.trim(),
                applicantPhone = applicantPhone.trim(),
                coverLetter = coverLetter.trim(),
                cvUri = cvUri.trim(),
                currentStatus = currentStatus,
                changedAt = DateTimeHelper.nowTimestamp(),
                note = editNote.trim()
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

    companion object {
        private val TOKEN_SPLIT_REGEX = Regex("[\\s\\-_/.,;:'’\"()\\[\\]{}|\\\\]+")
        private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val ARABIC_DIACRITICS_AND_TATWEEL_REGEX = Regex("[\\u0640\\u064B-\\u065F\\u0670]")

        internal fun normalizeSearchText(input: String): String {
            if (input.isBlank()) return ""
            val preNormalized = input
                .replace("œ", "oe")
                .replace("Œ", "oe")
                .replace("æ", "ae")
                .replace("Æ", "ae")
            val decomposed = Normalizer.normalize(preNormalized, Normalizer.Form.NFD)
            val withoutMarks = DIACRITICS_REGEX.replace(decomposed, "")
            val arabicCleaned = ARABIC_DIACRITICS_AND_TATWEEL_REGEX.replace(withoutMarks, "")
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ٱ', 'ا')
                .replace('ة', 'ه')
                .replace('ى', 'ي')
                .replace('ؤ', 'و')
                .replace('ئ', 'ي')
            return arabicCleaned.lowercase(Locale.ROOT)
        }

        internal fun tokenizeQuery(query: String): List<String> {
            val normalized = normalizeSearchText(query).trim()
            if (normalized.isEmpty()) return emptyList()
            return normalized.split(TOKEN_SPLIT_REGEX).filter { it.isNotBlank() }
        }

        private fun normalizeIndexedField(raw: String): Pair<String, String> {
            val spaced = TOKEN_SPLIT_REGEX.replace(normalizeSearchText(raw), " ")
            val compact = spaced.replace(" ", "")
            return spaced to compact
        }

        private fun Pair<String, String>.containsToken(token: String): Boolean {
            return first.contains(token) || second.contains(token)
        }

        /**
         * Returns:
         * - `0` if every search token matches in the offer title (FR or AR), in any word position
         * - `1` if at least one token matches in the title and all remaining tokens match across metadata/description
         * - `2` if all tokens match across company, domain, city, region, skills, or description
         * - `-1` if any token fails to match
         */
        private fun Offer.searchMatchTier(tokens: List<String>): Int {
            val titleIndexed = normalizeIndexedField("$titleFr $titleAr")
            val fullRaw = buildString {
                append(titleFr).append(' ')
                append(titleAr).append(' ')
                append(descriptionFr).append(' ')
                append(descriptionAr).append(' ')
                append(company.name).append(' ')
                append(company.sectorFr).append(' ')
                append(company.sectorAr).append(' ')
                append(domain.nameFr).append(' ')
                append(domain.nameAr).append(' ')
                append(location.cityFr).append(' ')
                append(location.cityAr).append(' ')
                append(location.regionFr).append(' ')
                append(location.regionAr).append(' ')
                skills.forEach { skill ->
                    append(skill.nameFr).append(' ')
                    append(skill.nameAr).append(' ')
                }
            }
            val fullIndexed = normalizeIndexedField(fullRaw)

            if (!tokens.all { token -> fullIndexed.containsToken(token) }) {
                return -1
            }
            val allInTitle = tokens.all { token -> titleIndexed.containsToken(token) }
            if (allInTitle) return 0

            val anyInTitle = tokens.any { token -> titleIndexed.containsToken(token) }
            return if (anyInTitle) 1 else 2
        }
    }
}
