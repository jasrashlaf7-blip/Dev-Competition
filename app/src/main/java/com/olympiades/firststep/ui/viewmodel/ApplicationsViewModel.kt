package com.olympiades.firststep.ui.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.usecase.UpdateApplicationStatusUseCase
import com.olympiades.firststep.domain.validation.ApplicationStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ApplicationsUiState {
    data object Loading : ApplicationsUiState
    data class Success(
        val applications: List<ApplicationWithDetails>,
        val selectedFilter: ApplicationStatus?,
        val expandedApplicationId: Long?
    ) : ApplicationsUiState
    data class Error(val message: String) : ApplicationsUiState
}

data class PendingFinalStatusChange(
    val applicationId: Long,
    val reference: String,
    val targetStatus: ApplicationStatus,
    val note: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationsViewModel(
    private val updateApplicationStatusUseCase: UpdateApplicationStatusUseCase
) : ViewModel() {

    private val statusFilterFlow = MutableStateFlow<ApplicationStatus?>(null)
    private val expandedAppIdFlow = MutableStateFlow<Long?>(null)

    val noteDrafts = mutableStateMapOf<Long, String>()

    private val _pendingConfirmation = MutableStateFlow<PendingFinalStatusChange?>(null)
    val pendingConfirmation: StateFlow<PendingFinalStatusChange?> = _pendingConfirmation.asStateFlow()

    private val _statusUpdatedEvents = MutableSharedFlow<Unit>()
    val statusUpdatedEvents: SharedFlow<Unit> = _statusUpdatedEvents.asSharedFlow()

    val uiState: StateFlow<ApplicationsUiState> = statusFilterFlow
        .flatMapLatest { filter ->
            combine(
                updateApplicationStatusUseCase.observeApplications(filter),
                expandedAppIdFlow
            ) { apps, expandedId ->
                ApplicationsUiState.Success(
                    applications = apps,
                    selectedFilter = filter,
                    expandedApplicationId = expandedId
                ) as ApplicationsUiState
            }
        }
        .catch { emit(ApplicationsUiState.Error(it.localizedMessage ?: "Database error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ApplicationsUiState.Loading
        )

    fun selectFilter(status: ApplicationStatus?) {
        statusFilterFlow.value = status
    }

    fun toggleExpanded(applicationId: Long) {
        expandedAppIdFlow.update { current ->
            if (current == applicationId) null else applicationId
        }
    }

    fun expandApplication(applicationId: Long) {
        expandedAppIdFlow.value = applicationId
    }

    fun updateNoteDraft(applicationId: Long, note: String) {
        noteDrafts[applicationId] = note
    }

    fun requestStatusTransition(
        application: ApplicationWithDetails,
        targetStatus: ApplicationStatus
    ) {
        if (!ApplicationStateMachine.canTransition(application.currentStatus, targetStatus)) {
            return
        }
        val note = noteDrafts[application.id].orEmpty()
        if (ApplicationStateMachine.requiresConfirmation(targetStatus)) {
            _pendingConfirmation.value = PendingFinalStatusChange(
                applicationId = application.id,
                reference = application.reference,
                targetStatus = targetStatus,
                note = note
            )
        } else {
            performStatusTransition(application.id, targetStatus, note)
        }
    }

    fun confirmPendingTransition() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        performStatusTransition(pending.applicationId, pending.targetStatus, pending.note)
    }

    fun dismissPendingTransition() {
        _pendingConfirmation.value = null
    }

    private fun performStatusTransition(
        applicationId: Long,
        targetStatus: ApplicationStatus,
        note: String
    ) {
        viewModelScope.launch {
            val result = updateApplicationStatusUseCase(
                applicationId = applicationId,
                newStatus = targetStatus,
                note = note
            )
            if (result.isSuccess) {
                noteDrafts.remove(applicationId)
                _statusUpdatedEvents.emit(Unit)
            }
        }
    }
}
