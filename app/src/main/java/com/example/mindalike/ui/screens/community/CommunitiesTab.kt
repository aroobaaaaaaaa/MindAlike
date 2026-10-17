package com.example.mindalike.ui.screens.community

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mindalike.data.Community
import com.example.mindalike.data.fetchAllCommunities
import com.example.mindalike.data.fetchJoinedCommunityIds
import com.example.mindalike.data.joinCommunity
import com.example.mindalike.data.supabase
import com.example.mindalike.ui.components.BrandBg
import com.example.mindalike.ui.components.BrandBorder
import com.example.mindalike.ui.components.BrandBrown
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items

internal fun categoryColor(category: String): Color = when (category.lowercase()) {
    "tech" -> Color(0xFF5865F2)
    "sports" -> Color(0xFF3BA55D)
    "arts" -> Color(0xFFEB459E)
    "academic" -> Color(0xFFF0A020)
    "support" -> Color(0xFF2BB3A8)
    else -> BrandBrown
}

internal fun categoryEmoji(category: String): String = when (category.lowercase()) {
    "tech" -> "💻"
    "sports" -> "⚽"
    "arts" -> "🎨"
    "academic" -> "📚"
    "support" -> "💛"
    else -> "🌟"
}

@Composable
internal fun CommunityIcon(community: Community, size: Dp, modifier: Modifier = Modifier) {
    val accent = categoryColor(community.category)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .padding(3.dp)
            .clip(CircleShape)
            .background(accent),
        contentAlignment = Alignment.Center
    ) {
        if (!community.iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = community.iconUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(categoryEmoji(community.category), fontSize = (size.value * 0.42f).sp)
        }
    }
}

/**
 * Drop this into the Communities tab of your FYP screen: CommunitiesTab()
 * If onCommunityClick is null, the chat opens inline (no navigation changes needed).
 */
@Composable
fun CommunitiesTab(
    onCommunityClick: ((String) -> Unit)? = null,
    onUserProfileClick: (String) -> Unit = {}
) {
    var openId by remember { mutableStateOf<String?>(null) }
    val currentOpen = openId

    if (currentOpen != null && onCommunityClick == null) {
        BackHandler { openId = null }
        CommunityChatScreen(
            communityId = currentOpen,
            onBack = { openId = null },
            onUserProfileClick = onUserProfileClick
        )
    } else {
        CommunityListContent(onOpen = { id ->
            if (onCommunityClick != null) onCommunityClick(id) else openId = id
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityListContent(onOpen: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val me = supabase.auth.currentUserOrNull()?.id ?: ""

    var communities by remember { mutableStateOf<List<Community>>(emptyList()) }
    var joined by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var busy by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        loading = true
        communities = fetchAllCommunities()
        if (me.isNotBlank()) joined = fetchJoinedCommunityIds(me)
        loading = false
    }

    val categories = listOf("All") + communities.map { it.category }.distinct()
    val shown = communities.filter { c ->
        (category == "All" || c.category == category) &&
                (query.isBlank() ||
                        c.name.contains(query, ignoreCase = true) ||
                        (c.description ?: "").contains(query, ignoreCase = true))
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(BrandBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBrown)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrandBg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF8C6D58), Color(0xFFB8957A))))
                    .padding(18.dp)
            ) {
                Column {
                    Text("Find your people 👋", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Jump into a hub, say hi, and build something together.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search hubs", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBrown,
                    unfocusedBorderColor = BrandBorder,
                    cursorColor = BrandBrown,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBrown,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No hubs found 🔍", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        items(shown, key = { it.id ?: it.name }) { community ->
            val id = community.id
            CommunityCard(
                community = community,
                isJoined = id != null && id in joined,
                isBusy = id != null && id in busy,
                onOpen = { id?.let { onOpen(it) } },
                onJoin = {
                    if (id != null && me.isNotBlank()) {
                        scope.launch {
                            busy = busy + id
                            if (joinCommunity(id, me)) {
                                joined = joined + id
                                communities = communities.map {
                                    if (it.id == id) it.copy(memberCount = it.memberCount + 1) else it
                                }
                            }
                            busy = busy - id
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun CommunityCard(
    community: Community,
    isJoined: Boolean,
    isBusy: Boolean,
    onOpen: () -> Unit,
    onJoin: () -> Unit
) {
    val accent = categoryColor(community.category)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.55f))))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                CommunityIcon(community, 52.dp, Modifier.offset(y = (-22).dp))
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                    Text(
                        text = community.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.14f)) {
                        Text(
                            text = "# ${community.category}",
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (!community.description.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = community.description,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3BA55D)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (community.memberCount == 1) "1 member" else "${community.memberCount} members",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                if (isJoined) {
                    OutlinedButton(
                        onClick = onOpen,
                        modifier = Modifier.padding(top = 6.dp).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BrandBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBrown)
                    ) {
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onJoin,
                        enabled = !isBusy,
                        modifier = Modifier.padding(top = 6.dp).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBrown)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}