package com.example.mindalike.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mindalike.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

private val PrimaryColor = Color(0xFF8C6D58)
private val LightGrayBg = Color(0xFFF7F7F8)

@Serializable
private data class CreatePostPayload(
    val user_id: String,
    val content: String,
    val post_type: String,
    val media_url: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    postType: String, // Accepts "media", "post", or "thread"
    onPostSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Standardize input type: 'media' (Instagram post) vs 'thread' (LinkedIn article/thought)
    val isMediaPost = postType == "media" || postType == "post"
    val targetType = if (isMediaPost) "media" else "thread"

    var contentText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    suspend fun uploadMediaToSupabase(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            val fileName = "posts/${UUID.randomUUID()}.jpg"

            val bucket = supabase.storage.from("post-media")
            bucket.upload(fileName, bytes) {
                upsert = false
            }
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun submitPost() {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId == null) {
            Toast.makeText(context, "Session expired. Please log in.", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetType == "media" && selectedImageUri == null) {
            Toast.makeText(context, "Please attach a photo for your post.", Toast.LENGTH_SHORT).show()
            return
        }

        if (contentText.isBlank() && selectedImageUri == null) {
            Toast.makeText(context, "Cannot publish empty content.", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isPosting = true
            try {
                var uploadedMediaUrl: String? = null
                selectedImageUri?.let { uri ->
                    uploadedMediaUrl = uploadMediaToSupabase(uri)
                }

                val payload = CreatePostPayload(
                    user_id = currentUserId,
                    content = contentText.trim(),
                    post_type = targetType,
                    media_url = uploadedMediaUrl
                )

                supabase.from("posts").insert(payload)
                Toast.makeText(
                    context,
                    if (targetType == "media") "Photo published!" else "Thread published!",
                    Toast.LENGTH_SHORT
                ).show()
                onPostSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error publishing: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isPosting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (targetType == "media") "New Media Post" else "Start a Thread",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { submitPost() },
                        enabled = !isPosting && (contentText.isNotBlank() || selectedImageUri != null),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (targetType == "media") "Share" else "Publish Thread", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (targetType == "media") {
            // INSTAGRAM-STYLE PHOTO POST
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightGrayBg)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(48.dp), tint = PrimaryColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Select photo to upload", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = { Text("Write a caption...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        } else {
            // LINKEDIN-STYLE PROFESSIONAL THREAD
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Default.Article, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share knowledge, insights, or updates with your network", fontSize = 13.sp, color = Color.Gray)
                }

                Surface(
                    color = LightGrayBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        BasicTextField(
                            value = contentText,
                            onValueChange = { contentText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1C1B1F)),
                            decorationBox = { innerTextField ->
                                if (contentText.isEmpty()) {
                                    Text("What do you want to discuss?", style = TextStyle(fontSize = 15.sp, color = Color.Gray))
                                }
                                innerTextField()
                            }
                        )

                        AnimatedVisibility(visible = selectedImageUri != null) {
                            selectedImageUri?.let { uri ->
                                Box(
                                    modifier = Modifier
                                        .padding(top = 10.dp)
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    IconButton(
                                        onClick = { selectedImageUri = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedImageUri == null) "Attach image" else "Change image", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}