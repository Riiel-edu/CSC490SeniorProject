package com.example.csc490seniorproject.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.viewmodels.LandingScreenVM
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import com.example.csc490seniorproject.viewmodels.SongScreenVM
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query

@Composable
fun SongScreen(viewModel: SongScreenVM, songId: String = "") {

    var songName by remember(songId) {
        mutableStateOf("Loading…")
    }
    var songRating by remember(songId) {
        mutableStateOf(0.0)
    }
    var songDescription by remember(songId) {
        mutableStateOf("No description provided.")
    }
    var songLoadError by remember(songId) {
        mutableStateOf<String?>(null)
    }
    var openPanel by remember(songId) {
        mutableStateOf<String?>(null)
    }

    var ratingCount by remember(songId) {
        mutableIntStateOf(0)
    }
    var myRating by remember(songId) {
        mutableIntStateOf(0)
    }
    var selectedRating by remember(songId) {
        mutableIntStateOf(0)
    }
    var showRatingDialog by remember(songId) {
        mutableStateOf(false)
    }
    var savingRating by remember(songId) {
        mutableStateOf(false)
    }
    var ratingError by remember(songId) {
        mutableStateOf<String?>(null)
    }

    openPanel?.let { panel ->
        SongFeaturePopup( songId = songId, leaderboard = panel == "Leaderboard", onClose = { openPanel = null }
        )
    }
    var ownerId by remember(songId) { mutableStateOf("") }
    var showDescriptionEditor by remember(songId) { mutableStateOf(false) }
    var descriptionDraft by remember(songId) { mutableStateOf("") }
    var savingDescription by remember(songId) { mutableStateOf(false) }
    var descriptionError by remember(songId) {
        mutableStateOf<String?>(null)
    }

    val auth = remember { FirebaseAuth.getInstance() }
    var currentUid by remember {
        mutableStateOf(auth.currentUser?.uid)
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            currentUid = it.currentUser?.uid
            showDescriptionEditor = false
        }
        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val canEditDescription =
        songId.isNotBlank() &&
                ownerId.isNotBlank() &&
                currentUid == ownerId &&
                songLoadError == null

    DisposableEffect(songId) {
        var active = true

        val listener = if (songId.isNotBlank()) {
            FirebaseFirestore.getInstance()
                .collection("songs")
                .document(songId)
                .addSnapshotListener { document, error ->
                    if (active) {
                        when {
                            error != null -> {
                                songLoadError = "Unable to load song details."
                                songName = "Unable to load song"
                            }

                            document == null || !document.exists() -> {
                                songLoadError = "Song not found."
                                songName = "Song not found"
                            }

                            else -> {
                                ownerId = document.getString("ownerId").orEmpty()
                                songLoadError = null
                                songName = document.getString("name")
                                    ?: "Untitled Song"

                                songRating = (
                                        document.get("rating") as? Number
                                        )?.toDouble()?.coerceIn(0.0, 5.0) ?: 0.0

                                ratingCount = (
                                        document.get("ratingCount") as? Number
                                        )?.toInt() ?: 0

                                songDescription = document
                                    .getString("description")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "No description provided."
                            }
                        }
                    }
                }
        } else {
            songName = "No song selected"
            null
        }

        onDispose {
            active = false
            listener?.remove()
        }
    }

    DisposableEffect(songId) {
        if (songId.isBlank()) {
            onDispose { }
        } else {
            val listener = FirebaseFirestore.getInstance()
                .collection("songs")
                .document(songId)
                .collection("ratings")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val ratings = snapshot.documents.mapNotNull { doc ->
                            doc.getLong("rating")?.toInt()
                                ?.takeIf { it in 1..5 }
                        }

                        ratingCount = ratings.size
                        songRating = if (ratings.isNotEmpty()) {
                            ratings.average()
                        } else {
                            0.0
                        }
                    }
                }

            onDispose {
                listener.remove()
            }
        }
    }

    val fredoka = FontFamily(
        Font(R.font.fredoka_medium, FontWeight.Normal)
    )
    val purple = Color(0xFF5D36EB)


    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!savingRating) {
                    showRatingDialog = false
                }
            },
            title = {
                Text("Rate this song")
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (selectedRating == 0) {
                            "Select 1 to 5 stars"
                        } else {
                            "$selectedRating out of 5 stars"
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            TextButton(
                                enabled = !savingRating,
                                onClick = {
                                    selectedRating = star
                                    ratingError = null
                                },
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(
                                    text = if (star <= selectedRating) "★" else "☆",
                                    color = if (star <= selectedRating) {
                                        Color(0xFF5D36EB)
                                    } else {
                                        Color.Gray
                                    },
                                    fontSize = 32.sp
                                )
                            }
                        }
                    }

                    ratingError?.let { message ->
                        Text(
                            text = message,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedRating in 1..5 && !savingRating,
                    onClick = {
                        val uid = auth.currentUser?.uid

                        if (uid == null) {
                            ratingError = "Please log in first."
                            return@TextButton
                        }

                        savingRating = true
                        ratingError = null

                        val db = FirebaseFirestore.getInstance()
                        val songRef = db.collection("songs")
                            .document(songId)

                        val ratingRef = songRef
                            .collection("ratings")
                            .document(uid)

                        viewModel.saveSongRating(
                            songId = songId,
                            ratingValue = selectedRating,
                            onSuccess = {
                                savingRating = false
                                showRatingDialog = false
                            },
                            onError = { message ->
                                savingRating = false
                                ratingError = message
                            }
                        )
                    }
                ) {
                    Text(if (savingRating) "Saving..." else "Submit rating")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !savingRating,
                    onClick = {
                        showRatingDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showDescriptionEditor && canEditDescription) {
        AlertDialog(
            onDismissRequest = {
                if (!savingDescription) {
                    showDescriptionEditor = false
                }
            },
            title = {
                Text("Edit description")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = descriptionDraft,
                        onValueChange = {
                            if (it.length <= 1000) {
                                descriptionDraft = it
                                descriptionError = null
                            }
                        },
                        label = { Text("Description") },
                        supportingText = {
                            Text("${descriptionDraft.length}/1000")
                        },
                        minLines = 3,
                        maxLines = 6,
                        enabled = !savingDescription,
                        modifier = Modifier.fillMaxWidth()
                    )

                    descriptionError?.let {
                        Text(it, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !savingDescription,
                    onClick = {
                        if (auth.currentUser?.uid != ownerId) {
                            descriptionError =
                                "Only the song owner can edit this."
                        } else {
                            savingDescription = true
                            descriptionError = null

                            FirebaseFirestore.getInstance()
                                .collection("songs")
                                .document(songId)
                                .update(
                                    "description",
                                    descriptionDraft.trim()
                                )
                                .addOnSuccessListener {
                                    savingDescription = false
                                    showDescriptionEditor = false
                                }
                                .addOnFailureListener {
                                    savingDescription = false
                                    descriptionError =
                                        it.localizedMessage
                                            ?: "Unable to save description."
                                }
                        }
                    }
                ) {
                    Text(
                        if (savingDescription) "Saving…" else "Save"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !savingDescription,
                    onClick = { showDescriptionEditor = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(2.5.dp))

        HorizontalDivider(
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            thickness = 2.dp
        )

        Spacer(modifier = Modifier.height(7.5.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 30.dp,
                border = BorderStroke(2.dp, purple)
            ) {
                Image(
                    painter = painterResource(R.drawable.default_pfp),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
            }

            Column(
                modifier = Modifier
                    .height(120.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = songName,
                    fontFamily = fredoka,
                    fontSize = 24.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val filledStars = kotlin.math.floor(songRating + 0.5)
                        .toInt()
                        .coerceIn(0, 5)

                    repeat(5) { index ->
                        Text(
                            text = if (index < filledStars) "★" else "☆",
                            color = if (index < filledStars) {
                                Color(0xFFFFB300)
                            } else {
                                Color.LightGray
                            },
                            fontSize = 18.sp
                        )
                    }

                    Text(
                        text = if (ratingCount > 0) {
                            String.format(
                                java.util.Locale.US,
                                "%.1f/5.0",
                                songRating
                            )
                        } else {
                            "0.0/5.0"
                        },
                        fontFamily = fredoka,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Text(
                        text = if (ratingCount > 0) {
                            "($ratingCount ratings)"
                        } else {
                            "(No ratings)"
                        },
                        fontFamily = fredoka,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        if (currentUid == null) {
                            ratingError = "Please log in to rate this song."
                        } else {
                            selectedRating = myRating
                            ratingError = null
                            showRatingDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = purple
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    )
                ) {
                    Text(
                        text = if (myRating > 0) "Change rating" else "Rate",
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Description",
                fontFamily = fredoka,
                fontSize = 24.sp
            )

            if (canEditDescription) {
                TextButton(
                    onClick = {
                        descriptionDraft = if (
                            songDescription == "No description provided."
                        ) {
                            ""
                        } else {
                            songDescription
                        }
                        descriptionError = null
                        showDescriptionEditor = true
                    }
                ) {
                    Text("Edit", color = purple)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, purple),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = songLoadError ?: songDescription,
                fontFamily = fredoka,
                fontSize = 13.sp,
                color = Color(0xFF444444),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                color = purple,
                modifier = Modifier.weight(1f),
                border = BorderStroke(2.dp, purple),
                onClick = { openPanel = "Comments" }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        vertical = 12.dp,
                        horizontal = 8.dp
                    )
                ) {
                    Image(
                        painter = painterResource(R.drawable.comment),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Comments",
                        fontFamily = fredoka,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                color = purple,
                modifier = Modifier.weight(1f),
                border = BorderStroke(2.dp, purple),
                onClick = { openPanel = "Leaderboard" }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        vertical = 12.dp,
                        horizontal = 8.dp
                    )
                ) {
                    Image(
                        painter = painterResource(R.drawable.leaderboard),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Leaderboard",
                        fontFamily = fredoka,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class SongComment(
    val id: String,
    val userId: String,
    val text: String
)

private data class LeaderboardMap(
    val id: String,
    val title: String
)

private data class RankedScore(
    val userId: String,
    val score: Long
)

@Composable
private fun SongFeaturePopup(
    songId: String,
    leaderboard: Boolean,
    onClose: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val purple = Color(0xFF5D36EB)

    var maps by remember(songId) {
        mutableStateOf<List<LeaderboardMap>>(emptyList())
    }
    var selectedMap by remember(songId) { mutableStateOf("") }

    var comments by remember(songId) {
        mutableStateOf<List<SongComment>>(emptyList())
    }
    var scores by remember(songId) {
        mutableStateOf<List<RankedScore>>(emptyList())
    }

    val names = remember(songId) {
        mutableStateMapOf<String, String>()
    }

    var error by remember(songId) { mutableStateOf<String?>(null) }
    var mapError by remember(songId) { mutableStateOf<String?>(null) }
    var loading by remember(songId) { mutableStateOf(false) }
    var mapsLoading by remember(songId) { mutableStateOf(leaderboard) }
    var draft by remember(songId) { mutableStateOf("") }
    var sending by remember(songId) { mutableStateOf(false) }
    var postError by remember(songId) {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(songId, leaderboard) {
        var active = true
        mapsLoading = leaderboard && songId.isNotBlank()
        mapError = null

        val listener = if (leaderboard && songId.isNotBlank()) {
            db.collection("beatmaps")
                .whereEqualTo("songId", songId)
                .addSnapshotListener { snapshot, exception ->
                    if (active) {
                        mapsLoading = false
                        mapError = exception?.localizedMessage

                        if (exception == null) {
                            maps = snapshot?.documents.orEmpty().map { doc ->
                                LeaderboardMap(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "Beatmap"
                                )
                            }

                            if (maps.none { it.id == selectedMap }) {
                                selectedMap = maps.firstOrNull()?.id.orEmpty()
                            }
                        }
                    }
                }
        } else {
            null
        }

        onDispose {
            active = false
            listener?.remove()
        }
    }

    DisposableEffect(songId, leaderboard, selectedMap) {
        var active = true
        comments = emptyList()
        scores = emptyList()
        error = null

        fun loadUsername(uid: String) {
            if (uid.isBlank() || uid in names) return

            names[uid] = "User"

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { user ->
                    if (active) {
                        names[uid] = user.getString("username")
                            ?.takeIf { it.isNotBlank() } ?: "User"
                    }
                }
        }

        val query: Query? = when {
            songId.isBlank() -> null

            !leaderboard -> db.collection("songs")
                .document(songId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)

            selectedMap.isNotBlank() -> db.collection("beatmaps")
                .document(selectedMap)
                .collection("scores")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(50)

            else -> null
        }

        loading = query != null

        val listener = query?.addSnapshotListener { snapshot, exception ->
            if (active) {
                loading = false
                error = exception?.localizedMessage

                if (exception == null) {
                    try {
                        if (leaderboard) {
                            scores = snapshot?.documents.orEmpty().map { doc ->
                                loadUsername(doc.id)

                                RankedScore(
                                    userId = doc.id,
                                    score = doc.getLong("score") ?: 0L
                                )
                            }
                        } else {
                            comments = snapshot?.documents.orEmpty().map { doc ->
                                val uid = doc.getString("userId").orEmpty()
                                loadUsername(uid)

                                SongComment(
                                    id = doc.id,
                                    userId = uid,
                                    text = doc.getString("text").orEmpty()
                                )
                            }
                        }
                    } catch (e: Exception) {
                        error = e.localizedMessage ?: "Unable to load entries."
                    }
                }
            }
        }

        onDispose {
            active = false
            listener?.remove()
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF7F3FF),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (leaderboard) "Leaderboard" else "Comments",
                        color = purple,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                }

                if (songId.isBlank()) {
                    Text("Open a song through Search → More details.")
                }

                (mapError ?: error)?.let { message ->
                    Text(message, color = Color.Red)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (leaderboard) {
                        items(maps, key = { it.id }) { map ->
                            OutlinedButton(
                                onClick = { selectedMap = map.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (selectedMap == map.id) {
                                        "✓ ${map.title}"
                                    } else {
                                        map.title
                                    }
                                )
                            }
                        }
                    }

                    if (loading || mapsLoading) {
                        item {
                            Text("Loading…")
                        }
                    } else if (
                        error == null &&
                        mapError == null &&
                        songId.isNotBlank()
                    ) {
                        if (!leaderboard && comments.isEmpty()) {
                            item {
                                Text("No comments yet.")
                            }
                        } else if (leaderboard && maps.isEmpty()) {
                            item {
                                Text("No beatmaps for this song yet.")
                            }
                        } else if (leaderboard && scores.isEmpty()) {
                            item {
                                Text("No scores yet.")
                            }
                        }
                    }

                    if (leaderboard) {
                        items(
                            count = scores.size,
                            key = { index -> scores[index].userId }
                        ) { index ->
                            val entry = scores[index]

                            Surface(
                                color = Color(0xFFE9E2FF),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = names[entry.userId] ?: "User",
                                        color = purple,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("#${index + 1} • ${entry.score} points")
                                }
                            }
                        }
                    } else {
                        items(comments, key = { it.id }) { comment ->
                            Surface(
                                color = Color(0xFFE9E2FF),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = names[comment.userId] ?: "User",
                                        color = purple,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(comment.text)
                                }
                            }
                        }
                    }
                }

                if (!leaderboard && songId.isNotBlank()) {
                    postError?.let { message ->
                        Text(message, color = Color.Red)
                    }

                    OutlinedTextField(
                        value = draft,
                        onValueChange = { value ->
                            if (value.length <= 1000) {
                                draft = value
                            }
                        },
                        label = { Text("Write a comment") },
                        enabled = !sending,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        enabled = draft.isNotBlank() && !sending,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = purple
                        ),
                        onClick = {
                            val uid = FirebaseAuth.getInstance()
                                .currentUser?.uid
                            val message = draft.trim()

                            if (uid == null) {
                                postError = "Please log in first."
                            } else if (message.isNotEmpty()) {
                                sending = true
                                postError = null

                                db.collection("songs")
                                    .document(songId)
                                    .collection("comments")
                                    .add(
                                        mapOf(
                                            "userId" to uid,
                                            "text" to message,
                                            "createdAt" to
                                                    FieldValue.serverTimestamp()
                                        )
                                    )
                                    .addOnSuccessListener {
                                        draft = ""
                                        sending = false
                                    }
                                    .addOnFailureListener { exception ->
                                        postError = exception.localizedMessage
                                            ?: "Unable to post comment."
                                        sending = false
                                    }
                            }
                        }
                    ) {
                        Text(
                            text = if (sending) "Posting…" else "Post comment"
                        )
                    }
                }
            }
        }
    }
}