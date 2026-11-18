package com.olympiades.firststep.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "domains",
    indices = [
        Index(value = ["name_fr"], unique = true),
        Index(value = ["name_ar"], unique = true)
    ]
)
data class DomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name_fr")
    val nameFr: String,
    @ColumnInfo(name = "name_ar")
    val nameAr: String
)

@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["city_fr", "region_fr"], unique = true)
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "city_fr")
    val cityFr: String,
    @ColumnInfo(name = "city_ar")
    val cityAr: String,
    @ColumnInfo(name = "region_fr")
    val regionFr: String = "",
    @ColumnInfo(name = "region_ar")
    val regionAr: String = ""
)

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "sector_fr")
    val sectorFr: String = "",
    @ColumnInfo(name = "sector_ar")
    val sectorAr: String = "",
    @ColumnInfo(name = "description_fr")
    val descriptionFr: String = "",
    @ColumnInfo(name = "description_ar")
    val descriptionAr: String = "",
    val website: String = "",
    @ColumnInfo(name = "logo_url")
    val logoUrl: String = ""
)

@Entity(
    tableName = "skills",
    indices = [
        Index(value = ["name_fr"], unique = true),
        Index(value = ["name_ar"], unique = true)
    ]
)
data class SkillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name_fr")
    val nameFr: String,
    @ColumnInfo(name = "name_ar")
    val nameAr: String
)

@Entity(tableName = "student_profiles")
data class StudentProfileEntity(
    @PrimaryKey
    val id: Long = 1L,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    val email: String,
    val phone: String = "",
    val university: String = "",
    @ColumnInfo(name = "degree_level")
    val degreeLevel: String = "",
    val bio: String = "",
    @ColumnInfo(name = "cv_uri")
    val cvUri: String = "",
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
