package com.olympiades.firststep

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.olympiades.firststep.data.local.FirstStepDatabase
import com.olympiades.firststep.data.local.SeedDataLoader
import com.olympiades.firststep.data.preferences.UserPreferencesDataStore
import com.olympiades.firststep.data.repository.FirstStepRepositoryImpl
import com.olympiades.firststep.data.preferences.UserPreferencesState
import com.olympiades.firststep.domain.export.CsvExporter
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.Company
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.OfferSortOption
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.domain.model.StudentProfile
import com.olympiades.firststep.domain.repository.FirstStepRepository
import com.olympiades.firststep.domain.security.PinSecurityHelper
import com.olympiades.firststep.domain.usecase.GetDomainsAndLocationsUseCase
import com.olympiades.firststep.domain.usecase.GetFilteredOffersUseCase
import com.olympiades.firststep.domain.usecase.ManageOfferUseCase
import com.olympiades.firststep.domain.usecase.ToggleFavoriteUseCase
import com.olympiades.firststep.domain.usecase.UpdateApplicationStatusUseCase
import com.olympiades.firststep.domain.assistant.AssistantContext
import com.olympiades.firststep.domain.assistant.AssistantTopic
import com.olympiades.firststep.domain.assistant.OfflineAssistantEngine
import com.olympiades.firststep.ui.i18n.stringsFor
import com.olympiades.firststep.ui.viewmodel.ApplicationsUiState
import com.olympiades.firststep.ui.viewmodel.ApplicationsViewModel
import com.olympiades.firststep.ui.viewmodel.OffersUiState
import com.olympiades.firststep.ui.viewmodel.OffersViewModel
import com.olympiades.firststep.ui.viewmodel.OfflineAssistantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ModuleCSecurityAndMigrationTest {

    private lateinit var context: Context
    private lateinit var database: FirstStepDatabase
    private lateinit var repository: FirstStepRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, FirstStepDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val seedLoader = SeedDataLoader(context, database)
        val prefs = UserPreferencesDataStore(context)
        repository = FirstStepRepositoryImpl(database, seedLoader, prefs)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `room migration v1 to v2 preserves all user data and backfills rich columns`() = runTest {
        val dbName = "test_migration_v1_to_v2.db"
        context.deleteDatabase(dbName)

        // Build exact Room v1 schema and seed user data before migrating
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `domains` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name_fr` TEXT NOT NULL, `name_ar` TEXT NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_domains_name_fr` ON `domains` (`name_fr`)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_domains_name_ar` ON `domains` (`name_ar`)")

                        db.execSQL("CREATE TABLE IF NOT EXISTS `locations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `city_fr` TEXT NOT NULL, `city_ar` TEXT NOT NULL, `region_fr` TEXT NOT NULL, `region_ar` TEXT NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_locations_city_fr_region_fr` ON `locations` (`city_fr`, `region_fr`)")

                        db.execSQL("CREATE TABLE IF NOT EXISTS `companies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sector_fr` TEXT NOT NULL, `sector_ar` TEXT NOT NULL, `description_fr` TEXT NOT NULL, `description_ar` TEXT NOT NULL, `website` TEXT NOT NULL, `logo_url` TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `skills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name_fr` TEXT NOT NULL, `name_ar` TEXT NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_skills_name_fr` ON `skills` (`name_fr`)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_skills_name_ar` ON `skills` (`name_ar`)")

                        db.execSQL("CREATE TABLE IF NOT EXISTS `student_profiles` (`id` INTEGER NOT NULL, `full_name` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, `university` TEXT NOT NULL, `degree_level` TEXT NOT NULL, `bio` TEXT NOT NULL, `cv_uri` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`))")

                        db.execSQL("CREATE TABLE IF NOT EXISTS `offers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `company_id` INTEGER NOT NULL, `domain_id` INTEGER NOT NULL, `location_id` INTEGER NOT NULL, `title_fr` TEXT NOT NULL, `title_ar` TEXT NOT NULL, `description_fr` TEXT NOT NULL, `description_ar` TEXT NOT NULL, `duration_months` INTEGER NOT NULL, `is_paid` INTEGER NOT NULL, `status` TEXT NOT NULL, `deadline` TEXT NOT NULL, `created_at` TEXT NOT NULL, FOREIGN KEY(`company_id`) REFERENCES `companies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`domain_id`) REFERENCES `domains`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`location_id`) REFERENCES `locations`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offers_company_id` ON `offers` (`company_id`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offers_domain_id` ON `offers` (`domain_id`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offers_location_id` ON `offers` (`location_id`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offers_deadline` ON `offers` (`deadline`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offers_status` ON `offers` (`status`)")

                        db.execSQL("CREATE TABLE IF NOT EXISTS `offer_skills` (`offer_id` INTEGER NOT NULL, `skill_id` INTEGER NOT NULL, PRIMARY KEY(`offer_id`, `skill_id`), FOREIGN KEY(`offer_id`) REFERENCES `offers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`skill_id`) REFERENCES `skills`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offer_skills_skill_id` ON `offer_skills` (`skill_id`)")

                        db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `offer_id` INTEGER NOT NULL, `added_at` TEXT NOT NULL, FOREIGN KEY(`offer_id`) REFERENCES `offers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_offer_id` ON `favorites` (`offer_id`)")

                        db.execSQL("CREATE TABLE IF NOT EXISTS `applications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference` TEXT NOT NULL, `offer_id` INTEGER NOT NULL, `applicant_name` TEXT NOT NULL, `applicant_email` TEXT NOT NULL, `applicant_phone` TEXT NOT NULL, `cover_letter` TEXT NOT NULL, `cv_uri` TEXT NOT NULL, `current_status` TEXT NOT NULL, `applied_at` TEXT NOT NULL, FOREIGN KEY(`offer_id`) REFERENCES `offers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_applications_reference` ON `applications` (`reference`)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_applications_offer_id` ON `applications` (`offer_id`)")

                        db.execSQL("CREATE TABLE IF NOT EXISTS `status_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `application_id` INTEGER NOT NULL, `old_status` TEXT, `new_status` TEXT NOT NULL, `changed_at` TEXT NOT NULL, `note` TEXT NOT NULL, FOREIGN KEY(`application_id`) REFERENCES `applications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_status_history_application_id` ON `status_history` (`application_id`)")

                        db.execSQL("INSERT INTO domains(id, name_fr, name_ar) VALUES (1, 'Informatique', 'المعلوماتية')")
                        db.execSQL("INSERT INTO locations(id, city_fr, city_ar, region_fr, region_ar) VALUES (1, 'Oran', 'وهران', 'Ouest', 'الغرب')")
                        db.execSQL("INSERT INTO companies(id, name, sector_fr, sector_ar, description_fr, description_ar, website, logo_url) VALUES (1, 'TechAfrica', 'IT', 'تقنية', 'Desc', 'وصف', '', '')")
                        db.execSQL("INSERT INTO student_profiles(id, full_name, email, phone, university, degree_level, bio, cv_uri, updated_at) VALUES (1, 'Amina Saidi', 'amina@dz.edu', '0555112233', 'USTO', 'Master 2', 'Bio', '', '2026-11-01')")
                        db.execSQL("INSERT INTO offers(id, company_id, domain_id, location_id, title_fr, title_ar, description_fr, description_ar, duration_months, is_paid, status, deadline, created_at) VALUES (1, 1, 1, 1, 'Stage Kotlin', 'تربص كوتلن', 'Desc', 'وصف', 4, 1, 'OPEN', '2026-12-30', '2026-11-01')")
                        db.execSQL("INSERT INTO favorites(id, offer_id, added_at) VALUES (1, 1, '2026-11-10')")
                        db.execSQL("INSERT INTO applications(id, reference, offer_id, applicant_name, applicant_email, applicant_phone, cover_letter, cv_uri, current_status, applied_at) VALUES (1, 'FS-2026-0001', 1, 'Amina Saidi', 'amina@dz.edu', '0555112233', 'Motivation complete', '', 'SUBMITTED', '2026-11-05')")
                        db.execSQL("INSERT INTO status_history(id, application_id, old_status, new_status, changed_at, note) VALUES (1, 1, NULL, 'SUBMITTED', '2026-11-05', 'Init')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()

        // Open with Room v2 and MIGRATION_1_2
        val migratedDb = Room.databaseBuilder(context, FirstStepDatabase::class.java, dbName)
            .addMigrations(FirstStepDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        try {
            val readable = migratedDb.openHelper.readableDatabase
            readable.query("SELECT monthly_stipend_dzd, start_date FROM offers WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertTrue(c.getInt(0) > 0)
                assertEquals("2026-12-01", c.getString(1))
            }
            readable.query("SELECT offer_title_fr, company_name, city_fr, domain_fr FROM favorites WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Stage Kotlin", c.getString(0))
                assertEquals("TechAfrica", c.getString(1))
                assertEquals("Oran", c.getString(2))
                assertEquals("Informatique", c.getString(3))
            }
            readable.query("SELECT reference, offer_title_snapshot, company_name_snapshot FROM applications WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("FS-2026-0001", c.getString(0))
                assertEquals("Stage Kotlin", c.getString(1))
                assertEquals("TechAfrica", c.getString(2))
            }
            assertEquals(1, migratedDb.favoriteDao().observeFavoriteCount().first())
            val migratedApps = migratedDb.applicationDao().observeApplications(null).first()
            assertEquals(1, migratedApps.size)
            assertEquals(1, migratedApps.first().history.size)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `csv exporter neutralizes formula injection and formats RFC 4180 cells`() {
        assertEquals("\"'=SUM(A1:A2)\"", CsvExporter.sanitizeCsvCell("=SUM(A1:A2)"))
        assertEquals("\"'+cmd|' /C calc'!A0\"", CsvExporter.sanitizeCsvCell("+cmd|' /C calc'!A0"))
        assertEquals("\"'-1+1\"", CsvExporter.sanitizeCsvCell("-1+1"))
        assertEquals("\"'@SUM(1,1)\"", CsvExporter.sanitizeCsvCell("@SUM(1,1)"))
        assertEquals("\"'\t=1+1\"", CsvExporter.sanitizeCsvCell("\t=1+1"))
        assertEquals("\"Hello \"\"World\"\"\"", CsvExporter.sanitizeCsvCell("Hello \"World\""))

        val sampleApp = buildSampleApplication(status = ApplicationStatus.SUBMITTED, appliedAt = "2026-11-01")
            .copy(coverLetter = "=HYPERLINK(\"http://evil.test\",\"Click\")")
        val csv = CsvExporter.buildApplicationsCsv(listOf(sampleApp), todayIso = "2026-11-19")
        assertTrue(csv.startsWith('\uFEFF'))
        assertTrue(csv.contains("Reference,Offer_Title,Company"))
        assertTrue(csv.contains("\"'=HYPERLINK("))
        assertTrue(csv.contains("\"YES\""))
    }

    @Test
    fun `unanswered application over 7 days flag triggers only for submitted older than 7 days`() {
        val today = "2026-11-19"
        val oldSubmitted = buildSampleApplication(ApplicationStatus.SUBMITTED, appliedAt = "2026-11-10")
        val recentSubmitted = buildSampleApplication(ApplicationStatus.SUBMITTED, appliedAt = "2026-11-15")
        val oldUnderReview = buildSampleApplication(ApplicationStatus.UNDER_REVIEW, appliedAt = "2026-11-01")

        assertTrue(oldSubmitted.isUnansweredOver7Days(today))
        assertEquals(9, oldSubmitted.daysSinceApplied(today))
        assertFalse(recentSubmitted.isUnansweredOver7Days(today))
        assertFalse(oldUnderReview.isUnansweredOver7Days(today))
    }

    @Test
    fun `stipend filter and highest stipend sort work with Room queries`() = runTest {
        repository.ensureInitialized()

        val filtered = repository.observeFilteredOffers(
            OfferFilterCriteria(
                minStipendDzd = 25_000,
                sortOption = OfferSortOption.HIGHEST_STIPEND
            )
        ).first()

        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.isPaid && it.monthlyStipendDzd >= 25_000 })
        val stipends = filtered.map { it.monthlyStipendDzd }
        assertEquals(stipends.sortedDescending(), stipends)
    }

    @Test
    fun `favorites persist full descriptive metadata and personal note in sqlite`() = runTest {
        repository.ensureInitialized()

        assertTrue(repository.toggleFavorite(1L))
        repository.updateFavoriteNote(1L, "Appeler RH lundi matin à 9h")

        val favList = repository.observeFavorites().first()
        assertEquals(1, favList.size)
        assertEquals("Appeler RH lundi matin à 9h", favList.first().personalNote)

        // Inspect raw favorites table columns in SQLite (more than just offer_id)
        database.openHelper.readableDatabase.query(
            "SELECT offer_title_fr, company_name, city_fr, domain_fr, monthly_stipend_dzd, start_date, deadline, personal_note FROM favorites WHERE offer_id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getString(0).isNotBlank())
            assertTrue(cursor.getString(1).isNotBlank())
            assertTrue(cursor.getString(2).isNotBlank())
            assertTrue(cursor.getString(3).isNotBlank())
            assertTrue(cursor.getString(5).isNotBlank())
            assertTrue(cursor.getString(6).isNotBlank())
            assertEquals("Appeler RH lundi matin à 9h", cursor.getString(7))
        }
    }

    @Test
    fun `pin security helper validates format and verifies salted sha256 hash`() {
        assertTrue(PinSecurityHelper.isValidPinFormat("2026"))
        assertFalse(PinSecurityHelper.isValidPinFormat("123"))
        assertFalse(PinSecurityHelper.isValidPinFormat("12ab"))

        val salt1 = PinSecurityHelper.generateSalt()
        val salt2 = PinSecurityHelper.generateSalt()
        assertFalse(salt1 == salt2)

        val hash = PinSecurityHelper.hashPin("2026", salt1)
        assertTrue(PinSecurityHelper.verifyPin("2026", salt1, hash))
        assertFalse(PinSecurityHelper.verifyPin("9999", salt1, hash))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodels expose stipend toggle and filtered csv export`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModelStore = ViewModelStore()
        try {
            repository.ensureInitialized()

            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return when {
                        modelClass.isAssignableFrom(OffersViewModel::class.java) -> OffersViewModel(
                            repository = repository,
                            getFilteredOffersUseCase = GetFilteredOffersUseCase(repository),
                            getDomainsAndLocationsUseCase = GetDomainsAndLocationsUseCase(repository),
                            manageOfferUseCase = ManageOfferUseCase(repository),
                            toggleFavoriteUseCase = ToggleFavoriteUseCase(repository)
                        ) as T
                        modelClass.isAssignableFrom(ApplicationsViewModel::class.java) -> ApplicationsViewModel(
                            UpdateApplicationStatusUseCase(repository)
                        ) as T
                        else -> throw IllegalArgumentException(modelClass.name)
                    }
                }
            }
            val provider = ViewModelProvider(viewModelStore, factory)
            val offersVm = provider[OffersViewModel::class.java]
            offersVm.toggleMinStipendFilter(25_000)
            offersVm.selectSortOption(OfferSortOption.HIGHEST_STIPEND)

            val offersState = offersVm.uiState.first {
                it is OffersUiState.Success && (it.criteria.minStipendDzd ?: 0) >= 25_000
            } as OffersUiState.Success
            assertTrue(offersState.offers.isNotEmpty())
            assertTrue(offersState.offers.all { it.monthlyStipendDzd >= 25_000 })

            repository.submitApplication(
                offerId = 1L,
                applicantName = "Amina Saidi",
                applicantEmail = "amina.saidi@email.dz",
                applicantPhone = "+213 555 12 34 56",
                coverLetter = "Lettre de motivation complete pour test ViewModel CSV.",
                cvUri = "",
                initialNote = "Soumise"
            )
            val appsVm = provider[ApplicationsViewModel::class.java]
            val appsState = appsVm.uiState.first {
                it is ApplicationsUiState.Success && it.applications.isNotEmpty()
            } as ApplicationsUiState.Success
            assertEquals(1, appsState.applications.size)

            val (count, csv) = appsVm.buildFilteredCsv("2026-11-19")
            assertEquals(1, count)
            assertTrue(csv.contains("FS-2026-0001"))
        } finally {
            viewModelStore.clear()
            Dispatchers.resetMain()
        }
    }

    private fun buildSampleApplication(
        status: ApplicationStatus,
        appliedAt: String
    ): ApplicationWithDetails {
        val offer = Offer(
            id = 1L,
            company = Company(1L, "TechAfrica", "IT", "تقنية", "", "", "", ""),
            domain = Domain(1L, "Informatique", "المعلوماتية"),
            location = Location(1L, "Oran", "وهران", "Ouest", "الغرب"),
            titleFr = "Stage Android",
            titleAr = "تربص أندرويد",
            descriptionFr = "Desc",
            descriptionAr = "وصف",
            durationMonths = 3,
            isPaid = true,
            monthlyStipendDzd = 30_000,
            startDate = "2026-12-01",
            status = OfferStatus.OPEN,
            deadline = "2026-12-31",
            createdAt = "2026-11-01",
            skills = emptyList(),
            isFavorite = false,
            applicationStatus = status,
            applicationReference = "FS-2026-0001"
        )
        return ApplicationWithDetails(
            id = 1L,
            reference = "FS-2026-0001",
            offer = offer,
            applicantName = "Amina Saidi",
            applicantEmail = "amina@dz.edu",
            applicantPhone = "+213 555 00 11 22",
            coverLetter = "Motivation complete",
            cvUri = "",
            currentStatus = status,
            appliedAt = appliedAt,
            history = emptyList()
        )
    }

    @Test
    fun `seed loads all 58 Algerian wilayas without breaking existing offers`() = runTest {
        repository.ensureInitialized()
        val locations = repository.observeLocations().first()
        assertEquals(58, locations.size)
        assertTrue(locations.any { it.cityFr == "Oran" && it.regionFr.contains("31") })
        assertTrue(locations.any { it.cityFr == "Alger" && it.regionFr.contains("16") })
        assertTrue(locations.any { it.cityFr == "Constantine" && it.regionFr.contains("25") })
        assertTrue(locations.any { it.cityFr == "Adrar" && it.regionFr.contains("01") })
        assertTrue(locations.any { it.cityFr == "El Meniaa" && it.regionFr.contains("58") })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `offers viewmodel works against FakeFirstStepRepository`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModelStore = ViewModelStore()
        val fakeRepo = FakeFirstStepRepository()
        try {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OffersViewModel(
                        repository = fakeRepo,
                        getFilteredOffersUseCase = GetFilteredOffersUseCase(fakeRepo),
                        getDomainsAndLocationsUseCase = GetDomainsAndLocationsUseCase(fakeRepo),
                        manageOfferUseCase = ManageOfferUseCase(fakeRepo),
                        toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepo)
                    ) as T
                }
            }
            val provider = ViewModelProvider(viewModelStore, factory)
            val vm = provider[OffersViewModel::class.java]

            val initialState = vm.uiState.first { it is OffersUiState.Success } as OffersUiState.Success
            assertEquals(2, initialState.offers.size)

            vm.updateQuery("Kotlin")
            val searchedState = vm.uiState.first {
                it is OffersUiState.Success && it.offers.size == 1
            } as OffersUiState.Success
            assertEquals(101L, searchedState.offers.first().id)

            vm.toggleFavorite(101L)
            assertTrue(fakeRepo.favoriteIds.contains(101L))
        } finally {
            viewModelStore.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `offline assistant engine provides bilingual guide answers and live offer recommendations`() = runTest {
        repository.ensureInitialized()
        val offers = repository.observeFilteredOffers(OfferFilterCriteria()).first()
        val overdueApp = buildSampleApplication(ApplicationStatus.SUBMITTED, appliedAt = "2026-11-01")

        val frContext = AssistantContext(
            language = AppLanguage.FR,
            currentRoute = "offers",
            offers = offers,
            favorites = emptyList(),
            applications = listOf(overdueApp),
            profile = null,
            todayIso = "2026-11-19"
        )

        val welcome = OfflineAssistantEngine.welcomeMessage(frContext)
        assertTrue(welcome.message.contains("100 % local"))
        assertTrue(welcome.message.contains("1 candidature(s) sans réponse"))
        assertTrue(welcome.followUpQuestions.isNotEmpty())

        val recommended = OfflineAssistantEngine.answerTopic(AssistantTopic.RECOMMEND_BEST_OFFERS, frContext)
        assertTrue(recommended.suggestedOffers.isNotEmpty())

        val csvHelp = OfflineAssistantEngine.answerQuestion("Comment exporter mes candidatures en CSV ?", frContext)
        assertTrue(csvHelp.message.contains("CSV", ignoreCase = true))

        val cityQuery = OfflineAssistantEngine.answerQuestion("Oran", frContext)
        assertTrue(cityQuery.suggestedOffers.isNotEmpty())

        val arContext = frContext.copy(language = AppLanguage.AR, currentRoute = "applications")
        val arQuestions = OfflineAssistantEngine.quickQuestionsFor(arContext)
        assertEquals(AssistantTopic.CHECK_MY_APPLICATIONS, arQuestions.first().topic)

        val arSummary = OfflineAssistantEngine.answerTopic(AssistantTopic.CHECK_MY_APPLICATIONS, arContext)
        assertTrue(arSummary.message.contains("FS-2026-0001"))

        val enContext = frContext.copy(language = AppLanguage.EN, currentRoute = "offers")
        val enWelcome = OfflineAssistantEngine.welcomeMessage(enContext)
        assertTrue(enWelcome.message.contains("100% local"))
        val enApplyHelp = OfflineAssistantEngine.answerQuestion("How do I apply with my PDF CV?", enContext)
        assertTrue(enApplyHelp.message.contains("APPLY NOW"))
        val enStrings = stringsFor(AppLanguage.EN)
        assertEquals("Help ?", enStrings.helpButtonLabel)
        assertEquals("FIRSTSTEP – Internships", enStrings.appTitleOffers)
    }

    @Test
    fun `user preferences persist English language selection`() = runTest {
        val prefs = UserPreferencesDataStore(context)
        prefs.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, prefs.preferencesFlow.first().language)
        prefs.setLanguage(AppLanguage.FR)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `offline assistant viewmodel answers quick question buttons and free text offline`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModelStore = ViewModelStore()
        val fakeRepo = FakeFirstStepRepository()
        try {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OfflineAssistantViewModel(fakeRepo) as T
                }
            }
            val vm = ViewModelProvider(viewModelStore, factory)[OfflineAssistantViewModel::class.java]
            vm.onAssistantOpened("offers")

            val initialMessages = vm.messages.value
            assertEquals(1, initialMessages.size)
            assertFalse(initialMessages.first().isFromUser)

            val firstQuestion = vm.quickQuestions.first { it.isNotEmpty() }.first()
            vm.askQuickQuestion(firstQuestion)
            assertEquals(3, vm.messages.value.size)
            assertTrue(vm.messages.value[1].isFromUser)
            assertFalse(vm.messages.value[2].isFromUser)

            vm.updateInputQuery("Kotlin Oran")
            vm.sendUserQuestion()
            assertEquals(5, vm.messages.value.size)
            assertEquals("", vm.inputQuery)
            assertTrue(vm.messages.value.last().suggestedOffers.any { it.offerId == 101L })
        } finally {
            viewModelStore.clear()
            Dispatchers.resetMain()
        }
    }

    private class FakeFirstStepRepository : FirstStepRepository {
        private val offersFlow = MutableStateFlow(
            listOf(
                Offer(
                    id = 101L,
                    company = Company(1L, "TechAfrica", "IT", "تقنية", "", "", "", ""),
                    domain = Domain(1L, "ICT", "تكنولوجيا"),
                    location = Location(1L, "Oran", "وهران", "Wilaya 31", "ولاية 31"),
                    titleFr = "Stage Android Kotlin",
                    titleAr = "تربص أندرويد",
                    descriptionFr = "Kotlin Compose",
                    descriptionAr = "كوتلن",
                    durationMonths = 3,
                    isPaid = true,
                    monthlyStipendDzd = 30_000,
                    startDate = "2026-12-01",
                    status = OfferStatus.OPEN,
                    deadline = "2027-01-31",
                    createdAt = "2026-11-10",
                    skills = emptyList(),
                    isFavorite = false,
                    applicationStatus = null,
                    applicationReference = null
                ),
                Offer(
                    id = 102L,
                    company = Company(2L, "DataMaghreb", "Data", "بيانات", "", "", "", ""),
                    domain = Domain(2L, "Data", "البيانات"),
                    location = Location(2L, "Alger", "الجزائر", "Wilaya 16", "ولاية 16"),
                    titleFr = "Stage Power BI",
                    titleAr = "تربص بيانات",
                    descriptionFr = "SQL & Dashboards",
                    descriptionAr = "بيانات",
                    durationMonths = 4,
                    isPaid = false,
                    monthlyStipendDzd = 0,
                    startDate = "2026-12-10",
                    status = OfferStatus.OPEN,
                    deadline = "2027-02-15",
                    createdAt = "2026-11-09",
                    skills = emptyList(),
                    isFavorite = false,
                    applicationStatus = null,
                    applicationReference = null
                )
            )
        )
        val favoriteIds = mutableSetOf<Long>()

        override suspend fun ensureInitialized() = Unit

        override fun observeFilteredOffers(criteria: OfferFilterCriteria): Flow<List<Offer>> =
            offersFlow.map { list ->
                list.filter { offer ->
                    (criteria.query.isBlank() || offer.titleFr.contains(criteria.query, ignoreCase = true)) &&
                        (!criteria.paidOnly || offer.isPaid) &&
                        (criteria.minStipendDzd == null || offer.monthlyStipendDzd >= criteria.minStipendDzd)
                }
            }

        override fun observeOfferById(offerId: Long): Flow<Offer?> =
            offersFlow.map { list -> list.find { it.id == offerId } }

        override fun observeDomains(): Flow<List<Domain>> =
            flowOf(listOf(Domain(1L, "ICT", "تكنولوجيا"), Domain(2L, "Data", "البيانات")))

        override fun observeLocations(): Flow<List<Location>> =
            flowOf(listOf(Location(1L, "Oran", "وهران", "Wilaya 31", "ولاية 31")))

        override fun observeCompanies(): Flow<List<Company>> = flowOf(emptyList())
        override suspend fun createOffer(
            companyId: Long, domainId: Long, locationId: Long, titleFr: String, titleAr: String,
            descriptionFr: String, descriptionAr: String, durationMonths: Int, isPaid: Boolean,
            monthlyStipendDzd: Int, startDate: String, status: OfferStatus, deadline: String
        ): Result<Long> = Result.success(103L)

        override suspend fun updateOffer(
            offerId: Long, companyId: Long, domainId: Long, locationId: Long, titleFr: String,
            titleAr: String, descriptionFr: String, descriptionAr: String, durationMonths: Int,
            isPaid: Boolean, monthlyStipendDzd: Int, startDate: String, status: OfferStatus, deadline: String
        ): Result<Unit> = Result.success(Unit)

        override suspend fun deleteOffer(offerId: Long): Result<Unit> = Result.success(Unit)
        override fun observeFavorites(): Flow<List<FavoriteOffer>> = flowOf(emptyList())
        override fun observeFavoriteCount(): Flow<Int> = flowOf(favoriteIds.size)
        override suspend fun toggleFavorite(offerId: Long): Boolean =
            if (favoriteIds.remove(offerId)) false else favoriteIds.add(offerId)
        override suspend fun restoreFavorite(offerId: Long) { favoriteIds.add(offerId) }
        override suspend fun updateFavoriteNote(offerId: Long, personalNote: String) = Unit
        override fun observeApplications(statusFilter: ApplicationStatus?): Flow<List<ApplicationWithDetails>> = flowOf(emptyList())
        override suspend fun submitApplication(
            offerId: Long, applicantName: String, applicantEmail: String,
            applicantPhone: String, coverLetter: String, cvUri: String, initialNote: String
        ): Result<String> = Result.success("FS-2026-0001")
        override suspend fun updateApplicationStatus(applicationId: Long, newStatus: ApplicationStatus, note: String): Result<Unit> = Result.success(Unit)
        override suspend fun updateApplicationDetails(
            applicationId: Long, applicantName: String, applicantEmail: String,
            applicantPhone: String, coverLetter: String, cvUri: String, editNote: String
        ): Result<Unit> = Result.success(Unit)
        override fun observeStudentProfile(): Flow<StudentProfile?> = flowOf(null)
        override suspend fun updateStudentProfile(profile: StudentProfile): Result<Unit> = Result.success(Unit)
        override fun observePreferences(): Flow<UserPreferencesState> = flowOf(UserPreferencesState())
        override suspend fun setLanguage(language: AppLanguage) = Unit
        override suspend fun setThemeMode(themeMode: AppThemeMode) = Unit
        override suspend fun enablePinLock(pin: String) = Unit
        override suspend fun disablePinLock() = Unit
        override suspend fun saveLastFilters(criteria: OfferFilterCriteria) = Unit
    }
}
