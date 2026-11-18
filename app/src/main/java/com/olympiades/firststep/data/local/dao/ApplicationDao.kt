package com.olympiades.firststep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.olympiades.firststep.data.local.entities.ApplicationEntity
import com.olympiades.firststep.data.local.entities.ApplicationWithRelations
import com.olympiades.firststep.data.local.entities.StatusHistoryEntity
import com.olympiades.firststep.domain.model.ApplicationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {

    @Transaction
    @Query(
        """
        SELECT * FROM applications
        WHERE (:statusFilter IS NULL OR current_status = :statusFilter)
        ORDER BY applied_at DESC, id DESC
        """
    )
    fun observeApplications(statusFilter: ApplicationStatus?): Flow<List<ApplicationWithRelations>>

    @Transaction
    @Query("SELECT * FROM applications WHERE id = :applicationId")
    suspend fun getApplicationById(applicationId: Long): ApplicationWithRelations?

    @Query("SELECT * FROM applications WHERE offer_id = :offerId LIMIT 1")
    suspend fun getApplicationByOfferId(offerId: Long): ApplicationEntity?

    @Query("SELECT COALESCE(MAX(id), 0) FROM applications")
    suspend fun getMaxApplicationId(): Long

    @Query("SELECT EXISTS(SELECT 1 FROM applications WHERE reference = :reference)")
    suspend fun referenceExists(reference: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertApplication(application: ApplicationEntity): Long

    @Query("UPDATE applications SET current_status = :newStatus WHERE id = :applicationId")
    suspend fun updateApplicationStatus(applicationId: Long, newStatus: ApplicationStatus)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStatusHistory(history: StatusHistoryEntity): Long

    @Transaction
    suspend fun submitApplicationWithHistory(
        application: ApplicationEntity,
        initialNote: String
    ): Long {
        val newId = insertApplication(application)
        insertStatusHistory(
            StatusHistoryEntity(
                applicationId = newId,
                oldStatus = null,
                newStatus = ApplicationStatus.SUBMITTED,
                changedAt = application.appliedAt,
                note = initialNote
            )
        )
        return newId
    }

    @Query(
        """
        UPDATE applications
        SET applicant_name = :applicantName,
            applicant_email = :applicantEmail,
            applicant_phone = :applicantPhone,
            cover_letter = :coverLetter,
            cv_uri = :cvUri
        WHERE id = :applicationId
        """
    )
    suspend fun updateApplicationDetails(
        applicationId: Long,
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        coverLetter: String,
        cvUri: String
    )

    @Transaction
    suspend fun changeStatusWithHistory(
        applicationId: Long,
        oldStatus: ApplicationStatus,
        newStatus: ApplicationStatus,
        changedAt: String,
        note: String
    ) {
        updateApplicationStatus(applicationId, newStatus)
        insertStatusHistory(
            StatusHistoryEntity(
                applicationId = applicationId,
                oldStatus = oldStatus,
                newStatus = newStatus,
                changedAt = changedAt,
                note = note
            )
        )
    }

    @Transaction
    suspend fun updateApplicationDetailsWithHistory(
        applicationId: Long,
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        coverLetter: String,
        cvUri: String,
        currentStatus: ApplicationStatus,
        changedAt: String,
        note: String
    ) {
        updateApplicationDetails(
            applicationId = applicationId,
            applicantName = applicantName,
            applicantEmail = applicantEmail,
            applicantPhone = applicantPhone,
            coverLetter = coverLetter,
            cvUri = cvUri
        )
        insertStatusHistory(
            StatusHistoryEntity(
                applicationId = applicationId,
                oldStatus = currentStatus,
                newStatus = currentStatus,
                changedAt = changedAt,
                note = note
            )
        )
    }
}
