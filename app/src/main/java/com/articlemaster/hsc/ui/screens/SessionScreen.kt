package com.articlemaster.hsc.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.BLANK_KEYS
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.FlashcardEntity
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.SessionType
import com.articlemaster.hsc.model.TimerMode
import com.articlemaster.hsc.ui.ActiveSessionState
import com.articlemaster.hsc.ui.MainUiState
import com.articlemaster.hsc.ui.components.BlankAnswerPad
import com.articlemaster.hsc.ui.components.InteractiveInlinePassage
import com.articlemaster.hsc.ui.components.PassageImageCard
import com.articlemaster.hsc.ui.theme.AmberAccent
import com.articlemaster.hsc.ui.theme.AmberContainer
import com.articlemaster.hsc.ui.theme.EmeraldContainer
import com.articlemaster.hsc.ui.theme.IndigoPrimary
import com.articlemaster.hsc.ui.theme.RoseContainer
import com.articlemaster.hsc.ui.theme.RoseError
import com.articlemaster.hsc.ui.theme.TealSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionScreen(
    state: MainUiState,
    session: ActiveSessionState,
    loadBitmap: (String) -> Bitmap?,
    onSelectBlank: (String) -> Unit,
    onPickAnswer: (String?, Boolean) -> Unit,
    onAdvanceBlank: () -> Unit,
    onToggleWordBank: () -> Unit,
    onCheckPassage: () -> Unit,
    onHint: () -> Unit,
    onRevealAll: () -> Unit,
    onClearPassage: () -> Unit,
    onToggleBookmark: (String) -> Unit,
    onToggleFlag: () -> Unit,
    onTogglePause: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onSubmit: () -> Unit,
    onAbandon: () -> Unit,
    onRunOcr: (ExerciseItem) -> Unit,
    onSaveCustomText: (String, String) -> Unit,
    onPinBlankToFlashcards: (ExerciseItem, String) -> Unit
) {
    val ex = session.currentExercise ?: return
    val prefs = state.progress.preferences
    val customText = state.progress.customTexts[ex.key]
    val userAnswers = session.answersByKey[ex.key].orEmpty()
    val isBookmarked = state.progress.statsByKey[ex.key]?.bookmarked == true
    val isFlagged = ex.key in session.flaggedExerciseKeys
    val isPractice = session.config.sessionType == SessionType.PRACTICE
    val flashCardKey = FlashcardEntity.makeCardKey(ex.topic, ex.id, session.selectedBlankKey)
    val isSelectedInFlash = remember(state.flashcards, flashCardKey) {
        state.flashcards.any { it.cardKey == flashCardKey }
    }

    var showNavigator by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showSubmitConfirm by remember { mutableStateOf(false) }
    var showTextEditor by remember { mutableStateOf(false) }
    var showTextMode by remember(ex.key, customText != null) {
        mutableStateOf(customText != null && prefs.preferTextViewWhenAvailable)
    }

    BackHandler {
        showExitConfirm = true
    }

    val bitmap = remember(ex.imageAsset) { loadBitmap(ex.imageAsset) }
    val filledCount = userAnswers.size
    val totalFilledOverall = session.exercises.sumOf { session.answersByKey[it.key]?.size ?: 0 }
    val totalBlanksOverall = session.exercises.size * BLANK_KEYS.size
    val topicAccent = if (ex.topic == GrammarTopic.ARTICLE) IndigoPrimary else TealSecondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
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
                                "#${ex.id} (${session.currentIndex + 1}/${session.exercises.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "${ex.boardInfo} • p.${ex.page}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit")
                    }
                },
                actions = {
                    val timerText = when (session.config.timerMode) {
                        TimerMode.TOTAL_MINUTES -> AnswerEvaluator.formatDuration(session.remainingSeconds ?: 0)
                        TimerMode.PER_PASSAGE_SECONDS -> AnswerEvaluator.formatDuration(session.perPassageRemainingSeconds ?: 0)
                        TimerMode.NONE -> AnswerEvaluator.formatDuration(session.elapsedSeconds)
                    }
                    val lowTime = (session.remainingSeconds != null && session.remainingSeconds <= 30) ||
                        (session.perPassageRemainingSeconds != null && session.perPassageRemainingSeconds <= 15)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (lowTime) RoseContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = isPractice) { onTogglePause() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (session.isPaused) Icons.Default.PlayArrow else if (isPractice) Icons.Default.Pause else Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (lowTime) RoseError else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                timerText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (lowTime) RoseError else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(onClick = onToggleFlag, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                            contentDescription = "Flag",
                            tint = if (isFlagged) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { onToggleBookmark(ex.key) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { onJumpTo(session.currentIndex - 1) },
                        enabled = session.currentIndex > 0,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Prev")
                    }

                    FilledTonalButton(
                        onClick = { showNavigator = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("$totalFilledOverall/$totalBlanksOverall")
                    }

                    if (session.currentIndex < session.exercises.lastIndex) {
                        Button(
                            onClick = { onJumpTo(session.currentIndex + 1) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { showSubmitConfirm = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Finish")
                        }
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (session.timeExpiredNotice && !session.config.autoSubmitOnTimeout) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Time is up! You can continue reviewing or tap Finish to submit.",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            ex.note?.let { note ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            note,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            PassageImageCard(
                bitmap = bitmap,
                invertInDarkMode = prefs.invertPassageInDark,
                darkModeOverride = prefs.darkMode,
                hasExtractedText = customText != null,
                isOcrRunning = state.ocrBusyForKey == ex.key,
                onRunOcr = { onRunOcr(ex) },
                onOpenEditor = { showTextEditor = true },
                onToggleTextView = if (customText != null) {
                    { showTextMode = !showTextMode }
                } else null,
                showingTextView = showTextMode && customText != null
            )

            if (showTextMode && customText != null) {
                InteractiveInlinePassage(
                    passageText = customText,
                    topic = ex.topic,
                    userAnswers = userAnswers,
                    expectedAnswers = ex.answers,
                    selectedBlankKey = session.selectedBlankKey,
                    showFeedback = (ex.key in session.checkedExerciseKeys) ||
                        (isPractice && session.config.instantFeedback),
                    strictCapitalization = session.config.strictCapitalization,
                    onSelectBlank = onSelectBlank
                )
            }

            val blankShowFeedback = (ex.key in session.checkedExerciseKeys) ||
                (isPractice && session.config.instantFeedback && userAnswers.containsKey(session.selectedBlankKey))

            BlankAnswerPad(
                topic = ex.topic,
                exerciseKey = ex.key,
                userAnswers = userAnswers,
                expectedAnswers = ex.answers,
                selectedBlankKey = session.selectedBlankKey,
                showFeedback = blankShowFeedback,
                strictCapitalization = session.config.strictCapitalization,
                isPractice = isPractice,
                showWordBankHint = session.showWordBankHint,
                isSelectedBlankInFlashcards = isSelectedInFlash,
                onSelectBlank = onSelectBlank,
                onPickAnswer = onPickAnswer,
                onAdvanceBlank = onAdvanceBlank,
                onToggleWordBank = onToggleWordBank,
                onPinSelectedToFlashcards = { onPinBlankToFlashcards(ex, session.selectedBlankKey) }
            )

            if (isPractice) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCheckPassage,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (ex.key in session.checkedExerciseKeys) "Hide Check" else "Check ($filledCount/10)")
                    }
                    OutlinedButton(
                        onClick = onHint,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hint (${session.selectedBlankKey})")
                    }
                    OutlinedButton(
                        onClick = onRevealAll,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Solve All")
                    }
                    OutlinedButton(
                        onClick = onClearPassage,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            }
        }
    }

    if (showNavigator) {
        ModalBottomSheet(onDismissRequest = { showNavigator = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Question Navigator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Button(onClick = {
                        showNavigator = false
                        showSubmitConfirm = true
                    }) {
                        Text("Submit & Grade")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    session.exercises.chunked(5).forEachIndexed { rowIdx, chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chunk.forEachIndexed { colIdx, item ->
                                val idx = rowIdx * 5 + colIdx
                                val count = session.answersByKey[item.key]?.size ?: 0
                                val flagged = item.key in session.flaggedExerciseKeys
                                val isCur = idx == session.currentIndex

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            showNavigator = false
                                            onJumpTo(idx)
                                        },
                                    color = when {
                                        isCur -> MaterialTheme.colorScheme.primaryContainer
                                        count == BLANK_KEYS.size -> EmeraldContainer
                                        count > 0 -> AmberContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = if (isCur) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else null,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "${item.topic.shortLabel.first()}${item.id}${if (flagged) " ⚑" else ""}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            "$count/10",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            repeat(5 - chunk.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    if (showSubmitConfirm) {
        val unanswered = totalBlanksOverall - totalFilledOverall
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = false },
            title = { Text("Finish & Grade?") },
            text = {
                Text(
                    if (unanswered > 0) {
                        "You filled $totalFilledOverall of $totalBlanksOverall blanks ($unanswered unanswered). Any wrong blanks will be saved to Flash Memory. Submit now?"
                    } else {
                        "All $totalBlanksOverall blanks are filled! Ready to submit and grade?"
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitConfirm = false
                    onSubmit()
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirm = false }) { Text("Keep Working") }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Exit Session?") },
            text = { Text("Do you want to grade and save your progress so far, or discard this session?") },
            confirmButton = {
                Button(onClick = {
                    showExitConfirm = false
                    onSubmit()
                }) {
                    Text("Submit & Exit")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showExitConfirm = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        showExitConfirm = false
                        onAbandon()
                    }) {
                        Text("Discard", color = RoseError)
                    }
                }
            }
        )
    }

    if (showTextEditor) {
        var draft by remember(ex.key, customText) { mutableStateOf(customText.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showTextEditor = false },
            title = { Text("${ex.topic.label} #${ex.id} Interactive Text") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Use markers like (a) ___, (b) ___, ... (j) ___ for clickable inline blanks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveCustomText(ex.key, draft)
                    showTextMode = draft.isNotBlank()
                    showTextEditor = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextEditor = false }) { Text("Cancel") }
            }
        )
    }
}
