package com.olympiades.firststep.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.DateTimeHelper
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.theme.FavoriteHeartColor
import com.olympiades.firststep.ui.theme.FirstStepPrimary
import com.olympiades.firststep.ui.theme.FirstStepSecondary
import com.olympiades.firststep.ui.theme.OfferClosedColor

/**
 * OfferCard adhering strictly to Graphic Charter:
 * 12dp corners, 2dp elevation, left (start) accent bar, favorite heart.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfferCard(
    offer: Offer,
    todayIso: String,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isArabic = strings.language == AppLanguage.AR

    val title = if (isArabic) offer.titleAr else offer.titleFr
    val domainName = if (isArabic) offer.domain.nameAr else offer.domain.nameFr
    val cityName = if (isArabic) offer.location.cityAr else offer.location.cityFr
    val isClosed = offer.status == OfferStatus.CLOSED
    val isExpired = offer.isExpired(todayIso)

    val accentColor = when {
        isClosed || isExpired -> OfferClosedColor
        offer.isPaid -> FirstStepSecondary
        else -> FirstStepPrimary
    }

    val heartScale = remember { Animatable(1f) }
    var previousFavorite by remember { mutableStateOf(offer.isFavorite) }
    LaunchedEffect(offer.isFavorite) {
        if (offer.isFavorite != previousFavorite) {
            previousFavorite = offer.isFavorite
            heartScale.snapTo(if (offer.isFavorite) 1.3f else 0.85f)
            heartScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    val heartTint by animateColorAsState(
        targetValue = if (offer.isFavorite) {
            FavoriteHeartColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(180),
        label = "offer_heart_tint"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            .clickable(onClick = onClick)
            .testTag("offer_card_${offer.id}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${offer.company.name} · $domainName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("offer_favorite_button_${offer.id}")
                    ) {
                        Icon(
                            imageVector = if (offer.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = if (offer.isFavorite) {
                                strings.removeFromFavoritesCd
                            } else {
                                strings.addToFavoritesCd
                            },
                            tint = heartTint,
                            modifier = Modifier.graphicsLayer {
                                scaleX = heartScale.value
                                scaleY = heartScale.value
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = cityName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = strings.durationMonths(offer.durationMonths),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = DateTimeHelper.formatShortDate(offer.deadline),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = if (isExpired) OfferClosedColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (offer.startDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = strings.startDatePrefix(DateTimeHelper.formatShortDate(offer.startDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (offer.isPaid) {
                        val paidText = if (offer.monthlyStipendDzd > 0) {
                            strings.stipendPerMonth(offer.monthlyStipendDzd)
                        } else {
                            strings.paidBadge
                        }
                        StatusBadge(
                            text = paidText,
                            color = FirstStepSecondary
                        )
                    } else {
                        StatusBadge(
                            text = strings.unpaidBadge,
                            color = Color(0xFF64748B)
                        )
                    }

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
            }
        }
    }
}
