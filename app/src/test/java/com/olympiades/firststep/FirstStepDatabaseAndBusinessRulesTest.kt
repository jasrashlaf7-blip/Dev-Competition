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
}
