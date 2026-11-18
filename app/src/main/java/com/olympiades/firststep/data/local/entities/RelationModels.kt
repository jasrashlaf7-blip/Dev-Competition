package com.olympiades.firststep.data.local.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class OfferWithRelations(
    @Embedded val offer: OfferEntity,
    @Relation(
        parentColumn = "company_id",
        entityColumn = "id"
    )
    val company: CompanyEntity,
    @Relation(
        parentColumn = "domain_id",
        entityColumn = "id"
    )
    val domain: DomainEntity,
    @Relation(
        parentColumn = "location_id",
        entityColumn = "id"
    )
    val location: LocationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = OfferSkillCrossRef::class,
            parentColumn = "offer_id",
            entityColumn = "skill_id"
        )
    )
    val skills: List<SkillEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "offer_id"
    )
    val favorite: FavoriteEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "offer_id"
    )
    val application: ApplicationEntity?
)

data class FavoriteWithOffer(
    @Embedded val favorite: FavoriteEntity,
    @Relation(
        entity = OfferEntity::class,
        parentColumn = "offer_id",
        entityColumn = "id"
    )
    val offerWithRelations: OfferWithRelations
)

data class ApplicationWithRelations(
    @Embedded val application: ApplicationEntity,
    @Relation(
        entity = OfferEntity::class,
        parentColumn = "offer_id",
        entityColumn = "id"
    )
    val offerWithRelations: OfferWithRelations,
    @Relation(
        parentColumn = "id",
        entityColumn = "application_id"
    )
    val history: List<StatusHistoryEntity>
)
