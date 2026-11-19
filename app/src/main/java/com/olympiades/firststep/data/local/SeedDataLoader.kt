package com.olympiades.firststep.data.local

import android.content.Context
import androidx.room.withTransaction
import com.olympiades.firststep.data.local.entities.CompanyEntity
import com.olympiades.firststep.data.local.entities.DomainEntity
import com.olympiades.firststep.data.local.entities.LocationEntity
import com.olympiades.firststep.data.local.entities.OfferEntity
import com.olympiades.firststep.data.local.entities.OfferSkillCrossRef
import com.olympiades.firststep.data.local.entities.SkillEntity
import com.olympiades.firststep.data.local.entities.StudentProfileEntity
import com.olympiades.firststep.domain.model.OfferStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SeedDataLoader(
    private val context: Context,
    private val database: FirstStepDatabase
) {
    private val seedMutex = Mutex()

    suspend fun ensureSeeded() = seedMutex.withLock {
        withContext(Dispatchers.IO) {
            val offerDao = database.offerDao()
            val profileDao = database.studentProfileDao()

            val needsOffers = offerDao.getOfferCount() == 0
            val needsProfile = profileDao.getProfileOnce() == null

            if (!needsOffers && !needsProfile) {
                return@withContext
            }

            val jsonText = if (needsOffers) {
                context.assets.open("firststep_seed.json").bufferedReader().use { it.readText() }
            } else {
                ""
            }

            database.withTransaction {
                if (needsProfile && profileDao.getProfileOnce() == null) {
                    profileDao.upsertProfile(defaultStudentProfile())
                }

                if (needsOffers && offerDao.getOfferCount() == 0) {
                    val root = JSONObject(jsonText)

                    val domains = mutableListOf<DomainEntity>()
                    val domainsJson = root.getJSONArray("domains")
                    for (i in 0 until domainsJson.length()) {
                        val obj = domainsJson.getJSONObject(i)
                        domains.add(
                            DomainEntity(
                                id = obj.getLong("id"),
                                nameFr = obj.getString("name_fr"),
                                nameAr = obj.getString("name_ar")
                            )
                        )
                    }

                    val locations = mutableListOf<LocationEntity>()
                    val locationsJson = root.getJSONArray("locations")
                    for (i in 0 until locationsJson.length()) {
                        val obj = locationsJson.getJSONObject(i)
                        locations.add(
                            LocationEntity(
                                id = obj.getLong("id"),
                                cityFr = obj.getString("city_fr"),
                                cityAr = obj.getString("city_ar"),
                                regionFr = obj.optString("region_fr", ""),
                                regionAr = obj.optString("region_ar", "")
                            )
                        )
                    }

                    val companies = mutableListOf<CompanyEntity>()
                    val companiesJson = root.getJSONArray("companies")
                    for (i in 0 until companiesJson.length()) {
                        val obj = companiesJson.getJSONObject(i)
                        companies.add(
                            CompanyEntity(
                                id = obj.getLong("id"),
                                name = obj.getString("name"),
                                sectorFr = obj.optString("sector_fr", ""),
                                sectorAr = obj.optString("sector_ar", ""),
                                descriptionFr = obj.optString("description_fr", ""),
                                descriptionAr = obj.optString("description_ar", ""),
                                website = obj.optString("website", ""),
                                logoUrl = obj.optString("logo_url", "")
                            )
                        )
                    }

                    val skills = mutableListOf<SkillEntity>()
                    val skillsJson = root.getJSONArray("skills")
                    for (i in 0 until skillsJson.length()) {
                        val obj = skillsJson.getJSONObject(i)
                        skills.add(
                            SkillEntity(
                                id = obj.getLong("id"),
                                nameFr = obj.getString("name_fr"),
                                nameAr = obj.getString("name_ar")
                            )
                        )
                    }

                    val offers = mutableListOf<OfferEntity>()
                    val offerSkills = mutableListOf<OfferSkillCrossRef>()
                    val offersJson = root.getJSONArray("offers")
                    for (i in 0 until offersJson.length()) {
                        val obj = offersJson.getJSONObject(i)
                        val offerId = obj.getLong("id")
                        val isPaid = obj.getBoolean("is_paid")
                        val defaultStipend = if (isPaid) 20000 + ((offerId.toInt() % 5) * 5000) else 0
                        val defaultStartDate = "2026-12-%02d".format(1 + (offerId.toInt() % 15))
                        offers.add(
                            OfferEntity(
                                id = offerId,
                                companyId = obj.getLong("company_id"),
                                domainId = obj.getLong("domain_id"),
                                locationId = obj.getLong("location_id"),
                                titleFr = obj.getString("title_fr"),
                                titleAr = obj.getString("title_ar"),
                                descriptionFr = obj.getString("description_fr"),
                                descriptionAr = obj.getString("description_ar"),
                                durationMonths = obj.getInt("duration_months"),
                                isPaid = isPaid,
                                monthlyStipendDzd = obj.optInt("monthly_stipend_dzd", defaultStipend),
                                startDate = obj.optString("start_date", defaultStartDate),
                                status = OfferStatus.valueOf(obj.getString("status")),
                                deadline = obj.getString("deadline"),
                                createdAt = obj.getString("created_at")
                            )
                        )
                        val skillIdsJson = obj.getJSONArray("skill_ids")
                        for (j in 0 until skillIdsJson.length()) {
                            offerSkills.add(
                                OfferSkillCrossRef(
                                    offerId = offerId,
                                    skillId = skillIdsJson.getLong(j)
                                )
                            )
                        }
                    }

                    offerDao.insertDomains(domains)
                    offerDao.insertLocations(locations)
                    offerDao.insertCompanies(companies)
                    offerDao.insertSkills(skills)
                    offerDao.insertOffers(offers)
                    offerDao.insertOfferSkills(offerSkills)
                }
            }
        }
    }

    companion object {
        fun defaultStudentProfile(): StudentProfileEntity = StudentProfileEntity(
            id = 1L,
            fullName = "Amina Saidi",
            email = "amina.saidi@email.dz",
            phone = "+213 555 12 34 56",
            university = "Univ. Oran 1",
            degreeLevel = "L3 Informatique",
            bio = "Étudiante en L3 Informatique passionnée par le développement mobile Android et l'ingénierie logicielle.",
            cvUri = "",
            updatedAt = "2026-11-18 09:00:00"
        )
    }
}
