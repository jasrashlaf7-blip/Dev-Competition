package com.olympiades.firststep.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editDraft = viewModel.editDraft
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(strings) {
        viewModel.savedEvents.collect {
            snackbarHostState.showSnackbar(strings.profileSavedSnackbar)
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
                        text = strings.profileTitle,
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Student Profile Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val fullName = uiState.profile?.fullName ?: "Amina Saidi"
                        val initials = fullName
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }
                            .ifBlank { "AS" }

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!editDraft.isEditing) {
                            val profile = uiState.profile
                            Text(
                                text = profile?.fullName ?: "Amina Saidi",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = listOfNotNull(
                                    profile?.degreeLevel?.takeIf { it.isNotBlank() },
                                    profile?.university?.takeIf { it.isNotBlank() }
                                ).joinToString(" – "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProfileDetailLine(
                                    icon = Icons.Outlined.Email,
                                    text = profile?.email.orEmpty()
                                )
                                ProfileDetailLine(
                                    icon = Icons.Outlined.Phone,
                                    text = profile?.phone.orEmpty()
                                )
                                if (!profile?.bio.isNullOrBlank()) {
                                    ProfileDetailLine(
                                        icon = Icons.Outlined.School,
                                        text = profile?.bio.orEmpty()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedButton(
                                onClick = viewModel::startEditing,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(text = strings.editProfileButton)
                            }
                        } else {
                            val nameErr = strings.validationMessage(editDraft.validation.nameError)
                            OutlinedTextField(
                                value = editDraft.fullName,
                                onValueChange = viewModel::onFullNameChange,
                                label = { Text(strings.fullNameLabel) },
                                isError = nameErr != null,
                                supportingText = nameErr?.let { { Text(it) } },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            val degreeErr = strings.validationMessage(editDraft.validation.degreeError)
                            OutlinedTextField(
                                value = editDraft.degreeLevel,
                                onValueChange = viewModel::onDegreeLevelChange,
                                label = { Text(strings.degreeLevelLabel) },
                                isError = degreeErr != null,
                                supportingText = degreeErr?.let { { Text(it) } },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_degree_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            val univErr = strings.validationMessage(editDraft.validation.universityError)
                            OutlinedTextField(
                                value = editDraft.university,
                                onValueChange = viewModel::onUniversityChange,
                                label = { Text(strings.universityLabel) },
                                isError = univErr != null,
                                supportingText = univErr?.let { { Text(it) } },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_university_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            val emailErr = strings.validationMessage(editDraft.validation.emailError)
                            OutlinedTextField(
                                value = editDraft.email,
                                onValueChange = viewModel::onEmailChange,
                                label = { Text(strings.emailLabel) },
                                isError = emailErr != null,
                                supportingText = emailErr?.let { { Text(it) } },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_email_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            val phoneErr = strings.validationMessage(editDraft.validation.phoneError)
                            OutlinedTextField(
                                value = editDraft.phone,
                                onValueChange = viewModel::onPhoneChange,
                                label = { Text(strings.phoneLabel) },
                                isError = phoneErr != null,
                                supportingText = phoneErr?.let { { Text(it) } },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_phone_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = editDraft.bio,
                                onValueChange = viewModel::onBioChange,
                                label = { Text(strings.bioLabel) },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_bio_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = viewModel::cancelEditing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cancel_profile_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = strings.cancelAction)
                                }
                                Button(
                                    onClick = viewModel::saveProfile,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_profile_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = strings.saveProfileButton)
                                }
                            }
                        }
                    }
                }

                // Settings Card (Language FR | AR + Theme Clair | Sombre)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.settingsSectionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strings.languageSettingLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = uiState.language == AppLanguage.FR,
                                    onClick = { viewModel.setLanguage(AppLanguage.FR) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    modifier = Modifier.testTag("lang_fr_button")
                                ) {
                                    Text(text = "FR", fontWeight = FontWeight.SemiBold)
                                }
                                SegmentedButton(
                                    selected = uiState.language == AppLanguage.AR,
                                    onClick = { viewModel.setLanguage(AppLanguage.AR) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    modifier = Modifier.testTag("lang_ar_button")
                                ) {
                                    Text(text = "AR", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.themeMode == AppThemeMode.DARK) {
                                        Icons.Outlined.DarkMode
                                    } else {
                                        Icons.Outlined.LightMode
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strings.themeSettingLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = uiState.themeMode == AppThemeMode.LIGHT,
                                    onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    modifier = Modifier.testTag("theme_light_button")
                                ) {
                                    Text(text = strings.themeLightLabel)
                                }
                                SegmentedButton(
                                    selected = uiState.themeMode == AppThemeMode.DARK,
                                    onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    modifier = Modifier.testTag("theme_dark_button")
                                ) {
                                    Text(text = strings.themeDarkLabel)
                                }
                            }
                        }
                    }
                }

                // About Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.firststep_logo),
                            contentDescription = "FIRSTSTEP Logo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = strings.aboutSectionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.aboutVersionLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.aboutEventLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = strings.aboutOfflineLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
