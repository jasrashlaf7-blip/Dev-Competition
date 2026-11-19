package com.olympiades.firststep.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.BuildConfig
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
    version = 2,
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

        /**
         * Lossless Room v1 -> v2 migration:
         * - Adds monthly_stipend_dzd & start_date to offers
         * - Adds rich descriptive columns to favorites & applications
         * - Preserves 100% of existing favorites, applications, status history, and profile rows
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE offers ADD COLUMN monthly_stipend_dzd INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE offers ADD COLUMN start_date TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE offers
                    SET monthly_stipend_dzd = CASE WHEN is_paid = 1 THEN 20000 + ((id % 5) * 5000) ELSE 0 END,
                        start_date = '2026-12-01'
                    """.trimIndent()
                )

                db.execSQL("ALTER TABLE favorites ADD COLUMN offer_title_fr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE favorites ADD COLUMN offer_title_ar TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE favorites ADD COLUMN company_name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE favorites ADD COLUMN city_fr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE favorites ADD COLUMN domain_fr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE favorites ADD COLUMN monthly_stipend_dzd INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE favorites ADD COLUMN start_date TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE favorites ADD COLUMN deadline TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE favorites ADD COLUMN personal_note TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE favorites
                    SET offer_title_fr = COALESCE((SELECT title_fr FROM offers WHERE offers.id = favorites.offer_id), ''),
                        offer_title_ar = COALESCE((SELECT title_ar FROM offers WHERE offers.id = favorites.offer_id), ''),
                        company_name = COALESCE((SELECT c.name FROM offers o INNER JOIN companies c ON o.company_id = c.id WHERE o.id = favorites.offer_id), ''),
                        city_fr = COALESCE((SELECT l.city_fr FROM offers o INNER JOIN locations l ON o.location_id = l.id WHERE o.id = favorites.offer_id), ''),
                        domain_fr = COALESCE((SELECT d.name_fr FROM offers o INNER JOIN domains d ON o.domain_id = d.id WHERE o.id = favorites.offer_id), ''),
                        monthly_stipend_dzd = COALESCE((SELECT monthly_stipend_dzd FROM offers WHERE offers.id = favorites.offer_id), 0),
                        start_date = COALESCE((SELECT start_date FROM offers WHERE offers.id = favorites.offer_id), ''),
                        deadline = COALESCE((SELECT deadline FROM offers WHERE offers.id = favorites.offer_id), '')
                    """.trimIndent()
                )

                db.execSQL("ALTER TABLE applications ADD COLUMN offer_title_snapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE applications ADD COLUMN company_name_snapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE applications ADD COLUMN monthly_stipend_dzd INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE applications
                    SET offer_title_snapshot = COALESCE((SELECT title_fr FROM offers WHERE offers.id = applications.offer_id), ''),
                        company_name_snapshot = COALESCE((SELECT c.name FROM offers o INNER JOIN companies c ON o.company_id = c.id WHERE o.id = applications.offer_id), ''),
                        monthly_stipend_dzd = COALESCE((SELECT monthly_stipend_dzd FROM offers WHERE offers.id = applications.offer_id), 0)
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): FirstStepDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FirstStepDatabase::class.java,
                    "firststep_offline.db"
                )
                    .setJournalMode(JournalMode.TRUNCATE)
                    .addMigrations(MIGRATION_1_2)
                    .apply {
                        if (BuildConfig.DEBUG) {
                            setQueryCallback(
                                { sqlQuery, bindArgs ->
                                    // Security rule: never write personal data / bind argument values into Logcat
                                    Log.d("FIRSTSTEP_SQL", "SQL: $sqlQuery (args=${bindArgs.size})")
                                },
                                Executors.newSingleThreadExecutor()
                            )
                        }
                    }
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
