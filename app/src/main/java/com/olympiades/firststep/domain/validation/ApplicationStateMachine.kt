package com.olympiades.firststep.domain.validation

import com.olympiades.firststep.domain.model.ApplicationStatus

object ApplicationStateMachine {

    fun allowedNextStatuses(current: ApplicationStatus): List<ApplicationStatus> {
        return when (current) {
            ApplicationStatus.SUBMITTED -> listOf(
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.INTERVIEW_SCHEDULED,
                ApplicationStatus.CANCELLED
            )
            ApplicationStatus.UNDER_REVIEW -> listOf(
                ApplicationStatus.INTERVIEW_SCHEDULED,
                ApplicationStatus.ACCEPTED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.CANCELLED
            )
            ApplicationStatus.INTERVIEW_SCHEDULED -> listOf(
                ApplicationStatus.ACCEPTED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.CANCELLED
            )
            ApplicationStatus.ACCEPTED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.CANCELLED -> emptyList()
        }
    }

    fun canTransition(from: ApplicationStatus, to: ApplicationStatus): Boolean {
        if (from.isFinal) return false
        return to in allowedNextStatuses(from)
    }

    fun requiresConfirmation(target: ApplicationStatus): Boolean = target.isFinal
}
