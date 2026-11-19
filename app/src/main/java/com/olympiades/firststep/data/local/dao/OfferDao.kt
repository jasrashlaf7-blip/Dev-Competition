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
        INNER JOIN domains d ON o.domain_id = d.id
        INNER JOIN locations l ON o.location_id = l.id
        WHERE (:domainId IS NULL OR o.domain_id = :domainId)
          AND (:locationId IS NULL OR o.location_id = :locationId)
          AND (:paidOnly = 0 OR o.is_paid = 1)
          AND (:minStipendDzd <= 0 OR o.monthly_stipend_dzd >= :minStipendDzd)
          AND (:openOnly = 0 OR (o.status = 'OPEN' AND o.deadline >= :todayIso))
          AND (
            :query = ''
            OR o.title_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR o.title_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR o.description_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR o.description_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR c.name LIKE '%' || :query || '%' ESCAPE '\'
            OR d.name_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR d.name_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR l.city_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR l.city_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR EXISTS (
                SELECT 1 FROM offer_skills os
                INNER JOIN skills s ON os.skill_id = s.id
                WHERE os.offer_id = o.id
                  AND (s.name_fr LIKE '%' || :query || '%' ESCAPE '\' OR s.name_ar LIKE '%' || :query || '%' ESCAPE '\')
            )
          )
        ORDER BY o.created_at DESC, o.id DESC
        """
    )
    fun getFilteredOffersOrderByNewest(
        query: String,
        domainId: Long?,
        locationId: Long?,
        paidOnly: Boolean,
        minStipendDzd: Int,
        openOnly: Boolean,
        todayIso: String
    ): Flow<List<OfferWithRelations>>

    @Transaction
    @Query(
        """
        SELECT o.* FROM offers o
        INNER JOIN companies c ON o.company_id = c.id
        INNER JOIN domains d ON o.domain_id = d.id
        INNER JOIN locations l ON o.location_id = l.id
        WHERE (:domainId IS NULL OR o.domain_id = :domainId)
          AND (:locationId IS NULL OR o.location_id = :locationId)
          AND (:paidOnly = 0 OR o.is_paid = 1)
          AND (:minStipendDzd <= 0 OR o.monthly_stipend_dzd >= :minStipendDzd)
          AND (:openOnly = 0 OR (o.status = 'OPEN' AND o.deadline >= :todayIso))
          AND (
            :query = ''
            OR o.title_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR o.title_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR o.description_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR o.description_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR c.name LIKE '%' || :query || '%' ESCAPE '\'
            OR d.name_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR d.name_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR l.city_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR l.city_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR EXISTS (
                SELECT 1 FROM offer_skills os
                INNER JOIN skills s ON os.skill_id = s.id
                WHERE os.offer_id = o.id
                  AND (s.name_fr LIKE '%' || :query || '%' ESCAPE '\' OR s.name_ar LIKE '%' || :query || '%' ESCAPE '\')
            )
          )
        ORDER BY o.deadline ASC, o.id ASC
        """
    )
    fun getFilteredOffersOrderByDeadline(
        query: String,
        domainId: Long?,
        locationId: Long?,
        paidOnly: Boolean,
        minStipendDzd: Int,
        openOnly: Boolean,
        todayIso: String
    ): Flow<List<OfferWithRelations>>

    @Transaction
    @Query(
        """
        SELECT o.* FROM offers o
        INNER JOIN companies c ON o.company_id = c.id
        INNER JOIN domains d ON o.domain_id = d.id
        INNER JOIN locations l ON o.location_id = l.id
        WHERE (:domainId IS NULL OR o.domain_id = :domainId)
          AND (:locationId IS NULL OR o.location_id = :locationId)
          AND (:paidOnly = 0 OR o.is_paid = 1)
          AND (:minStipendDzd <= 0 OR o.monthly_stipend_dzd >= :minStipendDzd)
          AND (:openOnly = 0 OR (o.status = 'OPEN' AND o.deadline >= :todayIso))
          AND (
            :query = ''
            OR o.title_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR o.title_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR o.description_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR o.description_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR c.name LIKE '%' || :query || '%' ESCAPE '\'
            OR d.name_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR d.name_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR l.city_fr LIKE '%' || :query || '%' ESCAPE '\'
            OR l.city_ar LIKE '%' || :query || '%' ESCAPE '\'
            OR EXISTS (
                SELECT 1 FROM offer_skills os
                INNER JOIN skills s ON os.skill_id = s.id
                WHERE os.offer_id = o.id
                  AND (s.name_fr LIKE '%' || :query || '%' ESCAPE '\' OR s.name_ar LIKE '%' || :query || '%' ESCAPE '\')
            )
          )
        ORDER BY o.monthly_stipend_dzd DESC, o.id DESC
        """
    )
    fun getFilteredOffersOrderByStipend(
        query: String,
        domainId: Long?,
        locationId: Long?,
        paidOnly: Boolean,
        minStipendDzd: Int,
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

    @Query("SELECT * FROM companies ORDER BY id ASC")
    fun observeAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT COUNT(*) FROM offers")
    suspend fun getOfferCount(): Int

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationCount(): Int

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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOffer(offer: OfferEntity): Long

    @Query(
        """
        UPDATE offers
        SET company_id = :companyId,
            domain_id = :domainId,
            location_id = :locationId,
            title_fr = :titleFr,
            title_ar = :titleAr,
            description_fr = :descriptionFr,
            description_ar = :descriptionAr,
            duration_months = :durationMonths,
            is_paid = :isPaid,
            monthly_stipend_dzd = :monthlyStipendDzd,
            start_date = :startDate,
            status = :status,
            deadline = :deadline
        WHERE id = :offerId
        """
    )
    suspend fun updateOffer(
        offerId: Long,
        companyId: Long,
        domainId: Long,
        locationId: Long,
        titleFr: String,
        titleAr: String,
        descriptionFr: String,
        descriptionAr: String,
        durationMonths: Int,
        isPaid: Boolean,
        monthlyStipendDzd: Int,
        startDate: String,
        status: String,
        deadline: String
    )

    @Query("DELETE FROM offers WHERE id = :offerId")
    suspend fun deleteOfferById(offerId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOfferSkills(crossRefs: List<OfferSkillCrossRef>)
}
