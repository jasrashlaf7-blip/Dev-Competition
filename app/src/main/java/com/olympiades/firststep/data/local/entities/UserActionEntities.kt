package com.olympiades.firststep.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.olympiades.firststep.domain.model.ApplicationStatus

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = OfferEntity::class,
            parentColumns = ["id"],
            childColumns = ["offer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["offer_id"], unique = true)
    ]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "offer_id")
    val offerId: Long,
    @ColumnInfo(name = "added_at")
    val addedAt: String
)

@Entity(
    tableName = "applications",
    foreignKeys = [
        ForeignKey(
            entity = OfferEntity::class,
            parentColumns = ["id"],
            childColumns = ["offer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reference"], unique = true),
        Index(value = ["offer_id"], unique = true)
    ]
)
data class ApplicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val reference: String,
    @ColumnInfo(name = "offer_id")
    val offerId: Long,
    @ColumnInfo(name = "applicant_name")
    val applicantName: String,
    @ColumnInfo(name = "applicant_email")
    val applicantEmail: String,
    @ColumnInfo(name = "applicant_phone")
    val applicantPhone: String,
    @ColumnInfo(name = "cover_letter")
    val coverLetter: String = "",
    @ColumnInfo(name = "cv_uri")
    val cvUri: String = "",
    @ColumnInfo(name = "current_status")
    val currentStatus: ApplicationStatus = ApplicationStatus.SUBMITTED,
    @ColumnInfo(name = "applied_at")
    val appliedAt: String
)

@Entity(
    tableName = "status_history",
    foreignKeys = [
        ForeignKey(
            entity = ApplicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["application_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["application_id"])
    ]
)
data class StatusHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "application_id")
    val applicationId: Long,
    @ColumnInfo(name = "old_status")
    val oldStatus: ApplicationStatus?,
    @ColumnInfo(name = "new_status")
    val newStatus: ApplicationStatus,
    @ColumnInfo(name = "changed_at")
    val changedAt: String,
    val note: String = ""
)
