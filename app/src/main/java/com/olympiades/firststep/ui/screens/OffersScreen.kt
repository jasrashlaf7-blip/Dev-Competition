package com.olympiades.firststep.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.DateTimeHelper
import com.olympiades.firststep.domain.model.OfferSortOption
import com.olympiades.firststep.ui.components.EmptyStateView
import com.olympiades.firststep.ui.components.ErrorStateView
import com.olympiades.firststep.ui.components.FirstStepFilterChip
import com.olympiades.firststep.ui.components.LoadingStateView
import com.olympiades.firststep.ui.components.OfferCard
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.viewmodel.OffersUiState
import com.olympiades.firststep.ui.viewmodel.OffersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    viewModel: OffersViewModel,
    onOfferClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery = viewModel.searchQuery
    val todayIso = remember { DateTimeHelper.todayIso() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.appTitleOffers,
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
            is OffersUiState.Loading -> {
                LoadingStateView(
                    message = strings.loadingLabel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is OffersUiState.Error -> {
                ErrorStateView(
                    title = strings.errorLoadingTitle,
                    retryLabel = strings.retryLabel,
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is OffersUiState.Success -> {
                val isArabic = strings.language == AppLanguage.AR
                val criteria = state.criteria
                var domainMenuExpanded by remember { mutableStateOf(false) }
                var locationMenuExpanded by remember { mutableStateOf(false) }
                var sortMenuExpanded by remember { mutableStateOf(false) }

                val selectedDomain = state.domains.find { it.id == criteria.domainId }
                val selectedLocation = state.locations.find { it.id == criteria.locationId }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("offers_search_input"),
                        placeholder = {
                            Text(
                                text = strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.updateQuery("") },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = strings.clearSearchCd
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Combinable filter chips row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FirstStepFilterChip(
                            label = strings.filterAll,
                            selected = !criteria.paidOnly &&
                                !criteria.openOnly &&
                                criteria.domainId == null &&
                                criteria.locationId == null &&
                                searchQuery.isBlank(),
                            onClick = viewModel::resetToAll,
                            testTag = "filter_chip_all"
                        )

                        FirstStepFilterChip(
                            label = strings.filterPaid,
                            selected = criteria.paidOnly,
                            onClick = viewModel::togglePaidOnly,
                            testTag = "filter_chip_paid"
                        )

                        FirstStepFilterChip(
                            label = strings.filterOpen,
                            selected = criteria.openOnly,
                            onClick = viewModel::toggleOpenOnly,
                            testTag = "filter_chip_open"
                        )

                        Box {
                            val domainChipLabel = selectedDomain?.let {
                                if (isArabic) it.nameAr else it.nameFr
                            } ?: strings.filterDomainLabel
                            FirstStepFilterChip(
                                label = domainChipLabel,
                                selected = criteria.domainId != null,
                                onClick = { domainMenuExpanded = true },
                                trailingIcon = Icons.Filled.ArrowDropDown,
                                testTag = "filter_chip_domain"
                            )
                            DropdownMenu(
                                expanded = domainMenuExpanded,
                                onDismissRequest = { domainMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.filterAllDomains) },
                                    onClick = {
                                        viewModel.selectDomain(null)
                                        domainMenuExpanded = false
                                    }
                                )
                                state.domains.forEach { domain ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (isArabic) domain.nameAr else domain.nameFr)
                                        },
                                        onClick = {
                                            viewModel.selectDomain(domain.id)
                                            domainMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Box {
                            val locationChipLabel = selectedLocation?.let {
                                if (isArabic) it.cityAr else it.cityFr
                            } ?: strings.filterLocationLabel
                            FirstStepFilterChip(
                                label = locationChipLabel,
                                selected = criteria.locationId != null,
                                onClick = { locationMenuExpanded = true },
                                trailingIcon = Icons.Filled.ArrowDropDown,
                                testTag = "filter_chip_location"
                            )
                            DropdownMenu(
                                expanded = locationMenuExpanded,
                                onDismissRequest = { locationMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.filterAllLocations) },
                                    onClick = {
                                        viewModel.selectLocation(null)
                                        locationMenuExpanded = false
                                    }
                                )
                                state.locations.forEach { location ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (isArabic) location.cityAr else location.cityFr)
                                        },
                                        onClick = {
                                            viewModel.selectLocation(location.id)
                                            locationMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Results counter + sorting + reset filters bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.resultsCount(state.offers.size),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("offers_results_count")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (criteria.hasActiveFilters) {
                                TextButton(
                                    onClick = viewModel::resetAllFilters,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("reset_filters_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = strings.resetFilters,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            Box {
                                TextButton(
                                    onClick = { sortMenuExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("sort_offers_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = when (criteria.sortOption) {
                                            OfferSortOption.NEWEST -> strings.sortNewest
                                            OfferSortOption.CLOSEST_DEADLINE -> strings.sortClosestDeadline
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.sortNewest) },
                                        onClick = {
                                            viewModel.selectSortOption(OfferSortOption.NEWEST)
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.sortClosestDeadline) },
                                        onClick = {
                                            viewModel.selectSortOption(OfferSortOption.CLOSEST_DEADLINE)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Offers list or empty state
                    if (state.offers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                icon = Icons.Outlined.SearchOff,
                                title = strings.noResultsTitle,
                                subtitle = strings.noResultsSubtitle,
                                actionLabel = if (criteria.hasActiveFilters) strings.resetFilters else null,
                                onAction = if (criteria.hasActiveFilters) viewModel::resetAllFilters else null
                            )
                        }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val useTwoColumns = maxWidth >= 600.dp
                            if (useTwoColumns) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 4.dp,
                                        bottom = 16.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.offers, key = { it.id }) { offer ->
                                        OfferCard(
                                            offer = offer,
                                            todayIso = todayIso,
                                            onClick = { onOfferClick(offer.id) },
                                            onFavoriteClick = { viewModel.toggleFavorite(offer.id) }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 4.dp,
                                        bottom = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 600.dp)
                                ) {
                                    items(state.offers, key = { it.id }) { offer ->
                                        OfferCard(
                                            offer = offer,
                                            todayIso = todayIso,
                                            onClick = { onOfferClick(offer.id) },
                                            onFavoriteClick = { viewModel.toggleFavorite(offer.id) }
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
}
