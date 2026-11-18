package com.olympiades.firststep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.olympiades.firststep.data.local.entities.CompanyEntity
import com.olympiades.firststep.data.local.entities.DomainEntity
import com.olympiades.firststep.data.local.entities.LocationEntity
import com.olympiades.firststep.data.local.entities.OfferEntity
import com.olympiades.firststep.data.local.entities.OfferSkillCrossRef
import com.olympiades.firststep.data.local.entities.OfferWithRelations
import com.olympiades.firststep.data.local.entities.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {

    @Transaction
    @Query(
        """
        SELECT o.* FROM offers o
        INNER JOIN companies c ON o.company_id = c.id
        WHERE (:domainId IS NULL OR o.domain_id = :domainId)
          AND (:locationId IS NULL OR o.location_id = :locationId)
          AND (:paidOnly = 0 OR o.is_paid = 1)
          AND (:openOnly = 0 OR (o.status = 'OPEN' AND o.deadline >= :todayIso))
          AND (
            :query = ''
            OR o.title_fr LIKE '%' || :query || '%'
            OR o.title_ar LIKE '%' || :query || '%'
            OR o.description_fr LIKE '%' || :query || '%'
            OR o.description_ar LIKE '%' || :query || '%'
            OR c.name LIKE '%' || :query || '%'
          )
        ORDER BY o.created_at DESC, o.id DESC
        """
    )
    fun getFilteredOffersOrderByNewest(
        query: String,
        domainId: Long?,
        locationId: Long?,
        paidOnly: Boolean,
        openOnly: Boolean,
        todayIso: String
    ): Flow<List<OfferWithRelations>>

    @Transaction
    @Query(
        """
        SELECT o.* FROM offers o
        INNER JOIN companies c ON o.company_id = c.id
        WHERE (:domainId IS NULL OR o.domain_id = :domainId)
          AND (:locationId IS NULL OR o.location_id = :locationId)
          AND (:paidOnly = 0 OR o.is_paid = 1)
          AND (:openOnly = 0 OR (o.status = 'OPEN' AND o.deadline >= :todayIso))
          AND (
            :query = ''
            OR o.title_fr LIKE '%' || :query || '%'
            OR o.title_ar LIKE '%' || :query || '%'
            OR o.description_fr LIKE '%' || :query || '%'
            OR o.description_ar LIKE '%' || :query || '%'
            OR c.name LIKE '%' || :query || '%'
          )
        ORDER BY o.deadline ASC, o.id ASC
        """
    )
    fun getFilteredOffersOrderByDeadline(
        query: String,
        domainId: Long?,
        locationId: Long?,
        paidOnly: Boolean,
        openOnly: Boolean,
        todayIso: String
    ): Flow<List<OfferWithRelations>>

    @Transaction
    @Query("SELECT * FROM offers WHERE id = :offerId")
    fun observeOfferById(offerId: Long): Flow<OfferWithRelations?>

    @Transaction
    @Query("SELECT * FROM offers WHERE id = :offerId")
    suspend fun getOfferByIdOnce(offerId: Long): OfferWithRelations?

    @Query("SELECT * FROM domains ORDER BY id ASC")
    fun observeAllDomains(): Flow<List<DomainEntity>>

    @Query("SELECT * FROM locations ORDER BY id ASC")
    fun observeAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT COUNT(*) FROM offers")
    suspend fun getOfferCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDomains(domains: List<DomainEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompanies(companies: List<CompanyEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkills(skills: List<SkillEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOffers(offers: List<OfferEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOfferSkills(crossRefs: List<OfferSkillCrossRef>)
}
