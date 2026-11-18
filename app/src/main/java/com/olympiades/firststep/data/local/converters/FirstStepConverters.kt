package com.olympiades.firststep.data.local.converters

import androidx.room.TypeConverter
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.OfferStatus

class FirstStepConverters {

    @TypeConverter
    fun fromOfferStatus(status: OfferStatus?): String? = status?.name

    @TypeConverter
    fun toOfferStatus(value: String?): OfferStatus? =
        value?.let { runCatching { OfferStatus.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromApplicationStatus(status: ApplicationStatus?): String? = status?.name

    @TypeConverter
    fun toApplicationStatus(value: String?): ApplicationStatus? =
        value?.let { runCatching { ApplicationStatus.valueOf(it) }.getOrNull() }
}
