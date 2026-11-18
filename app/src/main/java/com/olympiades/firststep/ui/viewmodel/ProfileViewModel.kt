package com.olympiades.firststep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.domain.model.StudentProfile
import com.olympiades.firststep.domain.repository.FirstStepRepository
import com.olympiades.firststep.domain.usecase.StudentProfileUseCase
import com.olympiades.firststep.domain.validation.ApplicationValidator
import com.olympiades.firststep.domain.validation.ProfileFormValidationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: StudentProfile? = null,
    val language: AppLanguage = AppLanguage.FR,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val isEditing: Boolean = false,
    val fullNameInput: String = "",
    val emailInput: String = "",
    val phoneInput: String = "",
    val universityInput: String = "",
    val degreeLevelInput: String = "",
    val bioInput: String = "",
    val validation: ProfileFormValidationResult = ProfileFormValidationResult()
)

class ProfileViewModel(
    private val repository: FirstStepRepository,
    private val studentProfileUseCase: StudentProfileUseCase
) : ViewModel() {

    private data class EditDraft(
        val isEditing: Boolean = false,
        val fullName: String = "",
        val email: String = "",
        val phone: String = "",
        val university: String = "",
        val degreeLevel: String = "",
        val bio: String = "",
        val validation: ProfileFormValidationResult = ProfileFormValidationResult()
    )

    private val editDraft = MutableStateFlow(EditDraft())
    private val _savedEvents = MutableSharedFlow<Unit>()
    val savedEvents: SharedFlow<Unit> = _savedEvents.asSharedFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        studentProfileUseCase.observeProfile(),
        repository.observePreferences(),
        editDraft
    ) { profile, prefs, draft ->
        ProfileUiState(
            profile = profile,
            language = prefs.language,
            themeMode = prefs.themeMode,
            isEditing = draft.isEditing,
            fullNameInput = if (draft.isEditing) draft.fullName else profile?.fullName.orEmpty(),
            emailInput = if (draft.isEditing) draft.email else profile?.email.orEmpty(),
            phoneInput = if (draft.isEditing) draft.phone else profile?.phone.orEmpty(),
            universityInput = if (draft.isEditing) draft.university else profile?.university.orEmpty(),
            degreeLevelInput = if (draft.isEditing) draft.degreeLevel else profile?.degreeLevel.orEmpty(),
            bioInput = if (draft.isEditing) draft.bio else profile?.bio.orEmpty(),
            validation = draft.validation
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun startEditing() {
        val currentProfile = uiState.value.profile ?: return
        editDraft.value = EditDraft(
            isEditing = true,
            fullName = currentProfile.fullName,
            email = currentProfile.email,
            phone = currentProfile.phone,
            university = currentProfile.university,
            degreeLevel = currentProfile.degreeLevel,
            bio = currentProfile.bio
        )
    }

    fun cancelEditing() {
        editDraft.value = EditDraft(isEditing = false)
    }

    fun onFullNameChange(v: String) = editDraft.update {
        it.copy(fullName = v, validation = it.validation.copy(nameError = null))
    }

    fun onEmailChange(v: String) = editDraft.update {
        it.copy(email = v, validation = it.validation.copy(emailError = null))
    }

    fun onPhoneChange(v: String) = editDraft.update {
        it.copy(phone = v, validation = it.validation.copy(phoneError = null))
    }

    fun onUniversityChange(v: String) = editDraft.update {
        it.copy(university = v, validation = it.validation.copy(universityError = null))
    }

    fun onDegreeLevelChange(v: String) = editDraft.update {
        it.copy(degreeLevel = v, validation = it.validation.copy(degreeError = null))
    }

    fun onBioChange(v: String) = editDraft.update { it.copy(bio = v) }

    fun saveProfile() {
        val draft = editDraft.value
        val validation = ApplicationValidator.validateProfileForm(
            fullName = draft.fullName,
            email = draft.email,
            phone = draft.phone,
            university = draft.university,
            degreeLevel = draft.degreeLevel
        )
        if (!validation.isValid) {
            editDraft.update { it.copy(validation = validation) }
            return
        }

        val existing = uiState.value.profile
        val updated = StudentProfile(
            id = 1L,
            fullName = draft.fullName,
            email = draft.email,
            phone = draft.phone,
            university = draft.university,
            degreeLevel = draft.degreeLevel,
            bio = draft.bio,
            cvUri = existing?.cvUri.orEmpty(),
            updatedAt = existing?.updatedAt.orEmpty()
        )
        viewModelScope.launch {
            if (studentProfileUseCase.updateProfile(updated).isSuccess) {
                editDraft.value = EditDraft(isEditing = false)
                _savedEvents.emit(Unit)
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }
}
