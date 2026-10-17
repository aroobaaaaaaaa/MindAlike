package com.example.mindalike.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mindalike.data.Message
import com.example.mindalike.data.Profile
import com.example.mindalike.data.deleteDirectChat
import com.example.mindalike.data.deleteMessage
import com.example.mindalike.data.fetchDirectMessages
import com.example.mindalike.data.fetchFriendProfiles
import com.example.mindalike.data.fetchProfilesByIds
import com.example.mindalike.data.fetchRecentDirectMessages
import com.example.mindalike.data.sendMessage
import com.example.mindalike.data.supabase
import com.example.mindalike.ui.components.BrandBg
import com.example.mindalike.ui.components.BrandBorder
import com.example.mindalike.ui.components.BrandBrown
import com.example.mindalike.ui.components.LocalAppNavController
import com.example.mindalike.ui.components.MessageInputBar
import com.example.mindalike.ui.components.UserAvatar
import com.example.mindalike.ui.components.formatChatTime
import com.example.mindalike.util.AiContentModerator
import com.example.mindalike.util.AiModerationResult
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Conversation(val other: Profile, val last: Message)

@Composable
fun DirectMessagesScreen(onBack: () -> Unit = {}, bottomBarPadding: Dp = 90.dp) {
    val me = supabase.auth.currentUserOrNull()?.id ?: ""

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var friends by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var openUser by remember { mutableStateOf<Profile?>(null) }
    val chatUser = openUser
    val nav = LocalAppNavController.current
    val openChat: (Profile) -> Unit = { p ->
        if (nav != null) nav.navigate("chat/${p.id}") else openUser = p
    }

    suspend fun load() {
        val msgs = fetchRecentDirectMessages(me)
        val latestByOther = LinkedHashMap<String, Message>()
        for (m in msgs) {
            val otherId = if (m.senderId == me) (m.receiverId ?: continue) else m.senderId
            if (otherId !in latestByOther) latestByOther[otherId] = m
        }
        val profs = fetchProfilesByIds(latestByOther.keys)
        conversations = latestByOther.mapNotNull { (id, m) -> profs[id]?.let { Conversation(it, m) } }
        friends = fetchFriendProfiles(me)
    }

    LaunchedEffect(chatUser == null) {
        if (chatUser == null && me.isNotBlank()) {
            while (true) {
                load()
                loading = false
                delay(5000)
            }
        }
    }

    if (chatUser != null) {
        BackHandler { openUser = null }
        DmChatScreen(other = chatUser, onBack = { openUser = null }, bottomBarPadding = bottomBarPadding)
        return
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(BrandBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBrown)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrandBg),
        contentPadding = PaddingValues(bottom = bottomBarPadding + 16.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.padding(start = 4.dp)) {
                    Text("Messages", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Chat with your friends 💬", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        if (friends.isNotEmpty()) {
            item {
                Text(
                    "Friends",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBrown,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(friends, key = { it.id }) { f ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .clickable { openChat(f) }
                        ) {
                            UserAvatar(f.avatarUrl, f.fullName, 56.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                f.fullName.substringBefore(" "),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Recent chats",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBrown,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp)
            )
        }

        if (conversations.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (friends.isEmpty()) "Add some friends to start chatting 👋"
                        else "No chats yet. Tap a friend above to say hi 👋",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        items(conversations, key = { it.other.id }) { convo ->
            val preview = when {
                convo.last.content.isNotBlank() -> convo.last.content
                !convo.last.mediaUrl.isNullOrBlank() -> "📷 Photo"
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openChat(convo.other) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(convo.other.avatarUrl, convo.other.fullName, 50.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(convo.other.fullName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        text = (if (convo.last.senderId == me) "You: " else "") + preview,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(formatChatTime(convo.last.createdAt), fontSize = 10.sp, color = Color.Gray)
            }
            HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f), modifier = Modifier.padding(start = 80.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DmChatScreen(other: Profile, onBack: () -> Unit, bottomBarPadding: Dp = 0.dp) {
    val scope = rememberCoroutineScope()
    val me = supabase.auth.currentUserOrNull()?.id ?: ""
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var moderationError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    // Delete UI state
    var chatMenuOpen by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var deletingChat by remember { mutableStateOf(false) }
    var menuForMessage by remember { mutableStateOf<Message?>(null) }

    val listState = rememberLazyListState()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val extraBottom = if (imeVisible) 0.dp else bottomBarPadding

    suspend fun refresh() {
        messages = fetchDirectMessages(me, other.id)
    }

    LaunchedEffect(other.id) {
        refresh()
        loading = false
        while (true) {
            delay(2500)
            refresh()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
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

    fun doDeleteChat() {
        scope.launch {
            deletingChat = true
            val ok = deleteDirectChat(me, other.id)
            deletingChat = false
            showDeleteChatDialog = false
            if (ok) {
                messages = emptyList()
                onBack()
            } else {
                moderationError = "Couldn't delete the chat. Try again."
            }
        }
    }

    if (showDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { if (!deletingChat) showDeleteChatDialog = false },
            title = { Text("Delete chat?") },
            text = {
                Text(
                    "This permanently deletes all messages between you and " +
                            "${other.fullName.substringBefore(" ")}. " +
                            "It will be removed for both of you and can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { doDeleteChat() }, enabled = !deletingChat) {
                    Text(if (deletingChat) "Deleting..." else "Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }, enabled = !deletingChat) {
                    Text("Cancel")
                }
            }
        )
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
                        UserAvatar(other.avatarUrl, other.fullName, 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(other.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (!other.universityName.isNullOrBlank()) {
                                Text(other.universityName, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Box {
                            IconButton(onClick = { chatMenuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = chatMenuOpen, onDismissRequest = { chatMenuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Delete chat", color = Color.Red.copy(alpha = 0.8f)) },
                                    onClick = {
                                        chatMenuOpen = false
                                        showDeleteChatDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

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
            Column(modifier = Modifier.padding(bottom = extraBottom)) {
                if (isChecking) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = BrandBrown
                    )
                }
                MessageInputBar(
                    placeholder = if (isChecking) "Checking safety..." else "Message ${other.fullName.substringBefore(" ")}"
                ) { text ->
                    // Ignore taps while a check is already running (prevents double sends)
                    if (isChecking) return@MessageInputBar

                    scope.launch {
                        isChecking = true
                        moderationError = null

                        when (AiContentModerator.checkMessage(text)) {
                            AiModerationResult.FLIRTING -> {
                                moderationError = "🚫 Flirting and romantic advances are not allowed on MindAlike."
                            }
                            AiModerationResult.TOXIC -> {
                                moderationError = "⚠️ This message violates community guidelines."
                            }
                            AiModerationResult.ERROR -> {
                                moderationError = "Couldn't verify your message. Check your connection and try again."
                            }
                            AiModerationResult.SAFE -> {
                                if (sendMessage(Message(senderId = me, receiverId = other.id, content = text))) {
                                    refresh()
                                }
                            }
                        }
                        isChecking = false
                    }
                }
            }
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBrown)
            }
        } else if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Say hi to ${other.fullName.substringBefore(" ")} 👋", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(messages, key = { index, m -> m.id ?: "local-$index" }) { index, msg ->
                    val mine = msg.senderId == me
                    val next = messages.getOrNull(index + 1)
                    val showTime = next == null || next.senderId != msg.senderId

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
                    ) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (mine) 18.dp else 4.dp,
                                    bottomEnd = if (mine) 4.dp else 18.dp
                                ),
                                color = if (mine) BrandBrown else Color.White,
                                border = if (mine) null else BorderStroke(1.dp, BrandBorder),
                                modifier = Modifier
                                    .widthIn(max = 290.dp)
                                    // Long-press only on your own messages
                                    .combinedClickable(
                                        enabled = mine,
                                        onClick = {},
                                        onLongClick = { menuForMessage = msg }
                                    )
                            ) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    if (!msg.mediaUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = msg.mediaUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .heightIn(max = 220.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    if (msg.content.isNotBlank()) {
                                        Text(
                                            msg.content,
                                            fontSize = 14.sp,
                                            color = if (mine) Color.White else Color(0xFF1C1B1F)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = menuForMessage == msg,
                                onDismissRequest = { menuForMessage = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete message", color = Color.Red.copy(alpha = 0.8f)) },
                                    onClick = {
                                        menuForMessage = null
                                        doDeleteMessage(msg)
                                    }
                                )
                            }
                        }
                        if (showTime) {
                            Text(
                                formatChatTime(msg.createdAt),
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Opens a chat directly with a user id (used by the profile "Message" button). */
@Composable
fun DmChatRoute(userId: String, onBack: () -> Unit) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        profile = fetchProfilesByIds(listOf(userId))[userId]
        loading = false
    }

    val p = profile
    when {
        loading -> Box(Modifier.fillMaxSize().background(BrandBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBrown)
        }
        p != null -> DmChatScreen(other = p, onBack = onBack)
        else -> Box(Modifier.fillMaxSize().background(BrandBg), contentAlignment = Alignment.Center) {
            Text("User not found", color = Color.Gray)
        }
    }
}