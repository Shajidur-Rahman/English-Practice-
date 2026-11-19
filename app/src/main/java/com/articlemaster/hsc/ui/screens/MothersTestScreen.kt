package com.articlemaster.hsc.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import com.articlemaster.hsc.model.BLANK_KEYS
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.ui.MainUiState
import com.articlemaster.hsc.ui.components.ZoomablePassageImage
import com.articlemaster.hsc.ui.theme.EmeraldContainer
import com.articlemaster.hsc.ui.theme.EmeraldSuccess
import com.articlemaster.hsc.ui.theme.IndigoPrimary
import com.articlemaster.hsc.ui.theme.RoseContainer
import com.articlemaster.hsc.ui.theme.RoseError
import com.articlemaster.hsc.ui.theme.TealSecondary

private enum class OralMark { NONE, WRONG, CORRECT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MothersTestScreen(
    state: MainUiState,
    loadBitmap: (String) -> Bitmap?,
    onSelectTopic: (GrammarTopic) -> Unit,
    onBack: () -> Unit,
    onSaveMistakesToFlashcards: (ExerciseItem, List<String>) -> Unit
) {
    val topic = state.activeTopic
    val topicList = state.exercisesFor(topic)
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember(topic) { mutableStateOf<Int?>(null) }

    val currentIndex = selectedIndex
    if (currentIndex != null && currentIndex in topicList.indices) {
        BackHandler { selectedIndex = null }
        MothersAnswerDetailView(
            exercise = topicList[currentIndex],
            index = currentIndex,
            total = topicList.size,
            state = state,
            loadBitmap = loadBitmap,
            onBackToList = { selectedIndex = null },
            onPrev = { if (currentIndex > 0) selectedIndex = currentIndex - 1 },
            onNext = { if (currentIndex < topicList.lastIndex) selectedIndex = currentIndex + 1 },
            onSaveMistakesToFlashcards = onSaveMistakesToFlashcards
        )
    } else {
        val filtered = remember(topicList, query) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) topicList
            else topicList.filter { ex ->
                ex.id.toString() == q ||
                    "#${ex.id}".contains(q) ||
                    ex.boardInfo.lowercase().contains(q) ||
                    "page ${ex.page}".contains(q)
            }
        }
        val accent = if (topic == GrammarTopic.ARTICLE) IndigoPrimary else TealSecondary

        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Mother's Test",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                "Clean Oral Answer Sheet • Articles & Prepositions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    GrammarTopic.entries.forEachIndexed { idx, t ->
                        val count = state.exercisesFor(t).size
                        SegmentedButton(
                            selected = topic == t,
                            onClick = { onSelectTopic(t) },
                            shape = SegmentedButtonDefaults.itemShape(idx, GrammarTopic.entries.size)
                        ) {
                            Text(
                                "${t.label} ($count)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Jump to #1..${topicList.size} or Board...", maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.key }) { ex ->
                        val realIdx = topicList.indexOfFirst { it.key == ex.key }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (realIdx >= 0) selectedIndex = realIdx },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = accent.copy(alpha = 0.14f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "#${ex.id}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = accent
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ex.boardInfo,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${ex.topic.label} • Page ${ex.page}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open Answers",
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MothersAnswerDetailView(
    exercise: ExerciseItem,
    index: Int,
    total: Int,
    state: MainUiState,
    loadBitmap: (String) -> Bitmap?,
    onBackToList: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSaveMistakesToFlashcards: (ExerciseItem, List<String>) -> Unit
) {
    var showMagicSolutionPic by remember(exercise.key) { mutableStateOf(false) }
    var showPassagePic by remember(exercise.key) { mutableStateOf(false) }
    var marks by remember(exercise.key) { mutableStateOf<Map<String, OralMark>>(emptyMap()) }

    val prefs = state.progress.preferences
    val accent = if (exercise.topic == GrammarTopic.ARTICLE) IndigoPrimary else TealSecondary
    val leftKeys = BLANK_KEYS.take(5)
    val rightKeys = BLANK_KEYS.drop(5)

    val wrongKeys = marks.filterValues { it == OralMark.WRONG }.keys.sorted()
    val correctCount = marks.count { it.value == OralMark.CORRECT }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "${exercise.topic.label} #${exercise.id}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1
                        )
                        Text(
                            "${exercise.boardInfo} • p.${exercise.page}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToList) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All List")
                    }
                },
                actions = {
                    if (marks.isNotEmpty()) {
                        IconButton(onClick = { marks = emptyMap() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear marks")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 5.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onPrev,
                        enabled = index > 0,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Before", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                    }

                    Text(
                        "${index + 1} / $total",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        maxLines = 1
                    )

                    Button(
                        onClick = onNext,
                        enabled = index < total - 1,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Next", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tap any answer to mark ✗ / ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (marks.isNotEmpty()) {
                    Text(
                        "✓ $correctCount  ✗ ${wrongKeys.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
            }

            // 5 LEFT (a-e) & 5 RIGHT (f-j) ANSWER GRID — Tuned for 360dp Samsung phones!
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (rowIdx in 0 until 5) {
                    val leftKey = leftKeys[rowIdx]
                    val rightKey = rightKeys[rowIdx]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MotherAnswerTile(
                            blankKey = leftKey,
                            answer = exercise.answers[leftKey] ?: "",
                            mark = marks[leftKey] ?: OralMark.NONE,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val next = when (marks[leftKey] ?: OralMark.NONE) {
                                    OralMark.NONE -> OralMark.WRONG
                                    OralMark.WRONG -> OralMark.CORRECT
                                    OralMark.CORRECT -> OralMark.NONE
                                }
                                marks = if (next == OralMark.NONE) marks - leftKey else marks + (leftKey to next)
                            }
                        )
                        MotherAnswerTile(
                            blankKey = rightKey,
                            answer = exercise.answers[rightKey] ?: "",
                            mark = marks[rightKey] ?: OralMark.NONE,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val next = when (marks[rightKey] ?: OralMark.NONE) {
                                    OralMark.NONE -> OralMark.WRONG
                                    OralMark.WRONG -> OralMark.CORRECT
                                    OralMark.CORRECT -> OralMark.NONE
                                }
                                marks = if (next == OralMark.NONE) marks - rightKey else marks + (rightKey to next)
                            }
                        )
                    }
                }
            }

            if (wrongKeys.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { onSaveMistakesToFlashcards(exercise, wrongKeys) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Save ${wrongKeys.size} Mistake${if (wrongKeys.size == 1) "" else "s"} to Flash Memory",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // MAGIC BUTTON + Optional Passage Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showMagicSolutionPic = !showMagicSolutionPic },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showMagicSolutionPic) Color(0xFF6D28D9) else Color(0xFF7C3AED),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.45f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (showMagicSolutionPic) "Hide Real Pic" else "Magic • Real Book",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { showPassagePic = !showPassagePic },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (showPassagePic) "Hide Q" else "Passage",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }

            AnimatedVisibility(visible = showMagicSolutionPic) {
                val solBmp = remember(exercise.solutionImageAsset) { loadBitmap(exercise.solutionImageAsset) }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, Color(0xFF7C3AED).copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "✨ Real Printed Book Solution — #${exercise.id}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF7C3AED)
                        )
                        Text(
                            "Blue box highlights #${exercise.id}. Tap ⛶ for fullscreen zoom.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ZoomablePassageImage(
                            bitmap = solBmp,
                            invertInDarkMode = prefs.invertPassageInDark,
                            darkModeOverride = prefs.darkMode,
                            minHeight = 85.dp,
                            maxHeight = 200.dp,
                            fullscreenTitle = "Real Solution #${exercise.id}"
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showPassagePic) {
                val passBmp = remember(exercise.imageAsset) { loadBitmap(exercise.imageAsset) }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Original Passage — #${exercise.id} (p.${exercise.page})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        ZoomablePassageImage(
                            bitmap = passBmp,
                            invertInDarkMode = prefs.invertPassageInDark,
                            darkModeOverride = prefs.darkMode,
                            minHeight = 130.dp,
                            maxHeight = 260.dp,
                            fullscreenTitle = "Passage #${exercise.id}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MotherAnswerTile(
    blankKey: String,
    answer: String,
    mark: OralMark,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when (mark) {
        OralMark.CORRECT -> EmeraldContainer
        OralMark.WRONG -> RoseContainer
        OralMark.NONE -> MaterialTheme.colorScheme.surface
    }
    val borderCol = when (mark) {
        OralMark.CORRECT -> EmeraldSuccess
        OralMark.WRONG -> RoseError
        OralMark.NONE -> accent.copy(alpha = 0.35f)
    }
    val answerCol = when (mark) {
        OralMark.CORRECT -> Color(0xFF065F46)
        OralMark.WRONG -> Color(0xFF991B1B)
        OralMark.NONE -> MaterialTheme.colorScheme.onSurface
    }
    val prettyAnswer = answer.replace("/", " / ")
    val fontSize = when {
        prettyAnswer.length > 22 -> 12.sp
        prettyAnswer.length > 13 -> 13.5.sp
        else -> 16.sp
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.5.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = when (mark) {
                    OralMark.CORRECT -> EmeraldSuccess
                    OralMark.WRONG -> RoseError
                    OralMark.NONE -> accent.copy(alpha = 0.14f)
                },
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (mark == OralMark.CORRECT) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else if (mark == OralMark.WRONG) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text(
                            blankKey,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = accent
                        )
                    }
                }
            }

            Text(
                text = prettyAnswer,
                fontSize = fontSize,
                lineHeight = (fontSize.value + 3.5f).sp,
                fontWeight = FontWeight.Bold,
                color = answerCol,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
