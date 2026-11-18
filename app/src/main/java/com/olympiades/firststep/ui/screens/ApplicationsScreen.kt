package com.olympiades.firststep.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.ApplicationStatus
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.DateTimeHelper
import com.olympiades.firststep.domain.validation.ApplicationStateMachine
import com.olympiades.firststep.ui.components.ApplicationStatusBadge
import com.olympiades.firststep.ui.components.EmptyStateView
import com.olympiades.firststep.ui.components.ErrorStateView
import com.olympiades.firststep.ui.components.FirstStepFilterChip
import com.olympiades.firststep.ui.components.LoadingStateView
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.theme.badgeColor
import com.olympiades.firststep.ui.viewmodel.ApplicationsUiState
import com.olympiades.firststep.ui.viewmodel.ApplicationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    viewModel: ApplicationsViewModel,
    onOfferClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val noteDrafts by viewModel.noteDrafts.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(strings) {
        viewModel.statusUpdatedEvents.collect {
            snackbarHostState.showSnackbar(strings.statusUpdatedSnackbar)
        }
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

                    if (state.applications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                icon = Icons.Outlined.Assignment,
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
                                        isExpanded = expanded,
                                        noteDraft = noteDrafts[app.id].orEmpty(),
                                        onToggleExpand = { viewModel.toggleExpanded(app.id) },
                                        onOpenOffer = { onOfferClick(app.offer.id) },
                                        onNoteChange = { viewModel.updateNoteDraft(app.id, it) },
                                        onSelectNextStatus = { nextStatus ->
                                            viewModel.requestStatusTransition(app, nextStatus)
                                        }
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
    isExpanded: Boolean,
    noteDraft: String,
    onToggleExpand: () -> Unit,
    onOpenOffer: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSelectNextStatus: (ApplicationStatus) -> Unit
) {
    val strings = LocalAppStrings.current
    val isArabic = strings.language == AppLanguage.AR
    val offerTitle = if (isArabic) application.offer.titleAr else application.offer.titleFr
    val allowedTransitions = ApplicationStateMachine.allowedNextStatuses(application.currentStatus)

    Card(
        modifier = Modifier
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
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    ApplicationStatusBadge(status = application.currentStatus)
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
