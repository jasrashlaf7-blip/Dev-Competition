package com.olympiades.firststep.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.ui.components.LoadingStateView
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.theme.FirstStepSecondary
import com.olympiades.firststep.ui.viewmodel.ApplicationFormEvent
import com.olympiades.firststep.ui.viewmodel.ApplicationFormViewModel
import com.olympiades.firststep.ui.viewmodel.SubmitFormError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationFormScreen(
    viewModel: ApplicationFormViewModel,
    onNavigateBack: () -> Unit,
    onSubmittedSuccess: (String) -> Unit,
    onHelpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uiState = viewModel.uiState
    val context = LocalContext.current

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveFileName(context, uri)
            viewModel.onCvSelected(uri.toString(), fileName)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ApplicationFormEvent.Submitted -> onSubmittedSuccess(event.reference)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.applicationFormTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.navigateBackCd,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onHelpClick,
                        modifier = Modifier.testTag("form_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = strings.helpButtonCd,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
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
                        onClick = { viewModel.submit(strings.initialHistoryNote) },
                        enabled = !uiState.isLoading && !uiState.isSubmitting && uiState.offer != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .heightIn(min = 50.dp)
                            .testTag("submit_application_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = strings.submitApplicationButton,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingStateView(
                message = strings.loadingLabel,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val isArabic = strings.language == AppLanguage.AR
            val offer = uiState.offer
            val offerTitle = offer?.let { if (isArabic) it.titleAr else it.titleFr }.orEmpty()

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
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (offer != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = offerTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = offer.company.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = strings.appReferencePreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (uiState.submitError != null) {
                        val errorText = when (uiState.submitError) {
                            SubmitFormError.CLOSED_OR_EXPIRED -> strings.submitErrorClosed
                            SubmitFormError.ALREADY_APPLIED -> strings.submitErrorAlreadyApplied
                            SubmitFormError.GENERIC, null -> strings.submitErrorGeneric
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = errorText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    val nameError = strings.validationMessage(uiState.validation.nameError)
                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = viewModel::onFullNameChange,
                        label = { Text(strings.fullNameLabel) },
                        isError = nameError != null,
                        supportingText = nameError?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val emailError = strings.validationMessage(uiState.validation.emailError)
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text(strings.emailLabel) },
                        isError = emailError != null,
                        supportingText = emailError?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val phoneError = strings.validationMessage(uiState.validation.phoneError)
                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = { Text(strings.phoneLabel) },
                        isError = phoneError != null,
                        supportingText = phoneError?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val coverError = strings.validationMessage(uiState.validation.coverLetterError)
                    OutlinedTextField(
                        value = uiState.coverLetter,
                        onValueChange = viewModel::onCoverLetterChange,
                        label = { Text(strings.coverLetterLabel) },
                        placeholder = { Text(strings.coverLetterPlaceholder) },
                        isError = coverError != null,
                        supportingText = coverError?.let { msg -> { Text(msg) } },
                        minLines = 4,
                        maxLines = 7,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_cover_letter_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("form_attach_cv_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.cvUri.isNotBlank()) {
                                Icons.Outlined.CheckCircle
                            } else {
                                Icons.Outlined.AttachFile
                            },
                            contentDescription = null,
                            tint = if (uiState.cvUri.isNotBlank()) {
                                FirstStepSecondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (uiState.cvDisplayName.isNotBlank()) {
                                strings.cvAttachedLabel(uiState.cvDisplayName)
                            } else {
                                strings.attachCvButton
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

private fun resolveFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        result = cursor.getString(idx)
                    }
                }
            }
        }
    }
    return result ?: uri.lastPathSegment?.substringAfterLast('/') ?: "cv_candidat.pdf"
}
