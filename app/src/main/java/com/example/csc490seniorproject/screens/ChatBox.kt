package com.example.csc490seniorproject.screens

import androidx.compose.material.icons.filled.MoreVert
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBox(modifier: Modifier = Modifier, friendUsernames: List<String> = emptyList()) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val chatPurple = Color(0xFF6938EF)

    data class MessengerPerson(
        val uid: String,
        val username: String
    )

    data class MessengerMessage(
        val id: String,
        val senderId: String,
        val recipientId: String,
        val text: String,
        val time: Long
    )

    var currentUid by remember {
        mutableStateOf(auth.currentUser?.uid)
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            currentUid = it.currentUser?.uid
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val uid = currentUid

    if (uid == null) {
        Text(
            text = "Please log in to view your chats.",
            modifier = modifier.padding(16.dp)
        )
        return
    }

    key(uid) {
        val messages = remember {
            mutableStateListOf<MessengerMessage>()
        }
        val people = remember {
            mutableStateMapOf<String, MessengerPerson>()
        }
        val friendIds = remember {
            mutableStateListOf<String>()
        }
        val deletedThrough = remember {
            mutableStateMapOf<String, Long>()
        }

        var selectedUid by rememberSaveable {
            mutableStateOf<String?>(null)
        }
        var selectedName by rememberSaveable {
            mutableStateOf("")
        }
        var search by rememberSaveable {
            mutableStateOf("")
        }
        var draft by rememberSaveable(selectedUid) {
            mutableStateOf("")
        }
        var searchFocused by remember {
            mutableStateOf(false)
        }

        var loading by remember {
            mutableStateOf(true)
        }
        var deletionsLoaded by remember {
            mutableStateOf(false)
        }
        var sending by remember {
            mutableStateOf(false)
        }
        var error by remember {
            mutableStateOf("")
        }
        var settingsError by remember {
            mutableStateOf<String?>(null)
        }

        var conversationToDelete by remember {
            mutableStateOf<String?>(null)
        }
        var deleting by remember {
            mutableStateOf(false)
        }
        var deleteError by remember {
            mutableStateOf<String?>(null)
        }

        val listState = rememberLazyListState()

        // Load sent and received messages.
        DisposableEffect(uid) {
            var active = true

            val registration = db.collection("chatMessages")
                .whereArrayContains("participants", uid)
                .addSnapshotListener { snapshot, exception ->
                    if (active) {
                        loading = false

                        if (exception != null) {
                            error = exception.localizedMessage ?: "Couldn't load conversations."
                        } else {
                            val updated = snapshot?.documents.orEmpty()
                                .map { document ->
                                    MessengerMessage(
                                        id = document.id,
                                        senderId = document
                                            .getString("senderId")
                                            .orEmpty(),
                                        recipientId = document
                                            .getString("recipientId")
                                            .orEmpty(),
                                        text = document
                                            .getString("text")
                                            .orEmpty(),
                                        time = document
                                            .getTimestamp("createdAt")
                                            ?.toDate()
                                            ?.time
                                            ?: Long.MAX_VALUE
                                    )
                                }
                                .sortedWith(
                                    compareBy<MessengerMessage> { it.time }
                                        .thenBy { it.id }
                                )

                            messages.clear()
                            messages.addAll(updated)
                        }
                    }
                }

            onDispose {
                active = false
                registration.remove()
            }
        }

        // Load conversations deleted by this account.
        DisposableEffect(uid) {
            var active = true

            val registration = db.collection("users")
                .document(uid)
                .collection("chatDeletions")
                .addSnapshotListener { snapshot, exception ->
                    if (active) {
                        if (exception != null) {
                            settingsError = exception.localizedMessage ?: "Couldn't load conversation settings."
                        } else {
                            val updated = snapshot?.documents.orEmpty()
                                .associate { document ->
                                    val cutoff = document.getTimestamp(
                                        "deletedThrough",
                                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                    )?.toDate()?.time ?: deletedThrough[document.id] ?: 0L

                                    document.id to cutoff
                                }

                            deletedThrough.clear()
                            deletedThrough.putAll(updated)
                            deletionsLoaded = true
                            settingsError = null
                        }
                    }
                }

            onDispose {
                active = false
                registration.remove()
            }
        }

        // Resolve usernames supplied by ProfileScreen.
        val names = friendUsernames.distinct().sorted()

        DisposableEffect(uid, names) {
            var active = true
            friendIds.clear()

            names.filter { it.isNotBlank() }.forEach { name ->
                db.collection("users")
                    .whereEqualTo("username", name)
                    .limit(2)
                    .get()
                    .addOnSuccessListener { result ->
                        if (active && result.documents.size == 1) {
                            val document = result.documents.first()

                            if (document.id != uid) {
                                people[document.id] = MessengerPerson(
                                    uid = document.id,
                                    username = document.getString("username") ?: name
                                )

                                if (document.id !in friendIds) {
                                    friendIds.add(document.id)
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (active) {
                            error = it.localizedMessage ?: "Couldn't load friends."
                        }
                    }
            }

            onDispose {
                active = false
            }
        }

        // Hide messages sent before this account deleted the chat.
        val visibleMessages: List<MessengerMessage> =
            if (!deletionsLoaded) {
                emptyList()
            } else {
                messages.filter { message ->
                    val peerId = if (message.senderId == uid) {
                        message.recipientId
                    } else {
                        message.senderId
                    }

                    val cutoff = deletedThrough[peerId]
                    cutoff == null || message.time > cutoff
                }
            }

        val conversations = visibleMessages.groupBy {
            if (it.senderId == uid) {
                it.recipientId
            } else {
                it.senderId
            }
        }.filterKeys {
            it.isNotBlank() && it != uid
        }

        val peerIds = conversations.keys.sorted()

        DisposableEffect(uid, peerIds) {
            var active = true

            peerIds.filter { it !in people }.forEach { peerId ->
                db.collection("users")
                    .document(peerId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (active) {
                            people[peerId] = MessengerPerson(
                                uid = peerId,
                                username = document.getString("username") ?: "Unknown user"
                            )
                        }
                    }
                    .addOnFailureListener {
                        if (active) {
                            error = it.localizedMessage
                                ?: "Couldn't load a chat profile."
                        }
                    }
            }

            onDispose {
                active = false
            }
        }

        val conversation = selectedUid?.let {
            conversations[it]
        }.orEmpty()

        LaunchedEffect(selectedUid, conversation.lastOrNull()?.id) {
            if (conversation.isNotEmpty()) {
                listState.animateScrollToItem(conversation.lastIndex)
            }
        }

        fun openPerson(person: MessengerPerson) {
            selectedUid = person.uid
            selectedName = person.username
            error = ""
        }

        BackHandler(
            enabled = selectedUid != null &&
                    !sending &&
                    !deleting &&
                    conversationToDelete == null
        ) {
            selectedUid = null
            error = ""
        }

        // Confirmation for deleting an entire conversation.
        conversationToDelete?.let { peerId ->
            AlertDialog(
                onDismissRequest = {
                    if (!deleting) {
                        conversationToDelete = null
                        deleteError = null
                    }
                },
                title = {
                    Text("Delete conversation?")
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Remove your conversation with " +
                                    "${people[peerId]?.username ?: "this person"}?"
                        )

                        Text(
                            "Existing messages will disappear for you. " +
                                    "The other person keeps their copy. " +
                                    "New messages will appear normally."
                        )

                        deleteError?.let {
                            Text(
                                text = it,
                                color = Color(0xFFB3261E)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !deleting,
                        onClick = {
                            if (auth.currentUser?.uid != uid) {
                                deleteError = "Please log in again."
                            } else {
                                deleting = true
                                deleteError = null

                                db.collection("users")
                                    .document(uid)
                                    .collection("chatDeletions")
                                    .document(peerId)
                                    .set(mapOf("deletedThrough" to FieldValue.serverTimestamp()))
                                    .addOnSuccessListener {
                                        deleting = false
                                        conversationToDelete = null

                                        if (selectedUid == peerId) {
                                            selectedUid = null
                                        }

                                        search = ""
                                        searchFocused = false
                                    }
                                    .addOnFailureListener {
                                        deleting = false
                                        deleteError = it.localizedMessage ?: "Couldn't delete conversation."
                                    }
                            }
                        }
                    ) {
                        Text(
                            text = if (deleting) "Deleting…" else "Delete",
                            color = Color(0xFFB3261E)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !deleting,
                        onClick = {
                            conversationToDelete = null
                            deleteError = null
                        }
                    ) {
                        Text("Cancel", color = chatPurple)
                    }
                }
            )
        }

        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFF7F4FF)
        ) {
            Column(Modifier.padding(14.dp)) {
                if (selectedUid == null) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it
                            error = ""
                        },
                        placeholder = {
                            Text("Search username")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                searchFocused = it.isFocused
                            }
                    )

                    Spacer(Modifier.height(10.dp))

                    val conversationIds = conversations.keys
                        .filter { peerId ->
                            search.isBlank() ||
                                    people[peerId]?.username?.contains(
                                        search.trim(),
                                        ignoreCase = true
                                    ) == true
                        }
                        .sortedWith(
                            compareByDescending<String> {
                                conversations[it]?.lastOrNull()?.time ?: 0L
                            }.thenBy {
                                people[it]?.username.orEmpty()
                            }
                        )

                    val friendPickerIds = friendIds
                        .filter { it !in conversations.keys }
                        .filter { peerId ->
                            search.isBlank() ||
                                    people[peerId]?.username?.contains(search.trim(), ignoreCase = true) == true
                        }
                        .sortedBy {
                            people[it]?.username.orEmpty()
                        }

                    val showPicker =
                        searchFocused || search.isNotBlank()

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            conversationIds,
                            key = { "conv_$it" }
                        ) { peerId ->
                            val person = people[peerId]
                            val lastMessage =
                                conversations[peerId]?.lastOrNull()

                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Tap the profile/preview to open the chat.
                                    Surface(
                                        onClick = {
                                            person?.let { openPerson(it) }
                                        },
                                        enabled = person != null && !deleting,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            ChatAvatar(
                                                person?.username ?: "?"
                                            )

                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = person?.username ?: "Loading profile…",
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(Modifier.height(4.dp))

                                                Text(
                                                    text = if (
                                                        lastMessage == null
                                                    ) {
                                                        "Tap to start chatting"
                                                    } else {
                                                        val prefix = if (
                                                            lastMessage.senderId == uid
                                                        ) {
                                                            "You: "
                                                        } else {
                                                            ""
                                                        }

                                                        prefix + lastMessage.text
                                                    },
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // Delete button on the RIGHT of the inbox row.
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        if (
                                            lastMessage != null &&
                                            lastMessage.time != Long.MAX_VALUE
                                        ) {
                                            Text(
                                                text = SimpleDateFormat("MMM d", Locale.US).format(Date(lastMessage.time)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }

                                        Box {
                                            var menuOpen by remember(peerId) {
                                                mutableStateOf(false)
                                            }

                                            IconButton(
                                                enabled = !deleting,
                                                onClick = { menuOpen = true }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Conversation options",
                                                    tint = chatPurple,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = menuOpen,
                                                onDismissRequest = { menuOpen = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "Delete conversation",
                                                            color = Color(0xFFB3261E)
                                                        )
                                                    },
                                                    onClick = {
                                                        menuOpen = false
                                                        deleteError = null
                                                        conversationToDelete = peerId
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showPicker && friendPickerIds.isNotEmpty()) {
                            if (conversationIds.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }

                            item {
                                Text(
                                    text = "Friends",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            items(
                                friendPickerIds,
                                key = { "friend_$it" }
                            ) { peerId ->
                                val person = people[peerId]

                                Surface(
                                    onClick = {
                                        person?.let { openPerson(it) }
                                    },
                                    enabled = person != null && !deleting,
                                    color = Color.White,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        ChatAvatar(
                                            person?.username ?: "?"
                                        )

                                        Text(
                                            text = person?.username ?: "Loading profile…",
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(
                                            text = "Tap to start chatting",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        if (
                            conversationIds.isEmpty() &&
                            !(showPicker && friendPickerIds.isNotEmpty())
                        ) {
                            item {
                                Text(
                                    text = when {
                                        settingsError != null ->
                                            "Unable to load conversations."

                                        loading || !deletionsLoaded ->
                                            "Loading conversations…"

                                        search.isNotBlank() ->
                                            "No matching chats."

                                        else ->
                                            "Your conversations appear here " + "after you send or receive a message."
                                    },
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            enabled = !sending && !deleting,
                            onClick = {
                                selectedUid = null
                                error = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to conversations",
                                tint = chatPurple
                            )
                        }

                        ChatAvatar(selectedName)

                        Text(
                            text = selectedName,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (conversation.isEmpty()) {
                            item {
                                Text(
                                    text = if (
                                        loading || !deletionsLoaded
                                    ) {
                                        "Loading…"
                                    } else {
                                        "Say hello to $selectedName!"
                                    },
                                    color = Color.Gray
                                )
                            }
                        }

                        // Message bubbles have no delete buttons.
                        items(
                            conversation,
                            key = { it.id }
                        ) { message ->
                            val mine = message.senderId == uid

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (mine) {
                                    Arrangement.End
                                } else {
                                    Arrangement.Start
                                }
                            ) {
                                Surface(
                                    color = if (mine) {
                                        chatPurple
                                    } else {
                                        Color.White
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        color = if (mine) {
                                            Color.White
                                        } else {
                                            Color.DarkGray
                                        },
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = {
                                Text("Message…")
                            },
                            enabled = !sending,
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FilledIconButton(
                            enabled = draft.isNotBlank() && !sending && deletionsLoaded && settingsError == null,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = chatPurple),
                            onClick = {
                                val recipient = selectedUid
                                val text = draft.trim()

                                when {
                                    auth.currentUser?.uid != uid -> {
                                        error = "Please log in again."
                                    }

                                    text.length > 2000 -> {
                                        error = "Keep messages under 2,001 characters."
                                    }

                                    recipient != null && text.isNotEmpty() -> {
                                        sending = true
                                        error = ""

                                        db.collection("chatMessages").add(hashMapOf<String, Any>(
                                                    "senderId" to uid,
                                                    "recipientId" to recipient,
                                                    "participants" to listOf(uid, recipient),
                                                    "text" to text,
                                                    "createdAt" to FieldValue.serverTimestamp()
                                                )
                                            )
                                            .addOnSuccessListener {
                                                draft = ""
                                                sending = false
                                            }
                                            .addOnFailureListener {
                                                sending = false
                                                error = it.localizedMessage ?: "Couldn't send message."
                                            }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = if (sending) {
                                    "Sending"
                                } else {
                                    "Send message"
                                }
                            )
                        }
                    }
                }

                val displayedError = settingsError
                    ?: error.takeIf { it.isNotBlank() }

                displayedError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatAvatar(username: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = Color(0xFFE8DFFF),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = username.take(2).uppercase(Locale.ROOT),
            fontWeight = FontWeight.Bold
        )
    }
}
