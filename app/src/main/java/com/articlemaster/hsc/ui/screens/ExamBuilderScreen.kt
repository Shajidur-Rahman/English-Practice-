package com.articlemaster.hsc.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.ExamConfig
import com.articlemaster.hsc.model.ExamTopicFilter
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.SelectionMode
import com.articlemaster.hsc.model.SessionType
import com.articlemaster.hsc.model.TimerMode
import com.articlemaster.hsc.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExamBuilderScreen(
    state: MainUiState,
    previewCountForConfig: (ExamConfig) -> Int,
    onBack: () -> Unit,
    onLaunch: (ExamConfig) -> Unit
) {
    val prefs = state.progress.preferences
    val initialTopic = if (state.activeTopic == GrammarTopic.ARTICLE) ExamTopicFilter.ARTICLE else ExamTopicFilter.PREPOSITION
    var topicFilter by remember { mutableStateOf(initialTopic) }
    var sessionType by remember { mutableStateOf(SessionType.EXAM) }
    var selectionMode by remember { mutableStateOf(SelectionMode.RANGE) }
    var rangeStart by remember { mutableIntStateOf(1) }
    var rangeEnd by remember { mutableIntStateOf(10) }
    var randomCount by remember { mutableIntStateOf(5) }
    var customKeys by remember { mutableStateOf(emptySet<String>()) }
    var shuffleOrder by remember { mutableStateOf(false) }

    var timerMode by remember { mutableStateOf(TimerMode.TOTAL_MINUTES) }
    var totalMinutes by remember { mutableIntStateOf(15) }
    var perPassageSeconds by remember { mutableIntStateOf(90) }
    var autoSubmitOnTimeout by remember { mutableStateOf(false) }

    var instantFeedback by remember { mutableStateOf(false) }
    var showWordBankHint by remember { mutableStateOf(false) }
    var strictCase by remember { mutableStateOf(prefs.defaultStrictCase) }
    var hscHalfMark by remember { mutableStateOf(prefs.defaultHscHalfMark) }
    var negativePenalty by remember { mutableDoubleStateOf(0.0) }

    val pool = remember(state.exercises, topicFilter) {
        when (topicFilter) {
            ExamTopicFilter.ARTICLE -> state.articleExercises
            ExamTopicFilter.PREPOSITION -> state.prepositionExercises
            ExamTopicFilter.BOTH -> state.exercises
        }
    }
    val maxId = when (topicFilter) {
        ExamTopicFilter.ARTICLE -> state.articleExercises.size.coerceAtLeast(1)
        ExamTopicFilter.PREPOSITION -> state.prepositionExercises.size.coerceAtLeast(1)
        ExamTopicFilter.BOTH -> maxOf(state.articleExercises.size, state.prepositionExercises.size).coerceAtLeast(1)
    }

    val safeRangeStart = rangeStart.coerceIn(1, maxId)
    val safeRangeEnd = rangeEnd.coerceIn(1, maxId)
    val safeRandom = randomCount.coerceIn(1, pool.size.coerceAtLeast(1))

    val builtConfig = ExamConfig(
        title = when (selectionMode) {
            SelectionMode.RANGE -> "${topicFilter.label} ${sessionType.label} (#${minOf(safeRangeStart, safeRangeEnd)}–#${maxOf(safeRangeStart, safeRangeEnd)})"
            SelectionMode.RANDOM -> "${topicFilter.label} ${sessionType.label} ($safeRandom Random)"
            else -> "${topicFilter.label} • ${selectionMode.label}"
        },
        topicFilter = topicFilter,
        sessionType = sessionType,
        selectionMode = selectionMode,
        rangeStart = safeRangeStart,
        rangeEnd = safeRangeEnd,
        randomCount = safeRandom,
        customSelectedKeys = customKeys,
        shuffleOrder = shuffleOrder,
        timerMode = timerMode,
        totalMinutes = totalMinutes,
        perPassageSeconds = perPassageSeconds,
        autoSubmitOnTimeout = autoSubmitOnTimeout,
        instantFeedback = if (sessionType == SessionType.PRACTICE) instantFeedback else false,
        strictCapitalization = strictCase,
        hscHalfMarkScale = hscHalfMark,
        negativePenaltyPerWrong = negativePenalty,
        showWordBankHint = if (sessionType == SessionType.PRACTICE) showWordBankHint else false
    )

    val matchingCount = previewCountForConfig(builtConfig)
    val maxMarks = matchingCount * (if (hscHalfMark) 5.0 else 10.0)

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Exam & Practice Builder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "$matchingCount passages • ${matchingCount * 10} blanks",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Max score: ${AnswerEvaluator.formatScore(maxMarks)} marks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onLaunch(builtConfig) },
                        enabled = matchingCount > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (sessionType == SessionType.EXAM) "Start Exam" else "Start Practice")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Topic Choice
            item {
                SectionCard("1. Topic") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ExamTopicFilter.entries.forEachIndexed { idx, tf ->
                            SegmentedButton(
                                selected = topicFilter == tf,
                                onClick = {
                                    topicFilter = tf
                                    if (tf == ExamTopicFilter.BOTH && selectionMode == SelectionMode.RANGE) {
                                        selectionMode = SelectionMode.RANDOM
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(idx, ExamTopicFilter.entries.size)
                            ) {
                                Text(
                                    when (tf) {
                                        ExamTopicFilter.ARTICLE -> "Articles"
                                        ExamTopicFilter.PREPOSITION -> "Preps"
                                        ExamTopicFilter.BOTH -> "Mixed"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (topicFilter) {
                            ExamTopicFilter.ARTICLE -> "Articles: 112 passages (Board 1–64, Practice 65–112)."
                            ExamTopicFilter.PREPOSITION -> "Prepositions: 80 passages (Board 1–56, Practice 57–80)."
                            ExamTopicFilter.BOTH -> "Mixed Exam: Combines Articles (112) + Prepositions (80) = 192 passages!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Mode Switch
            item {
                SectionCard("2. Mode") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SessionType.entries.forEachIndexed { idx, type ->
                            SegmentedButton(
                                selected = sessionType == type,
                                onClick = {
                                    sessionType = type
                                    if (type == SessionType.PRACTICE) {
                                        instantFeedback = true
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(idx, SessionType.entries.size)
                            ) {
                                Text(type.label)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (sessionType == SessionType.EXAM) {
                            "Exam Mode: No hints; all blanks graded together on submit. Mistakes auto-save to Flash Memory!"
                        } else {
                            "Practice Mode: Instant checks, hints, solve-all & word bank available."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (sessionType == SessionType.PRACTICE) {
                        RowSwitch(
                            label = "Instant feedback while answering",
                            checked = instantFeedback,
                            onCheckedChange = { instantFeedback = it }
                        )
                        if (topicFilter != ExamTopicFilter.ARTICLE) {
                            RowSwitch(
                                label = "Show Preposition Word Bank hint by default",
                                checked = showWordBankHint,
                                onCheckedChange = { showWordBankHint = it }
                            )
                        }
                    }
                }
            }

            // 3. Passage Selection
            item {
                SectionCard("3. Choose Passages (${pool.size} available)") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SelectionMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectionMode == mode,
                                onClick = { selectionMode = mode },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    when (selectionMode) {
                        SelectionMode.RANGE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StepperBox(
                                    label = "From #",
                                    value = safeRangeStart,
                                    min = 1,
                                    max = maxId,
                                    onValueChange = { rangeStart = it },
                                    modifier = Modifier.weight(1f)
                                )
                                StepperBox(
                                    label = "To #",
                                    value = safeRangeEnd,
                                    min = 1,
                                    max = maxId,
                                    onValueChange = { rangeEnd = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            val presets = if (topicFilter == ExamTopicFilter.ARTICLE) {
                                listOf(1 to 10, 1 to 64, 65 to 112, 1 to 112)
                            } else {
                                listOf(1 to 15, 1 to 56, 57 to 80, 1 to 80)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.forEach { (a, b) ->
                                    TextButton(onClick = { rangeStart = a; rangeEnd = b }) {
                                        Text("$a–$b")
                                    }
                                }
                            }
                        }

                        SelectionMode.RANDOM -> {
                            StepperBox(
                                label = "Number of random passages",
                                value = safeRandom,
                                min = 1,
                                max = pool.size.coerceAtLeast(1),
                                onValueChange = { randomCount = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(3, 5, 10, 15, 25).forEach { n ->
                                    TextButton(onClick = { randomCount = n.coerceAtMost(pool.size) }) { Text("$n") }
                                }
                            }
                        }

                        SelectionMode.CUSTOM -> {
                            val selectedInPool = pool.count { it.key in customKeys }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Tap numbers to toggle ($selectedInPool selected)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(onClick = {
                                    customKeys = if (selectedInPool == pool.size) {
                                        customKeys - pool.map { it.key }.toSet()
                                    } else {
                                        customKeys + pool.map { it.key }.toSet()
                                    }
                                }) {
                                    Text(if (selectedInPool == pool.size) "Clear" else "Select All")
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                pool.chunked(7).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowItems.forEach { item ->
                                            val picked = item.key in customKeys
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        customKeys = if (picked) customKeys - item.key else customKeys + item.key
                                                    },
                                                color = if (picked) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    val lbl = if (topicFilter == ExamTopicFilter.BOTH) {
                                                        "${item.topic.shortLabel.first()}${item.id}"
                                                    } else {
                                                        item.id.toString()
                                                    }
                                                    Text(
                                                        lbl,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (picked) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        repeat(7 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }

                        else -> Unit
                    }

                    if (selectionMode != SelectionMode.RANDOM) {
                        RowSwitch(
                            label = "Shuffle passage order",
                            checked = shuffleOrder,
                            onCheckedChange = { shuffleOrder = it }
                        )
                    }
                }
            }

            // 4. Timer Options
            item {
                SectionCard("4. Timer") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TimerMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = timerMode == mode,
                                onClick = { timerMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(idx, TimerMode.entries.size)
                            ) {
                                Text(
                                    when (mode) {
                                        TimerMode.NONE -> "None"
                                        TimerMode.TOTAL_MINUTES -> "Total"
                                        TimerMode.PER_PASSAGE_SECONDS -> "Per Q"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    when (timerMode) {
                        TimerMode.NONE -> {
                            Text(
                                "No countdown limit. Stopwatch tracks elapsed time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TimerMode.TOTAL_MINUTES -> {
                            StepperBox(
                                label = "Total Exam Minutes",
                                value = totalMinutes,
                                min = 1,
                                max = 180,
                                onValueChange = { totalMinutes = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5, 10, 15, 30, 45).forEach { m ->
                                    TextButton(onClick = { totalMinutes = m }) { Text("${m}m") }
                                }
                            }
                        }
                        TimerMode.PER_PASSAGE_SECONDS -> {
                            StepperBox(
                                label = "Seconds per Passage",
                                value = perPassageSeconds,
                                min = 20,
                                max = 600,
                                step = 15,
                                onValueChange = { perPassageSeconds = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (timerMode != TimerMode.NONE) {
                        RowSwitch(
                            label = "Auto-submit when timer expires",
                            checked = autoSubmitOnTimeout,
                            onCheckedChange = { autoSubmitOnTimeout = it }
                        )
                    }
                }
            }

            // 5. Scoring Rules
            item {
                SectionCard("5. Scoring & Precision") {
                    RowSwitch(
                        label = "HSC Marks Scale (0.5 mark/blank = 5 per passage)",
                        checked = hscHalfMark,
                        onCheckedChange = { hscHalfMark = it }
                    )
                    RowSwitch(
                        label = "Strict Capitalization check (In vs in, The vs the)",
                        checked = strictCase,
                        onCheckedChange = { strictCase = it }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Negative Marking per Wrong Blank", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.0 to "None", 0.25 to "-0.25x", 0.5 to "-0.5x").forEach { (pen, lbl) ->
                            FilterChip(
                                selected = negativePenalty == pen,
                                onClick = { negativePenalty = pen },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepperBox(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onValueChange((value - step).coerceIn(min, max)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { onValueChange((value + step).coerceIn(min, max)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        }
    }
}
