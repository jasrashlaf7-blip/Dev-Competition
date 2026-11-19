package com.olympiades.firststep.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.domain.assistant.AssistantActionShortcut
import com.olympiades.firststep.domain.assistant.AssistantContext
import com.olympiades.firststep.domain.assistant.AssistantOfferSuggestion
import com.olympiades.firststep.domain.assistant.AssistantQuickQuestion
import com.olympiades.firststep.domain.assistant.OfflineAssistantEngine
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.StudentProfile
import com.olympiades.firststep.domain.repository.FirstStepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AssistantChatMessage(
    val id: Long,
    val isFromUser: Boolean,
    val text: String,
    val suggestedOffers: List<AssistantOfferSuggestion> = emptyList(),
    val actionShortcut: AssistantActionShortcut? = null,
    val followUpQuestions: List<AssistantQuickQuestion> = emptyList()
)

private data class LiveAssistantSnapshot(
    val language: AppLanguage = AppLanguage.FR,
    val offers: List<Offer> = emptyList(),
    val favorites: List<FavoriteOffer> = emptyList(),
    val applications: List<ApplicationWithDetails> = emptyList(),
    val profile: StudentProfile? = null
)

class OfflineAssistantViewModel(
    repository: FirstStepRepository
) : ViewModel() {

    private var nextMessageId = 1L
    private val currentRouteFlow = MutableStateFlow<String?>(null)
    private var lastWelcomeLanguage: AppLanguage? = null

    var inputQuery by mutableStateOf("")
        private set

    private val snapshotState: StateFlow<LiveAssistantSnapshot> = combine(
        repository.observePreferences(),
        repository.observeFilteredOffers(OfferFilterCriteria()),
        repository.observeFavorites(),
        repository.observeApplications(null),
        repository.observeStudentProfile()
    ) { prefs, offers, favorites, applications, profile ->
        LiveAssistantSnapshot(
            language = prefs.language,
            offers = offers,
            favorites = favorites,
            applications = applications,
            profile = profile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LiveAssistantSnapshot()
    )

    val quickQuestions: StateFlow<List<AssistantQuickQuestion>> = combine(
        snapshotState,
        currentRouteFlow
    ) { snapshot, route ->
        OfflineAssistantEngine.quickQuestionsFor(buildContext(snapshot, route))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _messages = MutableStateFlow<List<AssistantChatMessage>>(emptyList())
    val messages: StateFlow<List<AssistantChatMessage>> = _messages.asStateFlow()

    fun onRouteChanged(route: String?) {
        currentRouteFlow.value = route
    }

    fun onAssistantOpened(route: String?) {
        currentRouteFlow.value = route
        val snapshot = snapshotState.value
        val current = _messages.value
        if (current.isEmpty() || (current.size == 1 && !current.first().isFromUser && lastWelcomeLanguage != snapshot.language)) {
            seedWelcome(snapshot, route)
        }
    }

    fun updateInputQuery(value: String) {
        inputQuery = value
    }

    fun askQuickQuestion(question: AssistantQuickQuestion) {
        val snapshot = snapshotState.value
        val context = buildContext(snapshot, currentRouteFlow.value)
        val userMsg = AssistantChatMessage(
            id = nextMessageId++,
            isFromUser = true,
            text = question.label
        )
        val response = OfflineAssistantEngine.answerTopic(question.topic, context)
        val botMsg = AssistantChatMessage(
            id = nextMessageId++,
            isFromUser = false,
            text = response.message,
            suggestedOffers = response.suggestedOffers,
            actionShortcut = response.actionShortcut,
            followUpQuestions = response.followUpQuestions
        )
        _messages.value = _messages.value + userMsg + botMsg
    }

    fun sendUserQuestion() {
        val trimmed = inputQuery.trim()
        if (trimmed.isEmpty()) return
        inputQuery = ""

        val snapshot = snapshotState.value
        val context = buildContext(snapshot, currentRouteFlow.value)
        val userMsg = AssistantChatMessage(
            id = nextMessageId++,
            isFromUser = true,
            text = trimmed
        )
        val response = OfflineAssistantEngine.answerQuestion(trimmed, context)
        val botMsg = AssistantChatMessage(
            id = nextMessageId++,
            isFromUser = false,
            text = response.message,
            suggestedOffers = response.suggestedOffers,
            actionShortcut = response.actionShortcut,
            followUpQuestions = response.followUpQuestions
        )
        _messages.value = _messages.value + userMsg + botMsg
    }

    fun resetConversation() {
        seedWelcome(snapshotState.value, currentRouteFlow.value)
    }

    private fun seedWelcome(snapshot: LiveAssistantSnapshot, route: String?) {
        lastWelcomeLanguage = snapshot.language
        val context = buildContext(snapshot, route)
        val welcome = OfflineAssistantEngine.welcomeMessage(context)
        _messages.value = listOf(
            AssistantChatMessage(
                id = nextMessageId++,
                isFromUser = false,
                text = welcome.message,
                suggestedOffers = welcome.suggestedOffers,
                actionShortcut = welcome.actionShortcut,
                followUpQuestions = welcome.followUpQuestions
            )
        )
    }

    private fun buildContext(
        snapshot: LiveAssistantSnapshot,
        route: String?
    ): AssistantContext = AssistantContext(
        language = snapshot.language,
        currentRoute = route,
        offers = snapshot.offers,
        favorites = snapshot.favorites,
        applications = snapshot.applications,
        profile = snapshot.profile
    )
}
