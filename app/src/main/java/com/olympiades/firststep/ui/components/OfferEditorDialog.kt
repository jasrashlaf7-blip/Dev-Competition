package com.olympiades.firststep.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.Company
import com.olympiades.firststep.domain.model.Domain
import com.olympiades.firststep.domain.model.Location
import com.olympiades.firststep.domain.model.Offer
import com.olympiades.firststep.domain.model.OfferStatus
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.theme.OfferClosedColor

private val ISO_DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

@Composable
fun OfferEditorDialog(
    existingOffer: Offer?,
    companies: List<Company>,
    domains: List<Domain>,
    locations: List<Location>,
    onDismiss: () -> Unit,
    onSave: (
        companyId: Long,
        domainId: Long,
        locationId: Long,
        titleFr: String,
        titleAr: String,
        descriptionFr: String,
        descriptionAr: String,
        durationMonths: Int,
        isPaid: Boolean,
        monthlyStipendDzd: Int,
        startDate: String,
        status: OfferStatus,
        deadline: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val isArabic = strings.language == AppLanguage.AR

    var selectedCompanyId by remember(existingOffer?.id) {
        mutableStateOf(existingOffer?.company?.id ?: companies.firstOrNull()?.id ?: 1L)
    }
    var selectedDomainId by remember(existingOffer?.id) {
        mutableStateOf(existingOffer?.domain?.id ?: domains.firstOrNull()?.id ?: 1L)
    }
    var selectedLocationId by remember(existingOffer?.id) {
        mutableStateOf(existingOffer?.location?.id ?: locations.firstOrNull()?.id ?: 1L)
    }
    var titleFr by remember(existingOffer?.id) { mutableStateOf(existingOffer?.titleFr.orEmpty()) }
    var titleAr by remember(existingOffer?.id) { mutableStateOf(existingOffer?.titleAr.orEmpty()) }
    var descFr by remember(existingOffer?.id) { mutableStateOf(existingOffer?.descriptionFr.orEmpty()) }
    var descAr by remember(existingOffer?.id) { mutableStateOf(existingOffer?.descriptionAr.orEmpty()) }
    var durationText by remember(existingOffer?.id) {
        mutableStateOf((existingOffer?.durationMonths ?: 3).toString())
    }
    var isPaid by remember(existingOffer?.id) { mutableStateOf(existingOffer?.isPaid ?: true) }
    var stipendText by remember(existingOffer?.id) {
        mutableStateOf((existingOffer?.monthlyStipendDzd ?: 25000).toString())
    }
    var startDate by remember(existingOffer?.id) {
        mutableStateOf(existingOffer?.startDate?.ifBlank { "2026-12-01" } ?: "2026-12-01")
    }
    var deadline by remember(existingOffer?.id) {
        mutableStateOf(existingOffer?.deadline ?: "2026-12-31")
    }
    var status by remember(existingOffer?.id) {
        mutableStateOf(existingOffer?.status ?: OfferStatus.OPEN)
    }
    var showError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var companyMenuOpen by remember { mutableStateOf(false) }
    var domainMenuOpen by remember { mutableStateOf(false) }
    var locationMenuOpen by remember { mutableStateOf(false) }

    val selectedCompany = companies.find { it.id == selectedCompanyId } ?: companies.firstOrNull()
    val selectedDomain = domains.find { it.id == selectedDomainId } ?: domains.firstOrNull()
    val selectedLocation = locations.find { it.id == selectedLocationId } ?: locations.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingOffer == null) strings.createOfferTitle else strings.editOfferTitle,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titleFr,
                    onValueChange = {
                        titleFr = it
                        showError = false
                    },
                    label = { Text(strings.offerTitleFrLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offer_editor_title_fr"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = titleAr,
                    onValueChange = { titleAr = it },
                    label = { Text(strings.offerTitleArLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offer_editor_title_ar"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = descFr,
                    onValueChange = {
                        descFr = it
                        showError = false
                    },
                    label = { Text(strings.offerDescFrLabel) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offer_editor_desc_fr"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = descAr,
                    onValueChange = { descAr = it },
                    label = { Text(strings.offerDescArLabel) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offer_editor_desc_ar"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Company selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { companyMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${strings.companyLabel}: ${selectedCompany?.name.orEmpty()}",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = companyMenuOpen,
                        onDismissRequest = { companyMenuOpen = false }
                    ) {
                        companies.forEach { company ->
                            DropdownMenuItem(
                                text = { Text(company.name) },
                                onClick = {
                                    selectedCompanyId = company.id
                                    companyMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Domain & Location selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { domainMenuOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = selectedDomain?.let {
                                    if (isArabic) it.nameAr else it.nameFr
                                } ?: strings.filterDomainLabel,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = domainMenuOpen,
                            onDismissRequest = { domainMenuOpen = false }
                        ) {
                            domains.forEach { domain ->
                                DropdownMenuItem(
                                    text = { Text(if (isArabic) domain.nameAr else domain.nameFr) },
                                    onClick = {
                                        selectedDomainId = domain.id
                                        domainMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { locationMenuOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = selectedLocation?.let {
                                    if (isArabic) it.cityAr else it.cityFr
                                } ?: strings.filterLocationLabel,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = locationMenuOpen,
                            onDismissRequest = { locationMenuOpen = false }
                        ) {
                            locations.forEach { location ->
                                DropdownMenuItem(
                                    text = { Text(if (isArabic) location.cityAr else location.cityFr) },
                                    onClick = {
                                        selectedLocationId = location.id
                                        locationMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Duration & Deadline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(strings.durationMonthsLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = deadline,
                        onValueChange = {
                            deadline = it
                            showError = false
                        },
                        label = { Text(strings.deadlineLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Paid toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.paidOfferToggleLabel,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isPaid,
                        onCheckedChange = { isPaid = it }
                    )
                }

                // Stipend (DZD) & Start Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = if (isPaid) stipendText else "0",
                        onValueChange = { stipendText = it.filter { ch -> ch.isDigit() } },
                        enabled = isPaid,
                        label = { Text(strings.monthlyStipendLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("offer_editor_stipend"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {
                            startDate = it
                            showError = false
                        },
                        label = { Text(strings.startDateLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("offer_editor_start_date"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Status chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.offerStatusLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = status == OfferStatus.OPEN,
                        onClick = { status = OfferStatus.OPEN },
                        label = { Text(strings.openStatusLabel) }
                    )
                    FilterChip(
                        selected = status == OfferStatus.CLOSED,
                        onClick = { status = OfferStatus.CLOSED },
                        label = { Text(strings.closedStatusLabel) }
                    )
                }

                if (showError) {
                    Text(
                        text = strings.offerFieldsError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (existingOffer != null && onDelete != null) {
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OfferClosedColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offer_editor_delete_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null
                        )
                        Text(
                            text = " ${strings.deleteOfferButton}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val months = durationText.toIntOrNull() ?: 0
                    val stipend = if (isPaid) (stipendText.toIntOrNull() ?: 0) else 0
                    val trimmedStartDate = startDate.trim()
                    val trimmedDeadline = deadline.trim()
                    if (titleFr.isBlank() ||
                        descFr.isBlank() ||
                        months !in 1..24 ||
                        !ISO_DATE_REGEX.matches(trimmedDeadline) ||
                        !ISO_DATE_REGEX.matches(trimmedStartDate)
                    ) {
                        showError = true
                    } else {
                        onSave(
                            selectedCompanyId,
                            selectedDomainId,
                            selectedLocationId,
                            titleFr,
                            titleAr,
                            descFr,
                            descAr,
                            months,
                            isPaid,
                            stipend,
                            trimmedStartDate,
                            status,
                            trimmedDeadline
                        )
                    }
                },
                modifier = Modifier.testTag("offer_editor_save_button")
            ) {
                Text(strings.saveChangesButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelAction)
            }
        }
    )

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.confirmDeleteOfferTitle) },
            text = { Text(strings.confirmDeleteOfferBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OfferClosedColor)
                ) {
                    Text(strings.confirmAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }
}
