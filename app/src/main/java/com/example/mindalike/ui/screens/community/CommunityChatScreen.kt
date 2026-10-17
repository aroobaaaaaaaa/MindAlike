package com.example.mindalike.ui.screens.community

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mindalike.data.Community
import com.example.mindalike.data.Message
import com.example.mindalike.data.Profile
import com.example.mindalike.data.deleteMessage
import com.example.mindalike.data.fetchCommunity
import com.example.mindalike.data.fetchCommunityMessages
import com.example.mindalike.data.fetchJoinedCommunityIds
import com.example.mindalike.data.fetchProfilesByIds
import com.example.mindalike.data.joinCommunity
import com.example.mindalike.data.leaveCommunity
import com.example.mindalike.data.sendMessage
import com.example.mindalike.data.supabase
import com.example.mindalike.ui.components.BrandBg
import com.example.mindalike.ui.components.BrandBorder
import com.example.mindalike.ui.components.BrandBrown
import com.example.mindalike.ui.components.MessageInputBar
import com.example.mindalike.ui.components.UserAvatar
import com.example.mindalike.ui.components.closeInTime
import com.example.mindalike.ui.components.formatChatTime
import com.example.mindalike.util.AiContentModerator
import com.example.mindalike.util.AiModerationResult
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CommunityChatScreen(
    communityId: String,
    onBack: () -> Unit,
    onUserProfileClick: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val me = supabase.auth.currentUserOrNull()?.id ?: ""

    var community by remember { mutableStateOf<Community?>(null) }
    var isMember by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, Profile>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var joining by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // AI Moderation UI state feedback
    var moderationError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    suspend fun refreshMessages() {
        val msgs = fetchCommunityMessages(communityId)
        val missing = msgs.map { it.senderId }.distinct().filter { it !in profiles }
        if (missing.isNotEmpty()) profiles = profiles + fetchProfilesByIds(missing)
        messages = msgs
    }

    LaunchedEffect(communityId) {
        community = fetchCommunity(communityId)
        if (me.isNotBlank()) isMember = communityId in fetchJoinedCommunityIds(me)
        refreshMessages()
        loading = false
        while (true) {
            delay(3000)
            refreshMessages()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    fun doJoin() {
        scope.launch {
            joining = true
            if (joinCommunity(communityId, me)) {
                isMember = true
                community = community?.let { it.copy(memberCount = it.memberCount + 1) }
            }
            joining = false
        }
    }

    fun doLeave() {
        scope.launch {
            if (leaveCommunity(communityId, me)) {
                isMember = false
                community = community?.let { it.copy(memberCount = maxOf(it.memberCount - 1, 0)) }
            }
        }
    }

    fun doDeleteMessage(msg: Message) {
        val id = msg.id ?: return
        scope.launch {
            if (deleteMessage(id)) {
                messages = messages.filter { it.id != msg.id }
            } else {
                moderationError = "Couldn't delete the message. Try again."
            }
        }
    }

    Scaffold(
        containerColor = BrandBg,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                Surface(color = Color.White, shadowElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        community?.let { CommunityIcon(it, 38.dp) }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "# ${community?.name ?: ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            community?.let {
                                Text(
                                    text = "${it.memberCount} members • ${it.category}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        if (community != null) {
                            if (!isMember) {
                                Button(
                                    onClick = { doJoin() },
                                    enabled = !joining,
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(17.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBrown)
                                ) {
                                    Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Box {
                                    IconButton(onClick = { menuOpen = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Leave hub", color = Color.Red.copy(alpha = 0.8f)) },
                                            onClick = {
                                                menuOpen = false
                                                doLeave()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Show AI moderation error warning banner if message gets blocked
                moderationError?.let { err ->
                    Surface(color = Color(0xFFFFEBEE), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = err, color = Color(0xFFC62828), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { moderationError = null }) {
                                Text("Dismiss", fontSize = 11.sp, color = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isMember) {
                Column {
                    if (isChecking) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = BrandBrown
                        )
                    }
                    MessageInputBar(
                        placeholder = if (isChecking) "Checking safety..." else "Message #${community?.name ?: "hub"}"
                    ) { text ->
                        // Ignore taps while a check is already running (prevents double sends)
                        if (isChecking) return@MessageInputBar

                        scope.launch {
                            isChecking = true
                            moderationError = null

                            // 1. Run AI check BEFORE sending
                            when (AiContentModerator.checkMessage(text)) {
                                AiModerationResult.FLIRTING -> {
                                    moderationError = "🚫 Flirting and romantic advances are not allowed on MindAlike."
                                }
                                AiModerationResult.TOXIC -> {
                                    moderationError = "⚠️ This message violates community guidelines against toxic speech."
                                }
                                AiModerationResult.ERROR -> {
                                    moderationError = "Couldn't verify your message. Check your connection and try again."
                                }
                                AiModerationResult.SAFE -> {
                                    // 2. Safe to send to Supabase
                                    if (sendMessage(Message(senderId = me, communityId = communityId, content = text))) {
                                        refreshMessages()
                                    }
                                }
                            }
                            isChecking = false
                        }
                    }
                }
            } else if (community != null) {
                Surface(color = Color.White, shadowElevation = 6.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Join this hub to start chatting 💬",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { doJoin() },
                            enabled = !joining,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBrown)
                        ) { Text("Join", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBrown)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item { WelcomeHeader(community) }

                itemsIndexed(messages, key = { index, m -> m.id ?: "local-$index" }) { index, msg ->
                    val prev = messages.getOrNull(index - 1)
                    val grouped = prev != null &&
                            prev.senderId == msg.senderId &&
                            closeInTime(prev.createdAt, msg.createdAt)
                    MessageRow(
                        message = msg,
                        sender = profiles[msg.senderId],
                        grouped = grouped,
                        isMine = msg.senderId == me,
                        onUserClick = { onUserProfileClick(msg.senderId) },
                        onDelete = { doDeleteMessage(msg) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader(community: Community?) {
    if (community == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CommunityIcon(community, 72.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Welcome to #${community.name}!",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        if (!community.description.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                community.description,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "This is the start of the hub. Say hi 👋",
            fontSize = 12.sp,
            color = BrandBrown,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = BrandBorder)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: Message,
    sender: Profile?,
    grouped: Boolean,
    isMine: Boolean,
    onUserClick: () -> Unit,
    onDelete: () -> Unit
) {
    val name = sender?.fullName ?: "Someone"
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Long-press only on your own messages
            .combinedClickable(
                enabled = isMine,
                onClick = {},
                onLongClick = { menuOpen = true }
            )
            .padding(horizontal = 12.dp, vertical = if (grouped) 1.dp else 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!grouped) {
            UserAvatar(
                url = sender?.avatarUrl,
                name = name,
                size = 38.dp,
                modifier = Modifier.clickable { onUserClick() }
            )
        } else {
            Spacer(Modifier.width(38.dp))
        }
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (!grouped) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isMine) BrandBrown else Color(0xFF1C1B1F),
                        modifier = Modifier.clickable { onUserClick() }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(formatChatTime(message.createdAt), fontSize = 10.sp, color = Color.Gray)
                }
            }
            if (message.content.isNotBlank()) {
                Text(message.content, fontSize = 14.sp, color = Color(0xFF303030))
            }
            if (!message.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .widthIn(max = 240.dp)
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Delete message", color = Color.Red.copy(alpha = 0.8f)) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}