package com.olympiades.firststep.domain.validation

enum class ValidationError {
    REQUIRED_NAME,
    INVALID_EMAIL,
    REQUIRED_PHONE,
    INVALID_PHONE,
    REQUIRED_COVER_LETTER,
    REQUIRED_UNIVERSITY,
    REQUIRED_DEGREE
}

data class ApplicationFormValidationResult(
    val nameError: ValidationError? = null,
    val emailError: ValidationError? = null,
    val phoneError: ValidationError? = null,
    val coverLetterError: ValidationError? = null
) {
    val isValid: Boolean
        get() = nameError == null && emailError == null && phoneError == null && coverLetterError == null
}

data class ProfileFormValidationResult(
    val nameError: ValidationError? = null,
    val emailError: ValidationError? = null,
    val phoneError: ValidationError? = null,
    val universityError: ValidationError? = null,
    val degreeError: ValidationError? = null
) {
    val isValid: Boolean
        get() = nameError == null &&
            emailError == null &&
            phoneError == null &&
            universityError == null &&
            degreeError == null
}

object ApplicationValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE_REGEX = Regex("^[+]?[0-9\\s-]{8,18}$")

    fun validateApplicationForm(
        fullName: String,
        email: String,
        phone: String,
        coverLetter: String
    ): ApplicationFormValidationResult {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()
        val trimmedCover = coverLetter.trim()

        val nameError = if (trimmedName.length < 2) ValidationError.REQUIRED_NAME else null
        val emailError = if (!EMAIL_REGEX.matches(trimmedEmail)) ValidationError.INVALID_EMAIL else null
        val phoneError = when {
            trimmedPhone.isEmpty() -> ValidationError.REQUIRED_PHONE
            !PHONE_REGEX.matches(trimmedPhone) -> ValidationError.INVALID_PHONE
            else -> null
        }
        val coverError = if (trimmedCover.length < 10) ValidationError.REQUIRED_COVER_LETTER else null

        return ApplicationFormValidationResult(
            nameError = nameError,
            emailError = emailError,
            phoneError = phoneError,
            coverLetterError = coverError
        )
    }

    fun validateProfileForm(
        fullName: String,
        email: String,
        phone: String,
        university: String,
        degreeLevel: String
    ): ProfileFormValidationResult {
        val nameError = if (fullName.trim().length < 2) ValidationError.REQUIRED_NAME else null
        val emailError = if (!EMAIL_REGEX.matches(email.trim())) ValidationError.INVALID_EMAIL else null
        val phoneError = when {
            phone.trim().isEmpty() -> ValidationError.REQUIRED_PHONE
            !PHONE_REGEX.matches(phone.trim()) -> ValidationError.INVALID_PHONE
            else -> null
        }
        val univError = if (university.trim().isEmpty()) ValidationError.REQUIRED_UNIVERSITY else null
        val degreeError = if (degreeLevel.trim().isEmpty()) ValidationError.REQUIRED_DEGREE else null

        return ProfileFormValidationResult(
            nameError = nameError,
            emailError = emailError,
            phoneError = phoneError,
            universityError = univError,
            degreeError = degreeError
        )
    }
}
