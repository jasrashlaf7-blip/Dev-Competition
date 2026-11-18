package com.olympiades.firststep.data.repository

import com.olympiades.firststep.data.local.entities.ApplicationWithRelations
import com.olympiades.firststep.data.local.entities.CompanyEntity
import com.olympiades.firststep.data.local.entities.DomainEntity
import com.olympiades.firststep.data.local.entities.FavoriteWithOffer
import com.olympiades.firststep.data.local.entities.LocationEntity
import com.olympiades.firststep.data.local.entities.OfferWithRelations
import com.olympiades.firststep.data.local.entities.SkillEntity
import com.olympiades.firststep.data.local.entities.StatusHistoryEntity
import com.olympiades.firststep.data.local.entities.StudentProfileEntity
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.Company
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.Skill
import com.olympiades.firststep.domain.model.StatusHistoryItem
import com.olympiades.firststep.domain.model.StudentProfile

fun DomainEntity.toDomain(): Domain = Domain(
    id = id,
    nameFr = nameFr,
    nameAr = nameAr
)

fun LocationEntity.toDomain(): Location = Location(
    id = id,
    cityFr = cityFr,
    cityAr = cityAr,
    regionFr = regionFr,
    regionAr = regionAr
)

fun CompanyEntity.toDomain(): Company = Company(
    id = id,
    name = name,
    sectorFr = sectorFr,
    sectorAr = sectorAr,
    descriptionFr = descriptionFr,
    descriptionAr = descriptionAr,
    website = website,
    logoUrl = logoUrl
)

fun SkillEntity.toDomain(): Skill = Skill(
    id = id,
    nameFr = nameFr,
    nameAr = nameAr
)

fun StudentProfileEntity.toDomain(): StudentProfile = StudentProfile(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone,
    university = university,
    degreeLevel = degreeLevel,
    bio = bio,
    cvUri = cvUri,
    updatedAt = updatedAt
)

fun StudentProfile.toEntity(updatedAt: String): StudentProfileEntity = StudentProfileEntity(
    id = 1L,
    fullName = fullName.trim(),
    email = email.trim(),
    phone = phone.trim(),
    university = university.trim(),
    degreeLevel = degreeLevel.trim(),
    bio = bio.trim(),
    cvUri = cvUri,
    updatedAt = updatedAt
)

fun OfferWithRelations.toDomain(): Offer = Offer(
    id = offer.id,
    company = company.toDomain(),
    domain = domain.toDomain(),
    location = location.toDomain(),
    titleFr = offer.titleFr,
    titleAr = offer.titleAr,
    descriptionFr = offer.descriptionFr,
    descriptionAr = offer.descriptionAr,
    durationMonths = offer.durationMonths,
    isPaid = offer.isPaid,
    status = offer.status,
    deadline = offer.deadline,
    createdAt = offer.createdAt,
    skills = skills.map { it.toDomain() },
    isFavorite = favorite != null,
    applicationStatus = application?.currentStatus,
    applicationReference = application?.reference
)

fun FavoriteWithOffer.toDomain(): FavoriteOffer = FavoriteOffer(
    favoriteId = favorite.id,
    addedAt = favorite.addedAt,
    offer = offerWithRelations.toDomain()
)

fun StatusHistoryEntity.toDomain(): StatusHistoryItem = StatusHistoryItem(
    id = id,
    applicationId = applicationId,
    oldStatus = oldStatus,
    newStatus = newStatus,
    changedAt = changedAt,
    note = note
)

fun ApplicationWithRelations.toDomain(): ApplicationWithDetails = ApplicationWithDetails(
    id = application.id,
    reference = application.reference,
    offer = offerWithRelations.toDomain(),
    applicantName = application.applicantName,
    applicantEmail = application.applicantEmail,
    applicantPhone = application.applicantPhone,
    coverLetter = application.coverLetter,
    cvUri = application.cvUri,
    currentStatus = application.currentStatus,
    appliedAt = application.appliedAt,
    history = history.sortedWith(compareBy({ it.changedAt }, { it.id })).map { it.toDomain() }
)
