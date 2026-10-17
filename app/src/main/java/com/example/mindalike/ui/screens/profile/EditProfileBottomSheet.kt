package com.example.mindalike.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mindalike.data.Profile
import com.example.mindalike.data.ProfileUpdate
import com.example.mindalike.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID

private val LightBrown = Color(0xFF8C6D58)
private val LightBrownBorder = Color(0xFFE2D7CE)
private val CleanBackground = Color(0xFFFBF8F5)

private val POPULAR_BADGES = listOf(
    "Android Dev", "Kotlin", "Jetpack Compose", "UI/UX", "Flutter",
    "Python", "Machine Learning", "Data Science", "CFA Prep", "Finance",
    "Web3", "Graphic Design", "Backend", "Supabase", "Cloud Architecture",
    "System Design", "DevOps", "Cybersecurity"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    profile: Profile,
    onDismiss: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf(profile.fullName) }
    var bio by remember { mutableStateOf(profile.bio ?: "") }
    var degree by remember { mutableStateOf(profile.degree ?: "") }
    var universityName by remember { mutableStateOf(profile.universityName ?: "") }
    var location by remember { mutableStateOf(profile.location ?: "") }

    var selectedBadges by remember { mutableStateOf(profile.badges) }
    var badgeSearchInput by remember { mutableStateOf("") }

    var avatarUrl by remember { mutableStateOf(profile.avatarUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    fun addBadge(badge: String) {
        val trimmed = badge.trim()
        if (trimmed.isNotBlank() && !selectedBadges.contains(trimmed)) {
            selectedBadges = selectedBadges + trimmed
        }
        badgeSearchInput = ""
    }

    val filteredSuggestions = remember(badgeSearchInput) {
        if (badgeSearchInput.isBlank()) {
            POPULAR_BADGES
        } else {
            POPULAR_BADGES.filter { it.contains(badgeSearchInput.trim(), ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(CleanBackground)
                    .border(1.5.dp, LightBrownBorder, CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Current Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Add Photo",
                        tint = LightBrown,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (isUploadingImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Text(
                text = "Change Photo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = LightBrown,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { imagePickerLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            CleanOutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name")
            Spacer(modifier = Modifier.height(12.dp))

            CleanOutlinedTextField(value = degree, onValueChange = { degree = it }, label = "Degree / Major")
            Spacer(modifier = Modifier.height(12.dp))

            CleanOutlinedTextField(value = universityName, onValueChange = { universityName = it }, label = "University")
            Spacer(modifier = Modifier.height(12.dp))

            CleanOutlinedTextField(value = location, onValueChange = { location = it }, label = "Location")
            Spacer(modifier = Modifier.height(12.dp))

            CleanOutlinedTextField(value = bio, onValueChange = { bio = it }, label = "Bio", maxLines = 3)

            Spacer(modifier = Modifier.height(20.dp))

            // BADGES SECTION
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Profile Badges & Interests",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1B1F)
                )
                Text(
                    text = "Select chips or type custom tags to help match with similar users and posts.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Active Badges Display
                if (selectedBadges.isNotEmpty()) {
                    OptInFlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalSpacing = 6.dp,
                        verticalSpacing = 6.dp
                    ) {
                        selectedBadges.forEach { badge ->
                            InputChip(
                                selected = true,
                                onClick = { selectedBadges = selectedBadges - badge },
                                label = { Text(badge, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = LightBrown.copy(alpha = 0.12f),
                                    selectedLabelColor = LightBrown
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    enabled = true,
                                    selected = true,
                                    borderColor = LightBrownBorder
                                )
                            )
                        }
                    }
                }

                // Input Bar for Badge Filter/Creation
                OutlinedTextField(
                    value = badgeSearchInput,
                    onValueChange = { badgeSearchInput = it },
                    label = { Text("Search or type custom badge...", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (badgeSearchInput.isNotBlank()) {
                            addBadge(badgeSearchInput)
                        }
                    }),
                    trailingIcon = {
                        if (badgeSearchInput.isNotBlank()) {
                            IconButton(onClick = { addBadge(badgeSearchInput) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Badge", tint = LightBrown)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightBrown,
                        unfocusedBorderColor = LightBrownBorder,
                        focusedLabelColor = LightBrown,
                        cursorColor = LightBrown,
                        focusedContainerColor = CleanBackground,
                        unfocusedContainerColor = CleanBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Suggestion Quick Chips
                OptInFlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalSpacing = 6.dp,
                    verticalSpacing = 6.dp
                ) {
                    if (badgeSearchInput.isNotBlank() && POPULAR_BADGES.none { it.equals(badgeSearchInput.trim(), ignoreCase = true) }) {
                        FilterChip(
                            selected = false,
                            onClick = { addBadge(badgeSearchInput) },
                            label = { Text("+ Add \"${badgeSearchInput.trim()}\"", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = LightBrown.copy(alpha = 0.08f),
                                labelColor = LightBrown
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = LightBrownBorder)
                        )
                    }

                    filteredSuggestions.forEach { badge ->
                        val isSelected = selectedBadges.contains(badge)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedBadges = selectedBadges - badge
                                } else {
                                    addBadge(badge)
                                }
                            },
                            label = { Text(badge, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LightBrown,
                                selectedLabelColor = Color.White,
                                containerColor = CleanBackground,
                                labelColor = Color(0xFF4A4A4A)
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = LightBrownBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val currentUserId = supabase.auth.currentUserOrNull()?.id ?: profile.id
                    if (fullName.isBlank()) {
                        Toast.makeText(context, "Full name is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    scope.launch {
                        try {
                            var updatedAvatarUrl = avatarUrl

                            if (selectedImageUri != null) {
                                isUploadingImage = true
                                val bytes = context.contentResolver.openInputStream(selectedImageUri!!)?.readBytes()
                                if (bytes != null) {
                                    val fileName = "$currentUserId/${UUID.randomUUID()}.jpg"
                                    val bucket = supabase.storage.from("avatars")
                                    bucket.upload(path = fileName, data = bytes) {
                                        upsert = true
                                    }
                                    updatedAvatarUrl = bucket.publicUrl(fileName)
                                }
                                isUploadingImage = false
                            }

                            // ✅ FIXED: Using ProfileUpdate object instead of mapOf(...)
                            val profileUpdate = ProfileUpdate(
                                fullName = fullName.trim(),
                                bio = bio.trim().ifBlank { null },
                                degree = degree.trim().ifBlank { null },
                                universityName = universityName.trim().ifBlank { null },
                                location = location.trim().ifBlank { null },
                                avatarUrl = updatedAvatarUrl.ifBlank { null },
                                badges = selectedBadges
                            )

                            supabase.from("profiles").update(profileUpdate) {
                                filter { eq("id", currentUserId) }
                            }

                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                            onProfileUpdated()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving = false
                            isUploadingImage = false
                        }
                    }
                },
                enabled = !isSaving && !isUploadingImage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightBrown,
                    contentColor = Color.White
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 6.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 6.dp,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content
    )
}

@Composable
private fun CleanOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        maxLines = maxLines,
        singleLine = maxLines == 1,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LightBrown,
            unfocusedBorderColor = LightBrownBorder,
            focusedLabelColor = LightBrown,
            cursorColor = LightBrown,
            focusedContainerColor = CleanBackground,
            unfocusedContainerColor = CleanBackground
        )
    )
}