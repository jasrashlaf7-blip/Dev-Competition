package com.olympiades.firststep.domain.model

enum class OfferStatus {
    OPEN,
    CLOSED
}

enum class ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    INTERVIEW_SCHEDULED,
    ACCEPTED,
    REJECTED,
    CANCELLED;

    val isFinal: Boolean
        get() = this == ACCEPTED || this == REJECTED || this == CANCELLED
}

enum class OfferSortOption {
    NEWEST,
    CLOSEST_DEADLINE
}

enum class AppLanguage(val code: String) {
    FR("fr"),
    AR("ar")
}

enum class AppThemeMode(val key: String) {
    LIGHT("light"),
    DARK("dark")
}
