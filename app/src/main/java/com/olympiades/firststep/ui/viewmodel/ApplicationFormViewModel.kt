package com.olympiades.firststep.ui.viewmodel

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(ApplicationFormUiState())
    val uiState: StateFlow<ApplicationFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ApplicationFormEvent>()
    val events: SharedFlow<ApplicationFormEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val offer = runCatching { getOfferDetailUseCase(offerId).first() }.getOrNull()
            val profile = runCatching { studentProfileUseCase.observeProfile().first() }.getOrNull()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    offer = offer,
                    fullName = state.fullName.ifEmpty { profile?.fullName.orEmpty() },
                    email = state.email.ifEmpty { profile?.email.orEmpty() },
                    phone = state.phone.ifEmpty { profile?.phone.orEmpty() },
                    cvUri = state.cvUri.ifEmpty { profile?.cvUri.orEmpty() },
                    cvDisplayName = state.cvDisplayName.ifEmpty {
                        profile?.cvUri?.takeIf { it.isNotBlank() }?.substringAfterLast('/') ?: ""
                    }
                )
            }
        }
    }

    fun onFullNameChange(value: String) {
        _uiState.update {
            it.copy(
                fullName = value,
                validation = it.validation.copy(nameError = null),
                submitError = null
            )
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                validation = it.validation.copy(emailError = null),
                submitError = null
            )
        }
    }

    fun onPhoneChange(value: String) {
        _uiState.update {
            it.copy(
                phone = value,
                validation = it.validation.copy(phoneError = null),
                submitError = null
            )
        }
    }

    fun onCoverLetterChange(value: String) {
        _uiState.update {
            it.copy(
                coverLetter = value,
                validation = it.validation.copy(coverLetterError = null),
                submitError = null
            )
        }
    }

    fun onCvSelected(uri: String, displayName: String) {
        _uiState.update {
            it.copy(cvUri = uri, cvDisplayName = displayName)
        }
    }

    fun submit(initialHistoryNote: String) {
        val current = _uiState.value
        val validation = ApplicationValidator.validateApplicationForm(
            fullName = current.fullName,
            email = current.email,
            phone = current.phone,
            coverLetter = current.coverLetter
        )
        if (!validation.isValid) {
            _uiState.update { it.copy(validation = validation) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, submitError = null) }
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
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.emit(ApplicationFormEvent.Submitted(reference))
                },
                onFailure = { error ->
                    val mapped = when (error) {
                        is ApplicationSubmissionError.OfferClosed,
                        is ApplicationSubmissionError.DeadlinePassed -> SubmitFormError.CLOSED_OR_EXPIRED
                        is ApplicationSubmissionError.AlreadyApplied -> SubmitFormError.ALREADY_APPLIED
                        else -> SubmitFormError.GENERIC
                    }
                    _uiState.update { it.copy(isSubmitting = false, submitError = mapped) }
                }
            )
        }
    }
}
