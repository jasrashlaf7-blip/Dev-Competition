package com.olympiades.firststep

import android.content.Context
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.olympiades.firststep.data.local.FirstStepDatabase
import com.olympiades.firststep.data.local.SeedDataLoader
import com.olympiades.firststep.data.preferences.UserPreferencesDataStore
import com.olympiades.firststep.data.repository.FirstStepRepositoryImpl
import com.olympiades.firststep.domain.usecase.GetDomainsAndLocationsUseCase
import com.olympiades.firststep.domain.usecase.GetFilteredOffersUseCase
import com.olympiades.firststep.domain.usecase.ManageOfferUseCase
import com.olympiades.firststep.domain.usecase.ToggleFavoriteUseCase
import com.olympiades.firststep.ui.screens.OffersScreen
import com.olympiades.firststep.ui.theme.FirstStepTheme
import com.olympiades.firststep.ui.viewmodel.OffersViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OffersScreenTypingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: FirstStepDatabase
    private lateinit var viewModel: OffersViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FirstStepDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val seedLoader = SeedDataLoader(context, database)
        val prefs = UserPreferencesDataStore(context)
        val repository = FirstStepRepositoryImpl(database, seedLoader, prefs)
        viewModel = OffersViewModel(
            repository = repository,
            getFilteredOffersUseCase = GetFilteredOffersUseCase(repository),
            getDomainsAndLocationsUseCase = GetDomainsAndLocationsUseCase(repository),
            manageOfferUseCase = ManageOfferUseCase(repository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `typing in search bar preserves left-to-right character order`() {
        composeTestRule.setContent {
            FirstStepTheme {
                OffersScreen(
                    viewModel = viewModel,
                    onOfferClick = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("offers_search_input").fetchSemanticsNodes().isNotEmpty()
        }

        val inputNode = composeTestRule.onNodeWithTag("offers_search_input")
        inputNode.performTextInput("D")
        inputNode.performTextInput("J")
        inputNode.performTextInput("A")
        inputNode.performTextInput("S")
        inputNode.performTextInput("S")
        inputNode.performTextInput("E")
        inputNode.performTextInput("R")
        composeTestRule.waitForIdle()

        assertEquals("DJASSER", viewModel.searchQuery)
        inputNode.assertTextContains("DJASSER")
    }
}
