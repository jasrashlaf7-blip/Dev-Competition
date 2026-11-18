package com.olympiades.firststep.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.data.repository.ApplicationSubmissionError
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.usecase.GetOfferDetailUseCase
import com.olympiades.firststep.domain.usecase.StudentProfileUseCase
import com.olympiades.firststep.domain.usecase.SubmitApplicationUseCase
import com.olympiades.firststep.domain.validation.ApplicationFormValidationResult
import com.olympiades.firststep.domain.validation.ApplicationValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ApplicationFormUiState(
    val isLoading: Boolean = true,
    val offer: Offer? = null,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val coverLetter: String = "",
    val cvUri: String = "",
    val cvDisplayName: String = "",
    val validation: ApplicationFormValidationResult = ApplicationFormValidationResult(),
    val isSubmitting: Boolean = false,
    val submitError: SubmitFormError? = null
)

enum class SubmitFormError {
    CLOSED_OR_EXPIRED,
    ALREADY_APPLIED,
    GENERIC
}

sealed interface ApplicationFormEvent {
    data class Submitted(val reference: String) : ApplicationFormEvent
}

class ApplicationFormViewModel(
    private val offerId: Long,
    private val getOfferDetailUseCase: GetOfferDetailUseCase,
    private val studentProfileUseCase: StudentProfileUseCase,
    private val submitApplicationUseCase: SubmitApplicationUseCase
) : ViewModel() {

    var uiState by mutableStateOf(ApplicationFormUiState())
        private set

    private val _events = MutableSharedFlow<ApplicationFormEvent>()
    val events: SharedFlow<ApplicationFormEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val offer = runCatching { getOfferDetailUseCase(offerId).first() }.getOrNull()
            val profile = runCatching { studentProfileUseCase.observeProfile().first() }.getOrNull()
            val current = uiState
            uiState = current.copy(
                isLoading = false,
                offer = offer,
                fullName = current.fullName.ifEmpty { profile?.fullName.orEmpty() },
                email = current.email.ifEmpty { profile?.email.orEmpty() },
                phone = current.phone.ifEmpty { profile?.phone.orEmpty() },
                cvUri = current.cvUri.ifEmpty { profile?.cvUri.orEmpty() },
                cvDisplayName = current.cvDisplayName.ifEmpty {
                    profile?.cvUri?.takeIf { it.isNotBlank() }?.substringAfterLast('/') ?: ""
                }
            )
        }
    }

    fun onFullNameChange(value: String) {
        uiState = uiState.copy(
            fullName = value,
            validation = uiState.validation.copy(nameError = null),
            submitError = null
        )
    }

    fun onEmailChange(value: String) {
        uiState = uiState.copy(
            email = value,
            validation = uiState.validation.copy(emailError = null),
            submitError = null
        )
    }

    fun onPhoneChange(value: String) {
        uiState = uiState.copy(
            phone = value,
            validation = uiState.validation.copy(phoneError = null),
            submitError = null
        )
    }

    fun onCoverLetterChange(value: String) {
        uiState = uiState.copy(
            coverLetter = value,
            validation = uiState.validation.copy(coverLetterError = null),
            submitError = null
        )
    }

    fun onCvSelected(uri: String, displayName: String) {
        uiState = uiState.copy(cvUri = uri, cvDisplayName = displayName)
    }

    fun submit(initialHistoryNote: String) {
        val current = uiState
        val validation = ApplicationValidator.validateApplicationForm(
            fullName = current.fullName,
            email = current.email,
            phone = current.phone,
            coverLetter = current.coverLetter
        )
        if (!validation.isValid) {
            uiState = current.copy(validation = validation)
            return
        }

        uiState = current.copy(isSubmitting = true, submitError = null)
        viewModelScope.launch {
            val result = submitApplicationUseCase(
                offerId = offerId,
                applicantName = current.fullName,
                applicantEmail = current.email,
                applicantPhone = current.phone,
                coverLetter = current.coverLetter,
                cvUri = current.cvUri,
                initialNote = initialHistoryNote
            )
            result.fold(
                onSuccess = { reference ->
                    uiState = uiState.copy(isSubmitting = false)
                    _events.emit(ApplicationFormEvent.Submitted(reference))
                },
                onFailure = { error ->
                    val mapped = when (error) {
                        is ApplicationSubmissionError.OfferClosed,
                        is ApplicationSubmissionError.DeadlinePassed -> SubmitFormError.CLOSED_OR_EXPIRED
                        is ApplicationSubmissionError.AlreadyApplied -> SubmitFormError.ALREADY_APPLIED
                        else -> SubmitFormError.GENERIC
                    }
                    uiState = uiState.copy(isSubmitting = false, submitError = mapped)
                }
            )
        }
    }
}
