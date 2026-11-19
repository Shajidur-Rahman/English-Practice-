package com.articlemaster.hsc.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.FlashcardEntity
import com.articlemaster.hsc.model.FlashcardStatus
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.ReviewRating
import com.articlemaster.hsc.model.SpacedRepetitionEngine
import com.articlemaster.hsc.ui.MainUiState
import com.articlemaster.hsc.ui.components.ZoomablePassageImage
import com.articlemaster.hsc.ui.theme.AmberAccent
import com.articlemaster.hsc.ui.theme.AmberContainer
import com.articlemaster.hsc.ui.theme.EmeraldContainer
import com.articlemaster.hsc.ui.theme.EmeraldSuccess
import com.articlemaster.hsc.ui.theme.IndigoPrimary
import com.articlemaster.hsc.ui.theme.RoseContainer
import com.articlemaster.hsc.ui.theme.RoseError
import com.articlemaster.hsc.ui.theme.TealSecondary

private enum class FlashTopicFilter(val label: String) {
    ALL("All Topics"),
    ARTICLE("Articles"),
    PREPOSITION("Prepositions")
}

private enum class FlashDeckFilter(val label: String) {
    DUE("Due Now"),
    ALL_MISTAKES("All Mistakes"),
    HARDEST("Hardest (2+ Misses)"),
    CUSTOM_PINNED("Pinned & Custom"),
    MASTERED("Mastered")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FlashcardScreen(
    state: MainUiState,
    loadBitmap: (String) -> Bitmap?,
    onBack: () -> Unit,
    onReviewCard: (FlashcardEntity, ReviewRating) -> Unit,
    onAddCustomCard: (GrammarTopic, String, String, String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteCard: (String) -> Unit,
    onOpenPassage: (String) -> Unit
) {
    var topicFilter by remember { mutableStateOf(FlashTopicFilter.ALL) }
    var deckFilter by remember { mutableStateOf(FlashDeckFilter.ALL_MISTAKES) }
    var search by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNoteCard by remember { mutableStateOf<FlashcardEntity?>(null) }

    // Active flash study queue (cardKeys)
    var activeStudyQueue by remember { mutableStateOf<List<String>?>(null) }
    var studyCursor by remember { mutableIntStateOf(0) }
    var reviewedInSession by remember { mutableIntStateOf(0) }

    val now = System.currentTimeMillis()
    val cardsByKey = remember(state.flashcards) { state.flashcards.associateBy { it.cardKey } }

    val topicCards = remember(state.flashcards, topicFilter) {
        when (topicFilter) {
            FlashTopicFilter.ALL -> state.flashcards
            FlashTopicFilter.ARTICLE -> state.flashcards.filter { it.topic == GrammarTopic.ARTICLE }
            FlashTopicFilter.PREPOSITION -> state.flashcards.filter { it.topic == GrammarTopic.PREPOSITION }
        }
    }

    val dueCards = remember(topicCards, now) { topicCards.filter { it.isDue(now) } }
    val masteredCount = remember(topicCards) { topicCards.count { it.status == FlashcardStatus.MASTERED } }
    val learningCount = topicCards.size - masteredCount

    val filteredCards = remember(topicCards, deckFilter, search, now) {
        val q = search.trim().lowercase()
        topicCards.filter { card ->
            val matchDeck = when (deckFilter) {
                FlashDeckFilter.DUE -> card.isDue(now)
                FlashDeckFilter.ALL_MISTAKES -> true
                FlashDeckFilter.HARDEST -> card.mistakeCount >= 2
                FlashDeckFilter.CUSTOM_PINNED -> card.isManual || card.isCustomCard
                FlashDeckFilter.MASTERED -> card.status == FlashcardStatus.MASTERED
            }
            val matchQuery = q.isEmpty() ||
                card.correctAnswer.lowercase().contains(q) ||
                (card.lastWrongAnswer?.lowercase()?.contains(q) == true) ||
                card.boardInfo.lowercase().contains(q) ||
                card.userNote.lowercase().contains(q) ||
                (card.customPrompt?.lowercase()?.contains(q) == true) ||
                "#${card.exerciseId}".contains(q) ||
                "(${card.blankKey})".contains(q)
            matchDeck && matchQuery
        }
    }

    // ==================== ACTIVE STUDY SESSION VIEW ====================
    val queue = activeStudyQueue
    if (queue != null) {
        val currentKey = queue.getOrNull(studyCursor)
        val currentCard = currentKey?.let { cardsByKey[it] }

        if (currentCard == null) {
            // Study Session Complete Summary
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Flash Session Complete") },
                        navigationIcon = {
                            IconButton(onClick = { activeStudyQueue = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Great Memory Workout!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "You reviewed $reviewedInSession flashcard${if (reviewedInSession == 1) "" else "s"} and updated your Spaced Repetition schedule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { activeStudyQueue = null },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back to Flash Memory")
                    }
                }
            }
            return
        }

        FlashStudyView(
            card = currentCard,
            cardIndex = studyCursor,
            totalInQueue = queue.size,
            state = state,
            loadBitmap = loadBitmap,
            onExit = { activeStudyQueue = null },
            onEditNote = { editingNoteCard = currentCard },
            onOpenPassage = {
                if (!currentCard.isCustomCard) {
                    activeStudyQueue = null
                    onOpenPassage("${currentCard.topic.name}_${currentCard.exerciseId}")
                }
            },
            onRate = { rating ->
                onReviewCard(currentCard, rating)
                reviewedInSession += 1
                if (rating == ReviewRating.AGAIN) {
                    // Re-queue this card at the end of current session so user drills it until remembered!
                    activeStudyQueue = queue + currentCard.cardKey
                }
                studyCursor += 1
            }
        )
    } else {
        // ==================== FLASH MEMORY DECK OVERVIEW ====================
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Flash Memory", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "SQLite Spaced-Repetition Mistake Bank",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add custom flashcard")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Topic Switcher
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        FlashTopicFilter.entries.forEachIndexed { idx, tf ->
                            SegmentedButton(
                                selected = topicFilter == tf,
                                onClick = { topicFilter = tf },
                                shape = SegmentedButtonDefaults.itemShape(idx, FlashTopicFilter.entries.size)
                            ) {
                                Text(
                                    when (tf) {
                                        FlashTopicFilter.ALL -> "All"
                                        FlashTopicFilter.ARTICLE -> "Articles"
                                        FlashTopicFilter.PREPOSITION -> "Preps"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Summary & Quick Launch Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FlashStatBox("Due Now", dueCards.size.toString(), RoseError, Modifier.weight(1f))
                                FlashStatBox("Learning", learningCount.toString(), AmberAccent, Modifier.weight(1f))
                                FlashStatBox("Mastered", masteredCount.toString(), EmeraldSuccess, Modifier.weight(1f))
                                FlashStatBox("Total", topicCards.size.toString(), IndigoPrimary, Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (dueCards.isNotEmpty()) {
                                            studyCursor = 0
                                            reviewedInSession = 0
                                            activeStudyQueue = dueCards.map { it.cardKey }
                                        }
                                    },
                                    enabled = dueCards.isNotEmpty(),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Flash Due (${dueCards.size})", fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        if (filteredCards.isNotEmpty()) {
                                            studyCursor = 0
                                            reviewedInSession = 0
                                            activeStudyQueue = filteredCards.shuffled().map { it.cardKey }
                                        }
                                    },
                                    enabled = filteredCards.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cram (${filteredCards.size})")
                                }
                            }
                        }
                    }
                }

                // Deck Filter Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FlashDeckFilter.entries) { df ->
                            FilterChip(
                                selected = deckFilter == df,
                                onClick = { deckFilter = df },
                                label = { Text(df.label) }
                            )
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search answer, passage #, board, note...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { search = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (filteredCards.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Style,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    if (state.flashcards.isEmpty()) "No flashcards yet" else "No cards match this filter",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Any wrong blank during Article or Preposition practice/exams is automatically stored here in your SQLite Flash Memory database — or tap + to create your own card!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredCards, key = { it.cardKey }) { card ->
                        FlashcardListRow(
                            card = card,
                            nowMs = now,
                            onFlashSingle = {
                                studyCursor = 0
                                reviewedInSession = 0
                                activeStudyQueue = listOf(card.cardKey)
                            },
                            onEditNote = { editingNoteCard = card },
                            onDelete = { onDeleteCard(card.cardKey) }
                        )
                    }
                }

                item { Spacer(Modifier.height(70.dp)) }
            }
        }
    }

    // Add Custom Card Dialog
    if (showAddDialog) {
        var newTopic by remember { mutableStateOf(GrammarTopic.PREPOSITION) }
        var prompt by remember { mutableStateOf("") }
        var answer by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Flashcard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        GrammarTopic.entries.forEachIndexed { idx, t ->
                            SegmentedButton(
                                selected = newTopic == t,
                                onClick = { newTopic = t },
                                shape = SegmentedButtonDefaults.itemShape(idx, GrammarTopic.entries.size)
                            ) {
                                Text(t.label)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Front / Prompt") },
                        placeholder = { Text("e.g. He is indifferent ___ his studies.") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("Correct Answer") },
                        placeholder = { Text("e.g. to  (or of/for)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Memory Tip / Rule (Optional)") },
                        placeholder = { Text("e.g. indifferent always takes 'to'") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomCard(newTopic, prompt, answer, note)
                        showAddDialog = false
                    },
                    enabled = prompt.isNotBlank() && answer.isNotBlank()
                ) {
                    Text("Save Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Note Dialog
    editingNoteCard?.let { card ->
        var noteDraft by remember(card.cardKey) { mutableStateOf(card.userNote) }
        AlertDialog(
            onDismissRequest = { editingNoteCard = null },
            title = { Text("Personal Memory Note") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text("Mnemonic / Rule Tip") },
                    placeholder = { Text("Why does this answer apply?") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateNote(card.cardKey, noteDraft)
                    editingNoteCard = null
                }) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteCard = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashStudyView(
    card: FlashcardEntity,
    cardIndex: Int,
    totalInQueue: Int,
    state: MainUiState,
    loadBitmap: (String) -> Bitmap?,
    onExit: () -> Unit,
    onEditNote: () -> Unit,
    onOpenPassage: () -> Unit,
    onRate: (ReviewRating) -> Unit
) {
    var flipped by remember(card.cardKey, cardIndex) { mutableStateOf(false) }
    var selfTestInput by remember(card.cardKey, cardIndex) { mutableStateOf("") }
    var showLastMistakeHint by remember(card.cardKey, cardIndex) { mutableStateOf(false) }
    val prefs = state.progress.preferences
    val bitmap = remember(card.imageAsset) { card.imageAsset?.let { loadBitmap(it) } }
    val topicAccent = if (card.topic == GrammarTopic.ARTICLE) IndigoPrimary else TealSecondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Flash Review (${cardIndex + 1}/$totalInQueue)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "${card.topic.label} • ${if (card.isCustomCard) "Custom Card" else "Passage #${card.exerciseId} (${card.blankKey})"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Review")
                    }
                },
                actions = {
                    IconButton(onClick = onEditNote) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Note")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { (cardIndex + 1).toFloat() / totalInQueue.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Front Context Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, topicAccent.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = topicAccent.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    card.topic.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = topicAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (!card.isCustomCard) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "Blank (${card.blankKey})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (card.mistakeCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RoseContainer
                            ) {
                                Text(
                                    "Missed ${card.mistakeCount}×",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        if (card.isCustomCard) "Custom Flashcard" else "Passage #${card.exerciseId} • ${card.boardInfo}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (card.isCustomCard && !card.customPrompt.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                card.customPrompt,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else if (bitmap != null) {
                        ZoomablePassageImage(
                            bitmap = bitmap,
                            invertInDarkMode = prefs.invertPassageInDark,
                            darkModeOverride = prefs.darkMode,
                            minHeight = 140.dp,
                            maxHeight = 290.dp
                        )
                    }

                    if (!flipped) {
                        // Optional Self-Test before flipping
                        if (card.topic == GrammarTopic.ARTICLE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("a", "an", "the", "×").forEach { tok ->
                                    val sel = selfTestInput.equals(tok, ignoreCase = true)
                                    FilledTonalButton(
                                        onClick = {
                                            selfTestInput = tok
                                            flipped = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text(tok, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = selfTestInput,
                                onValueChange = { selfTestInput = it },
                                label = { Text("Quick self-test (optional)") },
                                placeholder = { Text("Type your recall before flipping...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!card.lastWrongAnswer.isNullOrBlank()) {
                            TextButton(
                                onClick = { showLastMistakeHint = !showLastMistakeHint },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (showLastMistakeHint) {
                                        "Previous mistake: \"${card.lastWrongAnswer}\""
                                    } else {
                                        "Tap to peek at your previous mistake"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberAccent
                                )
                            }
                        }

                        Button(
                            onClick = { flipped = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Show Answer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Back Answer Reveal + SM-2 Rating Controls
            if (flipped) {
                val testedCorrect = if (selfTestInput.isNotBlank()) {
                    AnswerEvaluator.isCorrect(selfTestInput, card.correctAnswer, false, card.topic)
                } else null

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (testedCorrect) {
                            true -> EmeraldContainer
                            false -> RoseContainer
                            null -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (!card.isCustomCard) "Verified Answer for (${card.blankKey})" else "Correct Answer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            card.correctAnswer,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF065F46)
                        )

                        if (testedCorrect != null) {
                            Text(
                                if (testedCorrect) {
                                    "Your recall \"$selfTestInput\" is CORRECT!"
                                } else {
                                    "Your recall \"$selfTestInput\" did not match."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (testedCorrect) Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                        } else if (!card.lastWrongAnswer.isNullOrBlank()) {
                            Text(
                                "Last mistake recorded: ${card.lastWrongAnswer}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B)
                            )
                        }

                        if (card.userNote.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Note: ${card.userNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onEditNote) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (card.userNote.isBlank()) "Add Memory Tip" else "Edit Note")
                            }
                            if (!card.isCustomCard) {
                                TextButton(onClick = onOpenPassage) {
                                    Text("Practice Full Passage")
                                }
                            }
                        }
                    }
                }

                Text(
                    "How well did you remember this?",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val buttons = listOf(
                        Triple(ReviewRating.AGAIN, RoseError, RoseContainer),
                        Triple(ReviewRating.HARD, AmberAccent, AmberContainer),
                        Triple(ReviewRating.GOOD, TealSecondary, MaterialTheme.colorScheme.secondaryContainer),
                        Triple(ReviewRating.EASY, EmeraldSuccess, EmeraldContainer)
                    )
                    buttons.forEach { (rating, fg, bg) ->
                        val nextLabel = SpacedRepetitionEngine.previewIntervalLabel(card, rating)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onRate(rating) },
                            shape = RoundedCornerShape(12.dp),
                            color = bg,
                            border = BorderStroke(1.dp, fg.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    rating.label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = fg
                                )
                                Text(
                                    nextLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashStatBox(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = tint)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun FlashcardListRow(
    card: FlashcardEntity,
    nowMs: Long,
    onFlashSingle: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit
) {
    val topicTint = if (card.topic == GrammarTopic.ARTICLE) IndigoPrimary else TealSecondary
    val due = card.isDue(nowMs)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFlashSingle),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = topicTint.copy(alpha = 0.14f),
                modifier = Modifier.size(width = 48.dp, height = 44.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        card.topic.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = topicTint
                    )
                    Text(
                        if (card.isCustomCard) "Cust" else "#${card.exerciseId}(${card.blankKey})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (!card.lastWrongAnswer.isNullOrBlank()) {
                            "${card.lastWrongAnswer} → ${card.correctAnswer}"
                        } else {
                            "Key: ${card.correctAnswer}"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (card.mistakeCount > 0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = RoseContainer) {
                            Text(
                                "×${card.mistakeCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            card.status == FlashcardStatus.MASTERED -> EmeraldContainer
                            due -> AmberContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when {
                                card.status == FlashcardStatus.MASTERED -> "Mastered"
                                due -> "Due"
                                else -> "${card.intervalDays}d"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                card.status == FlashcardStatus.MASTERED -> Color(0xFF065F46)
                                due -> Color(0xFF92400E)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = card.customPrompt ?: card.boardInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (card.userNote.isNotBlank()) {
                    Text(
                        text = "Tip: ${card.userNote}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onEditNote, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Note", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(16.dp))
            }
        }
    }
}
