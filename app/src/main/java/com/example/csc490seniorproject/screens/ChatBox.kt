package com.example.csc490seniorproject.screens

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBox(modifier: Modifier = Modifier, friendUsernames: List<String> = emptyList()) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUid by remember { mutableStateOf(auth.currentUser?.uid) }

    val ChatPurple = Color(0xFF6938EF)
    data class MessengerPerson(val uid: String, val username: String)
    data class MessengerMessage(val id: String, val senderId: String, val recipientId: String, val text: String, val time: Long)

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            currentUid = it.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val uid = currentUid
    if (uid == null) {
        Text("Please log in to view your chats.", modifier.padding(16.dp))
        return
    }

    key(uid) {
        val messages = remember { mutableStateListOf<MessengerMessage>() }
        val people = remember { mutableStateMapOf<String, MessengerPerson>() }
        val friendIds = remember { mutableStateListOf<String>() }

        var selectedUid by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedName by rememberSaveable { mutableStateOf("") }
        var search by rememberSaveable { mutableStateOf("") }
        var draft by rememberSaveable(selectedUid) { mutableStateOf("") }

        var loading by remember { mutableStateOf(true) }
        var finding by remember { mutableStateOf(false) }
        var sending by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        // One listener supplies both the inbox and the open conversation.
        DisposableEffect(uid) {
            var active = true
            val registration = db.collection("chatMessages")
                .whereArrayContains("participants", uid)
                .addSnapshotListener { snapshot, exception ->
                    if (active) {
                        loading = false
                        if (exception != null) {
                            error = exception.localizedMessage
                                ?: "Couldn't load conversations."
                        } else {
                            val updated = snapshot?.documents.orEmpty().map {
                                MessengerMessage(
                                    id = it.id,
                                    senderId = it.getString("senderId").orEmpty(),
                                    recipientId = it.getString("recipientId").orEmpty(),
                                    text = it.getString("text").orEmpty(),
                                    time = it.getTimestamp("createdAt")?.toDate()?.time ?: Long.MAX_VALUE)

                            }.sortedWith(
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

        // Resolve the friends supplied by ProfileScreen to their account IDs.
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
                                people[document.id] = MessengerPerson(document.id, document.getString("username") ?: name)
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

            onDispose { active = false }
        }

        val conversations = messages.groupBy {
            if (it.senderId == uid) it.recipientId else it.senderId
        }.filterKeys { it.isNotBlank() && it != uid }

        val peerIds = conversations.keys.sorted()

        // Load names for existing conversations, including after restarting.
        DisposableEffect(uid, peerIds) {
            var active = true

            peerIds.filter { it !in people }.forEach { peerId ->
                db.collection("users").document(peerId).get()
                    .addOnSuccessListener { document ->
                        if (active) {
                            people[peerId] = MessengerPerson(
                                peerId,
                                document.getString("username") ?: "Unknown user"
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

            onDispose { active = false }
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

        BackHandler(enabled = selectedUid != null && !sending) {
            selectedUid = null
            error = ""
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
                        placeholder = { Text("Search username") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, "Search")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Search can also find someone you haven't messaged yet.
                    if (search.isNotBlank()) {
                        TextButton(
                            enabled = !finding,
                            onClick = {
                                finding = true
                                error = ""
                                val name = search.trim()

                                db.collection("users")
                                    .whereEqualTo("username", name)
                                    .limit(2)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        finding = false
                                        if (auth.currentUser?.uid == uid) {
                                            when {
                                                result.isEmpty ->
                                                    error = "Username not found."

                                                result.documents.size > 1 ->
                                                    error = "Multiple accounts use this username."

                                                result.documents.first().id == uid ->
                                                    error = "Choose another person's username."

                                                else -> {
                                                    val document = result.documents.first()
                                                    val person = MessengerPerson(
                                                        document.id,
                                                        document.getString("username") ?: name
                                                    )
                                                    people[person.uid] = person
                                                    openPerson(person)
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        finding = false
                                        error = it.localizedMessage ?: "Search failed."
                                    }
                            }
                        ) {
                            Text(
                                if (finding) "Searching…"
                                else "Find exact username",
                                color = ChatPurple
                            )
                        }
                    }

                    val visibleIds = (
                            conversations.keys + friendIds
                            ).distinct().filter { peerId ->
                            search.isBlank() ||
                                    people[peerId]?.username
                                        ?.contains(search.trim(), ignoreCase = true) == true
                        }.sortedWith(
                            compareByDescending<String> {
                                conversations[it]?.lastOrNull()?.time ?: 0L
                            }.thenBy { people[it]?.username.orEmpty() }
                        )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (visibleIds.isEmpty()) {
                            item {
                                Text(
                                    when {
                                        loading -> "Loading conversations…"
                                        search.isNotBlank() -> "No matching chats. Try exact username search."
                                        else -> "Your conversations appear here after you send or receive a message."
                                    },
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }

                        items(visibleIds, key = { it }) { peerId ->
                            val person = people[peerId]
                            val lastMessage = conversations[peerId]?.lastOrNull()

                            Surface(
                                onClick = {
                                    person?.let { openPerson(it) }
                                },
                                enabled = person != null,
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ChatAvatar(person?.username ?: "?")

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            person?.username ?: "Loading profile…",
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            if (lastMessage == null) {
                                                "Tap to start chatting"
                                            } else {
                                                (if (lastMessage.senderId == uid) "You: " else "") +
                                                        lastMessage.text
                                            },
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (
                                        lastMessage != null &&
                                        lastMessage.time != Long.MAX_VALUE
                                    ) {
                                        Text(
                                            SimpleDateFormat("MMM d", Locale.US).format(Date(lastMessage.time)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            enabled = !sending,
                            onClick = {
                                selectedUid = null
                                error = ""
                            }
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                "Back to conversations",
                                tint = ChatPurple
                            )
                        }
                        ChatAvatar(selectedName)
                        Text(
                            selectedName,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (conversation.isEmpty()) {
                            item {
                                Text(
                                    if (loading) "Loading…"
                                    else "Say hello to $selectedName!",
                                    color = Color.Gray
                                )
                            }
                        }

                        items(conversation, key = { it.id }) { message ->
                            val mine = message.senderId == uid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    if (mine) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (mine) ChatPurple else Color.White,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        message.text,
                                        color = if (mine) Color.White else Color.DarkGray,
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 10.dp
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
                            placeholder = { Text("Message…") },
                            enabled = !sending,
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FilledIconButton(
                            enabled = draft.isNotBlank() && !sending,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = ChatPurple
                            ),
                            onClick = {
                                val recipient = selectedUid
                                val text = draft.trim()

                                if (text.length > 2000) {
                                    error = "Keep messages under 2,001 characters."
                                } else if (recipient != null && text.isNotEmpty()) {
                                    sending = true
                                    error = ""

                                    db.collection("chatMessages").add(
                                        hashMapOf<String, Any>(
                                            "senderId" to uid,
                                            "recipientId" to recipient,
                                            "participants" to listOf(uid, recipient),
                                            "text" to text,
                                            "createdAt" to FieldValue.serverTimestamp()
                                        )
                                    ).addOnSuccessListener {
                                        draft = ""
                                        sending = false
                                    }.addOnFailureListener {
                                        sending = false
                                        error = it.localizedMessage ?: "Couldn't send message."
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Send,
                                if (sending) "Sending" else "Send message"
                            )
                        }
                    }
                }

                if (error.isNotBlank()) {
                    Text(
                        error,
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
            .background(Color(0xFFE8DFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            username.take(2).uppercase(Locale.ROOT),
            fontWeight = FontWeight.Bold
        )
    }
}