package com.articlemaster.hsc.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.ExamResult
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.PassageResult
import com.articlemaster.hsc.ui.MainUiState
import com.articlemaster.hsc.ui.theme.AmberAccent
import com.articlemaster.hsc.ui.theme.EmeraldContainer
import com.articlemaster.hsc.ui.theme.EmeraldSuccess
import com.articlemaster.hsc.ui.theme.IndigoPrimary
import com.articlemaster.hsc.ui.theme.RoseContainer
import com.articlemaster.hsc.ui.theme.RoseError
import com.articlemaster.hsc.ui.theme.TealSecondary

private enum class ResultFilter(val label: String) {
    ALL("All"),
    MISTAKES("Has Mistakes"),
    PERFECT("Perfect (10/10)")
}

private val ResultInvertMatrix = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -0.92f, 0f, 0f, 0f, 240f,
            0f, -0.92f, 0f, 0f, 240f,
            0f, 0f, -0.92f, 0f, 240f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    state: MainUiState,
    result: ExamResult,
    loadBitmap: (String) -> Bitmap?,
    onBackHome: () -> Unit,
    onRetryMistakes: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onPinBlankToFlashcards: (ExerciseItem, String) -> Unit
) {
    var filter by remember { mutableStateOf(ResultFilter.ALL) }
    val filteredPassages = remember(result, filter) {
        when (filter) {
            ResultFilter.ALL -> result.passageResults
            ResultFilter.MISTAKES -> result.passageResults.filter { it.correctCount < 10 }
            ResultFilter.PERFECT -> result.passageResults.filter { it.correctCount == 10 }
        }
    }
    val accuracyPct = if (result.totalBlanks > 0) (result.totalCorrect * 100 / result.totalBlanks) else 0
    val hasAnyMistake = result.passageResults.any { it.correctCount < 10 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result Summary") },
                navigationIcon = {
                    IconButton(onClick = onBackHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    result.config.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    buildString {
                                        append("Time: ${AnswerEvaluator.formatDuration(result.elapsedSeconds)}")
                                        if (result.config.strictCapitalization) append(" • Strict Case")
                                        if (result.config.hscHalfMarkScale) append(" • HSC 0.5/blank")
                                        if (result.timedOut) append(" • Timed Out")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "$accuracyPct%",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScoreBadge(
                                label = "Marks",
                                value = "${AnswerEvaluator.formatScore(result.totalScore)}/${AnswerEvaluator.formatScore(result.maxScore)}",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1.3f)
                            )
                            ScoreBadge("Correct", result.totalCorrect.toString(), EmeraldSuccess, Modifier.weight(1f))
                            ScoreBadge("Wrong", result.totalWrong.toString(), RoseError, Modifier.weight(1f))
                            ScoreBadge("Blank", result.totalUnanswered.toString(), AmberAccent, Modifier.weight(1f))
                        }

                        if (result.flashcardsAddedOrUpdated > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenFlashcards)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            Icons.Default.Style,
                                            contentDescription = null,
                                            tint = TealSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${result.flashcardsAddedOrUpdated} mistake${if (result.flashcardsAddedOrUpdated == 1) "" else "s"} saved to Flash Memory DB",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Text(
                                        "Flash Now →",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TealSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasAnyMistake) {
                                Button(
                                    onClick = onRetryMistakes,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry Wrong")
                                }
                            }
                            if (result.flashcardsAddedOrUpdated > 0) {
                                FilledTonalButton(
                                    onClick = onOpenFlashcards,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Flash Cards")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onBackHome,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Dashboard")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultFilter.entries.forEach { f ->
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(f.label) }
                            )
                        }
                    }
                }
                Text(
                    "Tip: Tap any blank box below to pin it to Flash Memory.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(filteredPassages, key = { it.exercise.key }) { pr ->
                PassageReviewCard(
                    state = state,
                    passageResult = pr,
                    loadBitmap = loadBitmap,
                    onPinBlank = { bKey -> onPinBlankToFlashcards(pr.exercise, bKey) }
                )
            }
        }
    }
}

@Composable
private fun ScoreBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PassageReviewCard(
    state: MainUiState,
    passageResult: PassageResult,
    loadBitmap: (String) -> Bitmap?,
    onPinBlank: (String) -> Unit
) {
    var expandedImage by remember { mutableStateOf(false) }
    val ex = passageResult.exercise
    val isDark = state.progress.preferences.darkMode ?: isSystemInDarkTheme()
    val invert = state.progress.preferences.invertPassageInDark
    val topicAccent = if (ex.topic == GrammarTopic.ARTICLE) IndigoPrimary else TealSecondary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedImage = !expandedImage },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = topicAccent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                ex.topic.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = topicAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            "#${ex.id} • ${ex.boardInfo}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "Score: ${passageResult.correctCount}/10 (${AnswerEvaluator.formatScore(passageResult.rawScore)}/${AnswerEvaluator.formatScore(passageResult.maxRawScore)} marks)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (passageResult.correctCount == 10) EmeraldSuccess else MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (expandedImage) "Hide" else "Passage",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        if (expandedImage) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (expandedImage) {
                val bmp = remember(ex.imageAsset) { loadBitmap(ex.imageAsset) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Passage #${ex.id}",
                        contentScale = ContentScale.FillWidth,
                        colorFilter = if (isDark && invert) ResultInvertMatrix else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 10-Blank Breakdown Grid (2 rows of 5 for Articles, 5 rows of 2 for Prepositions so multi-options fit cleanly!)
            val cols = if (ex.topic == GrammarTopic.PREPOSITION) 2 else 5
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                passageResult.blankOutcomes.chunked(cols).forEach { rowOutcomes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowOutcomes.forEach { bo ->
                            val bg = if (bo.isCorrect) EmeraldContainer else RoseContainer
                            val fg = if (bo.isCorrect) Color(0xFF065F46) else Color(0xFF991B1B)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPinBlank(bo.blankKey) },
                                shape = RoundedCornerShape(10.dp),
                                color = bg
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                    horizontalAlignment = if (cols == 2) Alignment.Start else Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            "(${bo.blankKey})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = fg
                                        )
                                        Icon(
                                            if (bo.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = fg,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Text(
                                        text = if (bo.isCorrect) {
                                            bo.expectedAnswer
                                        } else {
                                            "${bo.userAnswer ?: "—"} → ${bo.expectedAnswer}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = fg,
                                        maxLines = if (cols == 2) 2 else 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
