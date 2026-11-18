package com.olympiades.firststep.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileEditDraft(
    val isEditing: Boolean = false,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val university: String = "",
    val degreeLevel: String = "",
    val bio: String = "",
    val validation: ProfileFormValidationResult = ProfileFormValidationResult()
)

data class ProfileUiState(
    val profile: StudentProfile? = null,
    val language: AppLanguage = AppLanguage.FR,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT
)

class ProfileViewModel(
    private val repository: FirstStepRepository,
    private val studentProfileUseCase: StudentProfileUseCase
) : ViewModel() {

    var editDraft by mutableStateOf(ProfileEditDraft())
        private set

    private val _savedEvents = MutableSharedFlow<Unit>()
    val savedEvents: SharedFlow<Unit> = _savedEvents.asSharedFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        studentProfileUseCase.observeProfile(),
        repository.observePreferences()
    ) { profile, prefs ->
        ProfileUiState(
            profile = profile,
            language = prefs.language,
            themeMode = prefs.themeMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun startEditing() {
        val currentProfile = uiState.value.profile ?: return
        editDraft = ProfileEditDraft(
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
        editDraft = ProfileEditDraft(isEditing = false)
    }

    fun onFullNameChange(v: String) {
        editDraft = editDraft.copy(fullName = v, validation = editDraft.validation.copy(nameError = null))
    }

    fun onEmailChange(v: String) {
        editDraft = editDraft.copy(email = v, validation = editDraft.validation.copy(emailError = null))
    }

    fun onPhoneChange(v: String) {
        editDraft = editDraft.copy(phone = v, validation = editDraft.validation.copy(phoneError = null))
    }

    fun onUniversityChange(v: String) {
        editDraft = editDraft.copy(university = v, validation = editDraft.validation.copy(universityError = null))
    }

    fun onDegreeLevelChange(v: String) {
        editDraft = editDraft.copy(degreeLevel = v, validation = editDraft.validation.copy(degreeError = null))
    }

    fun onBioChange(v: String) {
        editDraft = editDraft.copy(bio = v)
    }

    fun saveProfile() {
        val draft = editDraft
        val validation = ApplicationValidator.validateProfileForm(
            fullName = draft.fullName,
            email = draft.email,
            phone = draft.phone,
            university = draft.university,
            degreeLevel = draft.degreeLevel
        )
        if (!validation.isValid) {
            editDraft = draft.copy(validation = validation)
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
                editDraft = ProfileEditDraft(isEditing = false)
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
