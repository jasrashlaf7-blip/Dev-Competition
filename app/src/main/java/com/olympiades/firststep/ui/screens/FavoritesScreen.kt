package com.olympiades.firststep.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.FavoriteOffer
import com.olympiades.firststep.ui.components.ApplicationStatusBadge
import com.olympiades.firststep.ui.components.EmptyStateView
import com.olympiades.firststep.ui.components.ErrorStateView
import com.olympiades.firststep.ui.components.LoadingStateView
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.theme.FavoriteHeartColor
import com.olympiades.firststep.ui.viewmodel.FavoritesUiState
import com.olympiades.firststep.ui.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOfferClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(strings) {
        viewModel.removedOfferEvents.collect { removedOfferId ->
            val result = snackbarHostState.showSnackbar(
                message = strings.removedFavoriteSnackbar,
                actionLabel = strings.undoAction,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoRemove(removedOfferId)
            }
        }
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
                        text = strings.favoritesTitle,
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
            is FavoritesUiState.Loading -> {
                LoadingStateView(
                    message = strings.loadingLabel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is FavoritesUiState.Error -> {
                ErrorStateView(
                    title = strings.errorLoadingTitle,
                    retryLabel = strings.retryLabel,
                    onRetry = {},
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is FavoritesUiState.Success -> {
                if (state.favorites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Outlined.FavoriteBorder,
                            title = strings.favoritesEmptyTitle,
                            subtitle = strings.favoritesEmptySubtitle
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                        ) {
                            item {
                                Text(
                                    text = strings.favoritesCounter(state.favorites.size),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("favorites_counter")
                                )
                            }

                            items(state.favorites, key = { it.offer.id }) { item ->
                                FavoriteItemCard(
                                    item = item,
                                    onClick = { onOfferClick(item.offer.id) },
                                    onRemoveClick = { viewModel.removeFavorite(item.offer.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteItemCard(
    item: FavoriteOffer,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isArabic = strings.language == AppLanguage.AR
    val offer = item.offer
    val title = if (isArabic) offer.titleAr else offer.titleFr
    val domainName = if (isArabic) offer.domain.nameAr else offer.domain.nameFr
    val cityName = if (isArabic) offer.location.cityAr else offer.location.cityFr
    val statusText = offer.applicationStatus?.let { strings.statusLabel(it) }
        ?: strings.notAppliedStatus

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("favorite_card_${offer.id}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${offer.company.name} · $cityName · $domainName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${strings.durationMonths(offer.durationMonths)} · $statusText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))

                ApplicationStatusBadge(status = offer.applicationStatus)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRemoveClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("favorite_remove_button_${offer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = FavoriteHeartColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = strings.removeFavoriteAction,
                        color = FavoriteHeartColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("favorite_details_button_${offer.id}")
                ) {
                    Text(
                        text = strings.viewDetailsAction,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
