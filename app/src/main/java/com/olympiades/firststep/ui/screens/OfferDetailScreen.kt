package com.olympiades.firststep.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.DateTimeHelper
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.ui.components.ApplicationStatusBadge
import com.olympiades.firststep.ui.components.EmptyStateView
import com.olympiades.firststep.ui.components.ErrorStateView
import com.olympiades.firststep.ui.components.LoadingStateView
import com.olympiades.firststep.ui.components.OfferEditorDialog
import com.olympiades.firststep.ui.components.StatusBadge
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.theme.FavoriteHeartColor
import com.olympiades.firststep.ui.theme.FirstStepPrimary
import com.olympiades.firststep.ui.theme.FirstStepSecondary
import com.olympiades.firststep.ui.theme.OfferClosedColor
import com.olympiades.firststep.ui.viewmodel.OfferDetailEvent
import com.olympiades.firststep.ui.viewmodel.OfferDetailUiState
import com.olympiades.firststep.ui.viewmodel.OfferDetailViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OfferDetailScreen(
    viewModel: OfferDetailViewModel,
    onNavigateBack: () -> Unit,
    onApplyClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayIso = remember { DateTimeHelper.todayIso() }
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditOfferDialog by remember { mutableStateOf(false) }

    LaunchedEffect(strings) {
        viewModel.events.collect { event ->
            when (event) {
                OfferDetailEvent.OfferUpdated -> {
                    snackbarHostState.showSnackbar(strings.offerUpdatedSnackbar)
                }
                OfferDetailEvent.OfferDeleted -> {
                    onNavigateBack()
                }
            }
        }
    }

    val successState = uiState as? OfferDetailUiState.Success
    if (showEditOfferDialog && successState != null) {
        OfferEditorDialog(
            existingOffer = successState.offer,
            companies = successState.companies,
            domains = successState.domains,
            locations = successState.locations,
            onDismiss = { showEditOfferDialog = false },
            onSave = { companyId, domainId, locationId, titleFr, titleAr, descFr, descAr, months, isPaid, stipend, startDate, status, deadline ->
                viewModel.updateOffer(
                    companyId = companyId,
                    domainId = domainId,
                    locationId = locationId,
                    titleFr = titleFr,
                    titleAr = titleAr,
                    descriptionFr = descFr,
                    descriptionAr = descAr,
                    durationMonths = months,
                    isPaid = isPaid,
                    monthlyStipendDzd = stipend,
                    startDate = startDate,
                    status = status,
                    deadline = deadline
                )
                showEditOfferDialog = false
            },
            onDelete = {
                showEditOfferDialog = false
                viewModel.deleteOffer()
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val isFavorite = (uiState as? OfferDetailUiState.Success)?.offer?.isFavorite == true
            TopAppBar(
                title = {
                    Text(
                        text = strings.offerDetailTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.navigateBackCd,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (uiState is OfferDetailUiState.Success) {
                        val detailHeartScale = remember { Animatable(1f) }
                        var prevDetailFavorite by remember { mutableStateOf(isFavorite) }
                        LaunchedEffect(isFavorite) {
                            if (isFavorite != prevDetailFavorite) {
                                prevDetailFavorite = isFavorite
                                detailHeartScale.snapTo(if (isFavorite) 1.32f else 0.85f)
                                detailHeartScale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                        val detailHeartTint by animateColorAsState(
                            targetValue = if (isFavorite) FavoriteHeartColor else Color.White,
                            animationSpec = tween(180),
                            label = "detail_heart_tint"
                        )
                        IconButton(
                            onClick = { showEditOfferDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_edit_offer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = strings.editOfferButtonCd,
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = viewModel::toggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = if (isFavorite) {
                                    strings.removeFromFavoritesCd
                                } else {
                                    strings.addToFavoritesCd
                                },
                                tint = detailHeartTint,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = detailHeartScale.value
                                    scaleY = detailHeartScale.value
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            val offer = (uiState as? OfferDetailUiState.Success)?.offer
            if (offer != null) {
                val canApply = offer.canApply(todayIso)
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onApplyClick(offer.id) },
                            enabled = canApply,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .heightIn(min = 50.dp)
                                .testTag("detail_apply_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        ) {
                            Text(
                                text = strings.applyButtonLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is OfferDetailUiState.Loading -> {
                LoadingStateView(
                    message = strings.loadingLabel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is OfferDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = strings.noResultsTitle,
                        subtitle = strings.errorLoadingTitle
                    )
                }
            }

            is OfferDetailUiState.Error -> {
                ErrorStateView(
                    title = strings.errorLoadingTitle,
                    retryLabel = strings.retryLabel,
                    onRetry = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is OfferDetailUiState.Success -> {
                val offer = state.offer
                val isArabic = strings.language == AppLanguage.AR
                val title = if (isArabic) offer.titleAr else offer.titleFr
                val description = if (isArabic) offer.descriptionAr else offer.descriptionFr
                val domainName = if (isArabic) offer.domain.nameAr else offer.domain.nameFr
                val cityName = if (isArabic) offer.location.cityAr else offer.location.cityFr
                val regionName = if (isArabic) offer.location.regionAr else offer.location.regionFr
                val companySector = if (isArabic) offer.company.sectorAr else offer.company.sectorFr
                val companyDesc = if (isArabic) offer.company.descriptionAr else offer.company.descriptionFr
                val isClosed = offer.status == OfferStatus.CLOSED
                val isExpired = offer.isExpired(todayIso)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header card
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${offer.company.name} · ${strings.refPrefix(offer.referenceCode)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Badges row
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(
                                text = domainName,
                                color = FirstStepPrimary
                            )
                            StatusBadge(
                                text = cityName,
                                color = Color(0xFF475569)
                            )
                            StatusBadge(
                                text = strings.durationMonths(offer.durationMonths),
                                color = Color(0xFF0284C7)
                            )
                            StatusBadge(
                                text = if (offer.isPaid) {
                                    if (offer.monthlyStipendDzd > 0) {
                                        strings.stipendPerMonth(offer.monthlyStipendDzd)
                                    } else {
                                        strings.paidBadge
                                    }
                                } else {
                                    strings.unpaidBadge
                                },
                                color = if (offer.isPaid) FirstStepSecondary else Color(0xFF64748B)
                            )
                            if (isClosed) {
                                StatusBadge(
                                    text = strings.closedStatusLabel,
                                    color = OfferClosedColor
                                )
                            } else if (isExpired) {
                                StatusBadge(
                                    text = strings.expiredLabel,
                                    color = OfferClosedColor
                                )
                            }
                            if (offer.applicationStatus != null) {
                                ApplicationStatusBadge(status = offer.applicationStatus)
                            }
                        }

                        // Status notice banners
                        AnimatedVisibility(
                            visible = isClosed || isExpired || offer.applicationReference != null,
                            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            when {
                                isClosed -> {
                                    NoticeBanner(
                                        icon = Icons.Outlined.EventBusy,
                                        text = strings.closedOfferBanner,
                                        containerColor = OfferClosedColor.copy(alpha = 0.12f),
                                        contentColor = OfferClosedColor
                                    )
                                }
                                isExpired -> {
                                    NoticeBanner(
                                        icon = Icons.Outlined.EventBusy,
                                        text = strings.expiredOfferBanner,
                                        containerColor = OfferClosedColor.copy(alpha = 0.12f),
                                        contentColor = OfferClosedColor
                                    )
                                }
                                offer.applicationReference != null -> {
                                    NoticeBanner(
                                        icon = Icons.Outlined.CheckCircle,
                                        text = strings.alreadyAppliedBanner(offer.applicationReference),
                                        containerColor = FirstStepSecondary.copy(alpha = 0.12f),
                                        contentColor = FirstStepSecondary
                                    )
                                }
                            }
                        }

                        // Description card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = strings.descriptionTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    MetaInfoRow(
                                        icon = Icons.Outlined.LocationOn,
                                        text = if (regionName.isNotBlank()) "$cityName ($regionName)" else cityName,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetaInfoRow(
                                        icon = Icons.Outlined.WorkOutline,
                                        text = strings.workModeOnsite,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (offer.startDate.isNotBlank()) {
                                    MetaInfoRow(
                                        icon = Icons.Outlined.EventAvailable,
                                        text = strings.startDatePrefix(DateTimeHelper.formatShortDate(offer.startDate))
                                    )
                                }
                                MetaInfoRow(
                                    icon = Icons.Outlined.Schedule,
                                    text = strings.deadlinePrefix(DateTimeHelper.formatShortDate(offer.deadline))
                                )
                            }
                        }

                        // Skills card
                        if (offer.skills.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = strings.requiredSkillsTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        offer.skills.forEach { skill ->
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        text = if (isArabic) skill.nameAr else skill.nameFr,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Company card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Business,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = strings.companySectionTitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = offer.company.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (companySector.isNotBlank()) {
                                    Text(
                                        text = companySector,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (companyDesc.isNotBlank()) {
                                    Text(
                                        text = companyDesc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NoticeBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor
            )
        }
    }
}
