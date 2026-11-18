package com.olympiades.firststep.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.olympiades.firststep.data.local.converters.FirstStepConverters
import com.olympiades.firststep.data.local.dao.ApplicationDao
import com.olympiades.firststep.data.local.dao.FavoriteDao
import com.olympiades.firststep.data.local.dao.OfferDao
import com.olympiades.firststep.data.local.dao.StudentProfileDao
import com.olympiades.firststep.data.local.entities.ApplicationEntity
import com.olympiades.firststep.data.local.entities.CompanyEntity
import com.olympiades.firststep.data.local.entities.DomainEntity
import com.olympiades.firststep.data.local.entities.FavoriteEntity
import com.olympiades.firststep.data.local.entities.LocationEntity
import com.olympiades.firststep.data.local.entities.OfferEntity
import com.olympiades.firststep.data.local.entities.OfferSkillCrossRef
import com.olympiades.firststep.data.local.entities.SkillEntity
import com.olympiades.firststep.data.local.entities.StatusHistoryEntity
import com.olympiades.firststep.data.local.entities.StudentProfileEntity
import java.util.concurrent.Executors

@Database(
    entities = [
        DomainEntity::class,
        LocationEntity::class,
        CompanyEntity::class,
        SkillEntity::class,
        StudentProfileEntity::class,
        OfferEntity::class,
        OfferSkillCrossRef::class,
        FavoriteEntity::class,
        ApplicationEntity::class,
        StatusHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(FirstStepConverters::class)
abstract class FirstStepDatabase : RoomDatabase() {

    abstract fun offerDao(): OfferDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun applicationDao(): ApplicationDao
    abstract fun studentProfileDao(): StudentProfileDao

    companion object {
        @Volatile
        private var INSTANCE: FirstStepDatabase? = null

        fun getInstance(context: Context): FirstStepDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FirstStepDatabase::class.java,
                    "firststep_offline.db"
                )
                    .setJournalMode(JournalMode.TRUNCATE)
                    .setQueryCallback(
                        { sqlQuery, bindArgs ->
                            Log.d("FIRSTSTEP_SQL", "SQL: $sqlQuery | args=$bindArgs")
                        },
                        Executors.newSingleThreadExecutor()
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
