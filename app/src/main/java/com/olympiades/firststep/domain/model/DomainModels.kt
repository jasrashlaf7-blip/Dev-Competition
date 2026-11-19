package com.olympiades.firststep.domain.model

data class Domain(
    val id: Long,
    val nameFr: String,
    val nameAr: String
)

data class Location(
    val id: Long,
    val cityFr: String,
    val cityAr: String,
    val regionFr: String,
    val regionAr: String
)

data class Company(
    val id: Long,
    val name: String,
    val sectorFr: String,
    val sectorAr: String,
    val descriptionFr: String,
    val descriptionAr: String,
    val website: String,
    val logoUrl: String
)

data class Skill(
    val id: Long,
    val nameFr: String,
    val nameAr: String
)

data class StudentProfile(
    val id: Long = 1L,
    val fullName: String,
    val email: String,
    val phone: String,
    val university: String,
    val degreeLevel: String,
    val bio: String,
    val cvUri: String,
    val updatedAt: String
)

data class Offer(
    val id: Long,
    val company: Company,
    val domain: Domain,
    val location: Location,
    val titleFr: String,
    val titleAr: String,
    val descriptionFr: String,
    val descriptionAr: String,
    val durationMonths: Int,
    val isPaid: Boolean,
    val monthlyStipendDzd: Int = 0,
    val startDate: String = "",
    val status: OfferStatus,
    val deadline: String,
    val createdAt: String,
    val skills: List<Skill>,
    val isFavorite: Boolean,
    val applicationStatus: ApplicationStatus?,
    val applicationReference: String?
) {
    val referenceCode: String
        get() = "OFF-2026-%03d".format(id)

    fun isExpired(todayIso: String): Boolean = deadline < todayIso

    fun canApply(todayIso: String): Boolean =
        status == OfferStatus.OPEN && !isExpired(todayIso) && applicationStatus == null
}

data class FavoriteOffer(
    val favoriteId: Long,
    val addedAt: String,
    val personalNote: String = "",
    val offer: Offer
)

data class StatusHistoryItem(
    val id: Long,
    val applicationId: Long,
    val oldStatus: ApplicationStatus?,
    val newStatus: ApplicationStatus,
    val changedAt: String,
    val note: String
)

data class ApplicationWithDetails(
    val id: Long,
    val reference: String,
    val offer: Offer,
    val applicantName: String,
    val applicantEmail: String,
    val applicantPhone: String,
    val coverLetter: String,
    val cvUri: String,
    val currentStatus: ApplicationStatus,
    val appliedAt: String,
    val history: List<StatusHistoryItem>
) {
    fun daysSinceApplied(todayIso: String): Int =
        DateTimeHelper.daysBetween(appliedAt, todayIso).coerceAtLeast(0)

    fun isUnansweredOver7Days(todayIso: String): Boolean =
        currentStatus == ApplicationStatus.SUBMITTED &&
            DateTimeHelper.isOlderThanDays(appliedAt, todayIso, 7)
}

data class OfferFilterCriteria(
    val query: String = "",
    val paidOnly: Boolean = false,
    val openOnly: Boolean = false,
    val minStipendDzd: Int? = null,
    val domainId: Long? = null,
    val locationId: Long? = null,
    val sortOption: OfferSortOption = OfferSortOption.NEWEST
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() ||
            paidOnly ||
            openOnly ||
            (minStipendDzd != null && minStipendDzd > 0) ||
            domainId != null ||
            locationId != null
}
