package com.olympiades.firststep.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.olympiades.firststep.domain.model.OfferStatus

@Entity(
    tableName = "offers",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["company_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domain_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["domain_id"]),
        Index(value = ["location_id"]),
        Index(value = ["deadline"]),
        Index(value = ["status"])
    ]
)
data class OfferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "company_id")
    val companyId: Long,
    @ColumnInfo(name = "domain_id")
    val domainId: Long,
    @ColumnInfo(name = "location_id")
    val locationId: Long,
    @ColumnInfo(name = "title_fr")
    val titleFr: String,
    @ColumnInfo(name = "title_ar")
    val titleAr: String,
    @ColumnInfo(name = "description_fr")
    val descriptionFr: String,
    @ColumnInfo(name = "description_ar")
    val descriptionAr: String,
    @ColumnInfo(name = "duration_months")
    val durationMonths: Int,
    @ColumnInfo(name = "is_paid")
    val isPaid: Boolean = false,
    @ColumnInfo(name = "monthly_stipend_dzd")
    val monthlyStipendDzd: Int = 0,
    @ColumnInfo(name = "start_date")
    val startDate: String = "",
    val status: OfferStatus = OfferStatus.OPEN,
    val deadline: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)

@Entity(
    tableName = "offer_skills",
    primaryKeys = ["offer_id", "skill_id"],
    foreignKeys = [
        ForeignKey(
            entity = OfferEntity::class,
            parentColumns = ["id"],
            childColumns = ["offer_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SkillEntity::class,
            parentColumns = ["id"],
            childColumns = ["skill_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["skill_id"])
    ]
)
data class OfferSkillCrossRef(
    @ColumnInfo(name = "offer_id")
    val offerId: Long,
    @ColumnInfo(name = "skill_id")
    val skillId: Long
)
