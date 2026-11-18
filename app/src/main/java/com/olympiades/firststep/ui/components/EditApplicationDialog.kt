package com.olympiades.firststep.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.validation.ApplicationFormValidationResult
import com.olympiades.firststep.domain.validation.ApplicationValidator
import com.olympiades.firststep.ui.i18n.LocalAppStrings

@Composable
fun EditApplicationDialog(
    application: ApplicationWithDetails,
    onDismiss: () -> Unit,
    onSave: (
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        coverLetter: String,
        cvUri: String
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var fullName by remember(application.id) { mutableStateOf(application.applicantName) }
    var email by remember(application.id) { mutableStateOf(application.applicantEmail) }
    var phone by remember(application.id) { mutableStateOf(application.applicantPhone) }
    var coverLetter by remember(application.id) { mutableStateOf(application.coverLetter) }
    var cvUri by remember(application.id) { mutableStateOf(application.cvUri) }
    var validation by remember(application.id) { mutableStateOf(ApplicationFormValidationResult()) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            cvUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.editApplicationDialogTitle(application.reference),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        validation = validation.copy(nameError = null)
                    },
                    label = { Text(strings.fullNameLabel) },
                    singleLine = true,
                    isError = validation.nameError != null,
                    supportingText = validation.nameError?.let { err ->
                        { strings.validationMessage(err)?.let { Text(it) } }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_app_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        validation = validation.copy(emailError = null)
                    },
                    label = { Text(strings.emailLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = validation.emailError != null,
                    supportingText = validation.emailError?.let { err ->
                        { strings.validationMessage(err)?.let { Text(it) } }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_app_email_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        validation = validation.copy(phoneError = null)
                    },
                    label = { Text(strings.phoneLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = validation.phoneError != null,
                    supportingText = validation.phoneError?.let { err ->
                        { strings.validationMessage(err)?.let { Text(it) } }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_app_phone_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = coverLetter,
                    onValueChange = {
                        coverLetter = it
                        validation = validation.copy(coverLetterError = null)
                    },
                    label = { Text(strings.coverLetterLabel) },
                    minLines = 3,
                    maxLines = 5,
                    isError = validation.coverLetterError != null,
                    supportingText = validation.coverLetterError?.let { err ->
                        { strings.validationMessage(err)?.let { Text(it) } }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_app_cover_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedButton(
                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_app_cv_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (cvUri.isNotBlank()) {
                            strings.cvAttachedLabel(cvUri.substringAfterLast('/'))
                        } else {
                            strings.attachCvButton
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = ApplicationValidator.validateApplicationForm(
                        fullName = fullName,
                        email = email,
                        phone = phone,
                        coverLetter = coverLetter
                    )
                    if (!result.isValid) {
                        validation = result
                    } else {
                        onSave(fullName, email, phone, coverLetter, cvUri)
                    }
                },
                modifier = Modifier.testTag("edit_app_save_button")
            ) {
                Text(strings.saveChangesButton)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(strings.cancelAction)
                }
            }
        }
    )
}
