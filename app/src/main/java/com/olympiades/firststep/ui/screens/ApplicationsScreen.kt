package com.olympiades.firststep.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.DateTimeHelper
import com.olympiades.firststep.domain.validation.ApplicationStateMachine
import com.olympiades.firststep.ui.components.ApplicationStatusBadge
import com.olympiades.firststep.ui.components.EditApplicationDialog
import com.olympiades.firststep.ui.components.EmptyStateView
import com.olympiades.firststep.ui.components.ErrorStateView
import com.olympiades.firststep.ui.components.FirstStepFilterChip
import com.olympiades.firststep.ui.components.LoadingStateView
import com.olympiades.firststep.ui.components.StatusBadge
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.theme.badgeColor
import com.olympiades.firststep.ui.viewmodel.ApplicationsUiState
import com.olympiades.firststep.ui.viewmodel.ApplicationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    viewModel: ApplicationsViewModel,
    onOfferClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val todayIso = remember { DateTimeHelper.todayIso() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val noteDrafts = viewModel.noteDrafts
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingApplication by remember { mutableStateOf<ApplicationWithDetails?>(null) }

    LaunchedEffect(strings) {
        viewModel.statusUpdatedEvents.collect {
            snackbarHostState.showSnackbar(strings.statusUpdatedSnackbar)
        }
    }

    LaunchedEffect(strings) {
        viewModel.detailsUpdatedEvents.collect {
            snackbarHostState.showSnackbar(strings.applicationEditedSnackbar)
        }
    }

    editingApplication?.let { targetApp ->
        EditApplicationDialog(
            application = targetApp,
            onDismiss = { editingApplication = null },
            onSave = { name, email, phone, coverLetter, cvUri ->
                viewModel.saveEditedDetails(
                    applicationId = targetApp.id,
                    applicantName = name,
                    applicantEmail = email,
                    applicantPhone = phone,
                    coverLetter = coverLetter,
                    cvUri = cvUri,
                    editNote = strings.applicationEditedHistoryNote
                )
                editingApplication = null
            }
        )
    }

    if (pendingConfirmation != null) {
        val pending = pendingConfirmation!!
        val statusLabel = strings.statusLabel(pending.targetStatus)
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingTransition,
            title = { Text(text = strings.confirmFinalStatusTitle) },
            text = { Text(text = strings.confirmFinalStatusBody(statusLabel)) },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmPendingTransition,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pending.targetStatus.badgeColor()
                    ),
                    modifier = Modifier.testTag("confirm_final_status_button")
                ) {
                    Text(text = strings.confirmAction, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissPendingTransition,
                    modifier = Modifier.testTag("cancel_final_status_button")
                ) {
                    Text(text = strings.cancelAction)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.applicationsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val (count, csvContent) = viewModel.buildFilteredCsv(todayIso)
                                if (count == 0) {
                                    snackbarHostState.showSnackbar(strings.exportCsvEmptySnackbar)
                                    return@launch
                                }
                                runCatching {
                                    val exportFile = withContext(Dispatchers.IO) {
                                        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                                        val file = File(dir, "candidatures_firststep.csv")
                                        file.writeText(csvContent, Charsets.UTF_8)
                                        file
                                    }
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        exportFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "FIRSTSTEP – Candidatures CSV")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, strings.exportCsvCd)
                                    )
                                }.onSuccess {
                                    snackbarHostState.showSnackbar(strings.exportCsvSuccessSnackbar(count))
                                }.onFailure {
                                    snackbarHostState.showSnackbar(strings.exportCsvErrorSnackbar)
                                }
                            }
                        },
                        modifier = Modifier.testTag("export_applications_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = strings.exportCsvCd,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is ApplicationsUiState.Loading -> {
                LoadingStateView(
                    message = strings.loadingLabel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is ApplicationsUiState.Error -> {
                ErrorStateView(
                    title = strings.errorLoadingTitle,
                    retryLabel = strings.retryLabel,
                    onRetry = { viewModel.selectFilter(null) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is ApplicationsUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Status filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FirstStepFilterChip(
                            label = strings.allStatusesFilter,
                            selected = state.selectedFilter == null,
                            onClick = { viewModel.selectFilter(null) },
                            testTag = "app_filter_all"
                        )
                        ApplicationStatus.entries.forEach { status ->
                            FirstStepFilterChip(
                                label = strings.statusLabel(status),
                                selected = state.selectedFilter == status,
                                onClick = { viewModel.selectFilter(status) },
                                testTag = "app_filter_${status.name.lowercase()}"
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = state.unansweredOver7DaysCount > 0,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("unanswered_over_7_days_banner"),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF57C00).copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.unansweredAlertBanner(state.unansweredOver7DaysCount),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    if (state.applications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                icon = Icons.AutoMirrored.Outlined.Assignment,
                                title = strings.applicationsEmptyTitle,
                                subtitle = strings.applicationsEmptySubtitle
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 680.dp)
                            ) {
                                items(state.applications, key = { it.id }) { app ->
                                    val expanded = state.expandedApplicationId == app.id
                                    ApplicationTrackingCard(
                                        application = app,
                                        todayIso = todayIso,
                                        isExpanded = expanded,
                                        noteDraft = noteDrafts[app.id].orEmpty(),
                                        onToggleExpand = { viewModel.toggleExpanded(app.id) },
                                        onOpenOffer = { onOfferClick(app.offer.id) },
                                        onEditDetailsClick = { editingApplication = app },
                                        onNoteChange = { viewModel.updateNoteDraft(app.id, it) },
                                        onSelectNextStatus = { nextStatus ->
                                            viewModel.requestStatusTransition(app, nextStatus)
                                        },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(220),
                                            fadeOutSpec = tween(160),
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplicationTrackingCard(
    application: ApplicationWithDetails,
    todayIso: String,
    isExpanded: Boolean,
    noteDraft: String,
    onToggleExpand: () -> Unit,
    onOpenOffer: () -> Unit,
    onEditDetailsClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSelectNextStatus: (ApplicationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isArabic = strings.language == AppLanguage.AR
    val offerTitle = if (isArabic) application.offer.titleAr else application.offer.titleFr
    val allowedTransitions = ApplicationStateMachine.allowedNextStatuses(application.currentStatus)
    val isOverdue = application.isUnansweredOver7Days(todayIso)
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "app_card_chevron_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("application_card_${application.id}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card summary header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(onClick = onOpenOffer)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${application.reference} · ${application.offer.company.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOverdue) {
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusBadge(
                            text = strings.unansweredOver7DaysBadge(application.daysSinceApplied(todayIso)),
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    ApplicationStatusBadge(status = application.currentStatus)
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(chevronRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    // Full saved application details from SQLite
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.applicationDetailsTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!application.currentStatus.isFinal) {
                                    TextButton(
                                        onClick = onEditDetailsClick,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("edit_application_${application.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = strings.editApplicationButton,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            Text(
                                text = application.applicantName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${application.applicantEmail} · ${application.applicantPhone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${strings.appliedDateLabel}: ${application.appliedAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${strings.coverLetterLabel.removeSuffix(" *")}: ${application.coverLetter}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${strings.cvDocumentLabel}: ${
                                    if (application.cvUri.isNotBlank()) {
                                        application.cvUri.substringAfterLast('/')
                                    } else {
                                        strings.notProvidedLabel
                                    }
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    // Chronological status history timeline
                    Text(
                        text = strings.historySectionTitle(application.reference),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        application.history.forEachIndexed { index, item ->
                            val isLast = index == application.history.lastIndex
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(item.newStatus.badgeColor())
                                    )
                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(26.dp)
                                                .background(MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = DateTimeHelper.formatDayMonth(item.changedAt),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = strings.statusLabel(item.newStatus),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = item.newStatus.badgeColor()
                                        )
                                    }
                                    if (item.note.isNotBlank()) {
                                        Text(
                                            text = item.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    if (application.currentStatus.isFinal || allowedTransitions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = application.currentStatus.badgeColor().copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = application.currentStatus.badgeColor(),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.finalStateLockedMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = application.currentStatus.badgeColor()
                                )
                            }
                        }
                    } else {
                        Text(
                            text = strings.updateStatusTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = onNoteChange,
                            label = { Text(strings.personalNoteLabel) },
                            placeholder = { Text(strings.personalNotePlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_note_input_${application.id}"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allowedTransitions.forEach { nextStatus ->
                                val color = nextStatus.badgeColor()
                                OutlinedButton(
                                    onClick = { onSelectNextStatus(nextStatus) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = color
                                    ),
                                    modifier = Modifier.testTag(
                                        "transition_${application.id}_${nextStatus.name.lowercase()}"
                                    )
                                ) {
                                    Text(
                                        text = strings.statusLabel(nextStatus),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
