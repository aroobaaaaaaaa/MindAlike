package com.example.mindalike.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val BrandBrown = Color(0xFF8C6D58)
val BrandBorder = Color(0xFFE2D7CE)
val BrandBg = Color(0xFFFBF8F5)

/** Provided by AppNavGraph so any screen can open full-screen pages (no bottom nav). */
val LocalAppNavController = compositionLocalOf<NavHostController?> { null }

@Composable
fun UserAvatar(url: String?, name: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(BrandBorder),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(BrandBrown.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = BrandBrown,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

@Composable
fun MessageInputBar(placeholder: String, modifier: Modifier = Modifier, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val canSend = text.isNotBlank()

    Surface(modifier = modifier, color = Color.White, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBrown,
                    unfocusedBorderColor = BrandBorder,
                    cursorColor = BrandBrown,
                    focusedContainerColor = BrandBg,
                    unfocusedContainerColor = BrandBg
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (canSend) {
                        onSend(text.trim())
                        text = ""
                    }
                },
                enabled = canSend,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = BrandBrown,
                    contentColor = Color.White,
                    disabledContainerColor = BrandBorder,
                    disabledContentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

fun parseChatInstant(ts: String?): Instant? = try {
    if (ts.isNullOrBlank()) null else OffsetDateTime.parse(ts).toInstant()
} catch (e: Exception) {
    null
}

fun closeInTime(a: String?, b: String?, minutes: Long = 5): Boolean {
    val x = parseChatInstant(a) ?: return false
    val y = parseChatInstant(b) ?: return false
    return Duration.between(x, y).abs().toMinutes() < minutes
}

fun formatChatTime(ts: String?): String {
    val instant = parseChatInstant(ts) ?: return ""
    val zone = ZoneId.systemDefault()
    val zdt = instant.atZone(zone)
    val today = LocalDate.now(zone)
    return when (zdt.toLocalDate()) {
        today -> zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
        today.minusDays(1) -> "Yesterday"
        else -> zdt.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}