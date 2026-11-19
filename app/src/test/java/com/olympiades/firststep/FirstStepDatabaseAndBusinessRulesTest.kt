package com.olympiades.firststep

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.olympiades.firststep.data.local.FirstStepDatabase
import com.olympiades.firststep.data.local.SeedDataLoader
import com.olympiades.firststep.data.preferences.UserPreferencesDataStore
import com.olympiades.firststep.data.repository.ApplicationSubmissionError
import com.olympiades.firststep.data.repository.FirstStepRepositoryImpl
import com.olympiades.firststep.data.repository.StatusTransitionError
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.OfferSortOption
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.domain.validation.ApplicationValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirstStepDatabaseAndBusinessRulesTest {

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
    fun `seed loads 32 offers and default profile without duplicating on second call`() = runTest {
        repository.ensureInitialized()
        repository.ensureInitialized()

        val allOffers = repository.observeFilteredOffers(OfferFilterCriteria()).first()
        assertEquals(32, allOffers.size)

        val closedCount = allOffers.count { it.status == OfferStatus.CLOSED }
        assertTrue(closedCount >= 2)

        val profile = repository.observeStudentProfile().first()
        assertNotNull(profile)
        assertEquals("Amina Saidi", profile?.fullName)
        assertEquals("amina.saidi@email.dz", profile?.email)
    }

    @Test
    fun `combinable filters and deadline sorting work accurately`() = runTest {
        repository.ensureInitialized()

        val ictOranPaidOpen = repository.observeFilteredOffers(
            OfferFilterCriteria(
                domainId = 1L,
                locationId = 1L,
                paidOnly = true,
                openOnly = true,
                sortOption = OfferSortOption.CLOSEST_DEADLINE
            )
        ).first()

        assertTrue(ictOranPaidOpen.isNotEmpty())
        assertTrue(ictOranPaidOpen.all { it.domain.id == 1L && it.location.id == 1L && it.isPaid && it.status == OfferStatus.OPEN })

        val deadlines = ictOranPaidOpen.map { it.deadline }
        assertEquals(deadlines.sorted(), deadlines)
    }

    @Test
    fun `favorites toggle and restore without duplicates`() = runTest {
        repository.ensureInitialized()

        val added = repository.toggleFavorite(1L)
        assertTrue(added)
        assertEquals(1, repository.observeFavoriteCount().first())

        val removed = repository.toggleFavorite(1L)
        assertFalse(removed)
        assertEquals(0, repository.observeFavoriteCount().first())

        repository.restoreFavorite(1L)
        assertEquals(1, repository.observeFavoriteCount().first())
    }

    @Test
    fun `application submission creates FS-2026-0001 and blocks closed and duplicate offers`() = runTest {
        repository.ensureInitialized()

        // Submit to OPEN offer 1
        val result = repository.submitApplication(
            offerId = 1L,
            applicantName = "Amina Saidi",
            applicantEmail = "amina.saidi@email.dz",
            applicantPhone = "+213 555 12 34 56",
            coverLetter = "Très motivée pour rejoindre l'équipe Android de TechAfrica.",
            cvUri = "",
            initialNote = "Soumission initiale"
        )
        assertTrue(result.isSuccess)
        assertEquals("FS-2026-0001", result.getOrNull())

        // Duplicate application to offer 1 must fail
        val duplicate = repository.submitApplication(
            offerId = 1L,
            applicantName = "Amina Saidi",
            applicantEmail = "amina.saidi@email.dz",
            applicantPhone = "+213 555 12 34 56",
            coverLetter = "Deuxième tentative sur la même offre.",
            cvUri = "",
            initialNote = ""
        )
        assertTrue(duplicate.exceptionOrNull() is ApplicationSubmissionError.AlreadyApplied)

        // Applying to CLOSED offer 15 must fail
        val closedResult = repository.submitApplication(
            offerId = 15L,
            applicantName = "Amina Saidi",
            applicantEmail = "amina.saidi@email.dz",
            applicantPhone = "+213 555 12 34 56",
            coverLetter = "Candidature sur une offre clôturée.",
            cvUri = "",
            initialNote = ""
        )
        assertTrue(closedResult.exceptionOrNull() is ApplicationSubmissionError.OfferClosed)
    }

    @Test
    fun `status history logs transitions and locks final states`() = runTest {
        repository.ensureInitialized()

        repository.submitApplication(
            offerId = 2L,
            applicantName = "Amina Saidi",
            applicantEmail = "amina.saidi@email.dz",
            applicantPhone = "+213 555 12 34 56",
            coverLetter = "Motivation complète pour le stage Data Analyst.",
            cvUri = "",
            initialNote = "Candidature envoyée"
        )

        val apps = repository.observeApplications(null).first()
        val appId = apps.first().id

        assertTrue(
            repository.updateApplicationStatus(appId, ApplicationStatus.UNDER_REVIEW, "Dossier étudié").isSuccess
        )
        assertTrue(
            repository.updateApplicationStatus(appId, ApplicationStatus.ACCEPTED, "Offre acceptée").isSuccess
        )

        // Reverting from ACCEPTED must be blocked
        val revertResult = repository.updateApplicationStatus(
            appId,
            ApplicationStatus.UNDER_REVIEW,
            "Tentative de retour"
        )
        assertTrue(revertResult.exceptionOrNull() is StatusTransitionError.FinalStateLocked)

        val updatedApp = repository.observeApplications(null).first().first()
        assertEquals(ApplicationStatus.ACCEPTED, updatedApp.currentStatus)
        assertEquals(3, updatedApp.history.size)
    }

    @Test
    fun `form validation detects invalid email and short cover letter`() {
        val result = ApplicationValidator.validateApplicationForm(
            fullName = "A",
            email = "bad-email",
            phone = "123",
            coverLetter = "court"
        )
        assertFalse(result.isValid)
        assertNotNull(result.nameError)
        assertNotNull(result.emailError)
        assertNotNull(result.phoneError)
        assertNotNull(result.coverLetterError)
    }

    @Test
    fun `search matches any word in any order across title company city domain skills and accents`() = runTest {
        repository.ensureInitialized()

        // Search by skill + city in reverse order
        val kotlinOran = repository.observeFilteredOffers(
            OfferFilterCriteria(query = "Oran Kotlin")
        ).first()
        assertTrue(kotlinOran.isNotEmpty())
        assertTrue(kotlinOran.all { offer ->
            offer.location.cityFr.contains("Oran", ignoreCase = true)
        })

        // Search without French accents ("developpement" -> "Développement")
        val noAccentResults = repository.observeFilteredOffers(
            OfferFilterCriteria(query = "developpement")
        ).first()
        assertTrue(noAccentResults.isNotEmpty())

        // Search by a MIDDLE or LAST word in the offer/course title (never restricted to first word)
        val createdId = repository.createOffer(
            companyId = 1L,
            domainId = 1L,
            locationId = 1L,
            titleFr = "Développeur Mobile Android Kotlin",
            titleAr = "مطور تطبيقات أندرويد كوتلن",
            descriptionFr = "Création d'applications natives.",
            descriptionAr = "تطوير تطبيقات أندرويد.",
            durationMonths = 4,
            isPaid = true,
            status = OfferStatus.OPEN,
            deadline = "2026-12-30"
        ).getOrThrow()

        // Search by middle word ("Mobile") and last word ("Kotlin") in reverse order
        val middleLastResults = repository.observeFilteredOffers(
            OfferFilterCriteria(query = "Kotlin Mobile")
        ).first()
        assertTrue(middleLastResults.any { it.id == createdId })
        // Offer whose title contains all tokens should be ranked at the top (Tier 0)
        assertTrue(
            middleLastResults.first().titleFr.contains("Mobile", ignoreCase = true) &&
                middleLastResults.first().titleFr.contains("Kotlin", ignoreCase = true)
        )

        // Search Arabic middle word with alef variant ("اندرويد" matches "أندرويد")
        val arabicMiddleWord = repository.observeFilteredOffers(
            OfferFilterCriteria(query = "اندرويد")
        ).first()
        assertTrue(arabicMiddleWord.any { it.id == createdId })
    }

    @Test
    fun `editing application details updates sqlite row and history`() = runTest {
        repository.ensureInitialized()

        repository.submitApplication(
            offerId = 1L,
            applicantName = "Amina Saidi",
            applicantEmail = "amina.saidi@email.dz",
            applicantPhone = "+213 555 12 34 56",
            coverLetter = "Lettre initiale suffisamment longue pour validation.",
            cvUri = "",
            initialNote = "Soumise"
        )

        val appId = repository.observeApplications(null).first().first().id
        val editResult = repository.updateApplicationDetails(
            applicationId = appId,
            applicantName = "Djasser Mansouri",
            applicantEmail = "djasser@email.dz",
            applicantPhone = "+213 555 99 88 77",
            coverLetter = "Lettre modifiée avec de nouveaux détails techniques.",
            cvUri = "content://cv/djasser.pdf",
            editNote = "Informations modifiées"
        )
        assertTrue(editResult.isSuccess)

        val updated = repository.observeApplications(null).first().first()
        assertEquals("Djasser Mansouri", updated.applicantName)
        assertEquals("djasser@email.dz", updated.applicantEmail)
        assertEquals("content://cv/djasser.pdf", updated.cvUri)
        assertEquals(2, updated.history.size)
    }

    @Test
    fun `create update and delete offer persists in sqlite`() = runTest {
        repository.ensureInitialized()

        val createResult = repository.createOffer(
            companyId = 1L,
            domainId = 1L,
            locationId = 1L,
            titleFr = "Stage Android Avancé",
            titleAr = "تربص أندرويد متقدم",
            descriptionFr = "Développement Kotlin et Jetpack Compose hors-ligne.",
            descriptionAr = "تطوير تطبيقات أندرويد باستخدام كوتلن.",
            durationMonths = 4,
            isPaid = true,
            status = OfferStatus.OPEN,
            deadline = "2026-12-30"
        )
        assertTrue(createResult.isSuccess)
        val newOfferId = createResult.getOrThrow()

        val created = repository.observeOfferById(newOfferId).first()
        assertNotNull(created)
        assertEquals("Stage Android Avancé", created?.titleFr)

        val updateResult = repository.updateOffer(
            offerId = newOfferId,
            companyId = 1L,
            domainId = 1L,
            locationId = 2L,
            titleFr = "Stage Android Senior",
            titleAr = "تربص أندرويد",
            descriptionFr = "Description mise à jour.",
            descriptionAr = "وصف محدث.",
            durationMonths = 6,
            isPaid = false,
            status = OfferStatus.CLOSED,
            deadline = "2026-12-31"
        )
        assertTrue(updateResult.isSuccess)

        val updated = repository.observeOfferById(newOfferId).first()
        assertEquals("Stage Android Senior", updated?.titleFr)
        assertEquals(6, updated?.durationMonths)
        assertEquals(OfferStatus.CLOSED, updated?.status)

        assertTrue(repository.deleteOffer(newOfferId).isSuccess)
        val deleted = repository.observeOfferById(newOfferId).first()
        assertEquals(null, deleted)
    }
}
