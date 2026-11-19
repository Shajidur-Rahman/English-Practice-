package com.articlemaster.hsc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.ui.MainUiState
import com.articlemaster.hsc.ui.theme.AmberAccent
import com.articlemaster.hsc.ui.theme.EmeraldSuccess
import com.articlemaster.hsc.ui.theme.RoseError

private enum class BrowseFilter {
    ALL, BOARD, PRACTICE, MISTAKES, BOOKMARKED, UNATTEMPTED, MASTERED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeBrowseScreen(
    state: MainUiState,
    onSelectTopic: (GrammarTopic) -> Unit,
    onBack: () -> Unit,
    onStartPassage: (ExerciseItem) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onRunBatchOcr: (GrammarTopic) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(BrowseFilter.ALL) }

    val topic = state.activeTopic
    val topicExercises = state.exercisesFor(topic)
    val statsMap = state.progress.statsByKey
    val textsMap = state.progress.customTexts
    val scannedCount = topicExercises.count { textsMap.containsKey(it.key) }

    val filtered = remember(topicExercises, statsMap, textsMap, query, activeFilter) {
        val q = query.trim().lowercase()
        topicExercises.filter { ex ->
            val st = statsMap[ex.key]
            val matchesFilter = when (activeFilter) {
                BrowseFilter.ALL -> true
                BrowseFilter.BOARD -> ex.isBoardQuestion
                BrowseFilter.PRACTICE -> !ex.isBoardQuestion
                BrowseFilter.MISTAKES -> (st?.wrongBlankKeys?.isNotEmpty()) == true
                BrowseFilter.BOOKMARKED -> st?.bookmarked == true
                BrowseFilter.UNATTEMPTED -> (st?.attempts ?: 0) == 0
                BrowseFilter.MASTERED -> st?.mastered == true
            }
            val matchesQuery = q.isEmpty() ||
                ex.id.toString() == q ||
                "#${ex.id}".contains(q) ||
                ex.boardInfo.lowercase().contains(q) ||
                "page ${ex.page}".contains(q) ||
                (textsMap[ex.key]?.lowercase()?.contains(q) == true)
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("${topic.label} (${filtered.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (scannedCount < topicExercises.size) {
                        OutlinedButton(
                            onClick = { onRunBatchOcr(topic) },
                            enabled = state.batchOcrProgress == null,
                            modifier = Modifier.padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (state.batchOcrProgress != null) {
                                    "${state.batchOcrProgress.first}/${state.batchOcrProgress.second}"
                                } else {
                                    "Scan Text"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
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
            state.batchOcrProgress?.let { (done, total) ->
                LinearProgressIndicator(
                    progress = { done.toFloat() / total.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Topic Menu Switcher
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                GrammarTopic.entries.forEachIndexed { idx, t ->
                    val count = state.exercisesFor(t).size
                    SegmentedButton(
                        selected = topic == t,
                        onClick = { onSelectTopic(t) },
                        shape = SegmentedButtonDefaults.itemShape(idx, GrammarTopic.entries.size)
                    ) {
                        Text("${t.label} ($count)", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search #, Dhaka, 2025, page...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BrowseFilter.entries) { filter ->
                    val label = when (filter) {
                        BrowseFilter.ALL -> "All (${topicExercises.size})"
                        BrowseFilter.BOARD -> if (topic == GrammarTopic.ARTICLE) "Board (1–24)" else "Board (1–56)"
                        BrowseFilter.PRACTICE -> if (topic == GrammarTopic.ARTICLE) "Practice (25–76)" else "Practice (57–80)"
                        BrowseFilter.MISTAKES -> "Mistakes"
                        BrowseFilter.BOOKMARKED -> "Bookmarked"
                        BrowseFilter.UNATTEMPTED -> "Unattempted"
                        BrowseFilter.MASTERED -> "Mastered"
                    }
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.key }) { ex ->
                    ExerciseBrowseRow(
                        exercise = ex,
                        bestCorrect = statsMap[ex.key]?.bestCorrect,
                        attempts = statsMap[ex.key]?.attempts ?: 0,
                        bookmarked = statsMap[ex.key]?.bookmarked == true,
                        mastered = statsMap[ex.key]?.mastered == true,
                        hasMistakes = (statsMap[ex.key]?.wrongBlankKeys?.isNotEmpty()) == true,
                        hasOcrText = textsMap.containsKey(ex.key),
                        onClick = { onStartPassage(ex) },
                        onToggleBookmark = { onToggleBookmark(ex.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseBrowseRow(
    exercise: ExerciseItem,
    bestCorrect: Int?,
    attempts: Int,
    bookmarked: Boolean,
    mastered: Boolean,
    hasMistakes: Boolean,
    hasOcrText: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (exercise.isBoardQuestion) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(44.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "#${exercise.id}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (exercise.isBoardQuestion) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.boardInfo,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Page ${exercise.page}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (attempts > 0 && bestCorrect != null) {
                        Text(
                            "Best: $bestCorrect/10",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (mastered) EmeraldSuccess else if (hasMistakes) RoseError else MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            "Not attempted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (hasOcrText) {
                        Text(
                            "• Text ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (mastered) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Mastered",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onToggleBookmark) {
                Icon(
                    if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (bookmarked) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Practice",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
