package com.articlemaster.hsc.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.ExamConfig
import com.articlemaster.hsc.model.ExamTopicFilter
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.SelectionMode
import com.articlemaster.hsc.model.SessionType
import com.articlemaster.hsc.model.TimerMode
import com.articlemaster.hsc.ui.MainUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExamBuilderScreen(
    state: MainUiState,
    previewCountForConfig: (ExamConfig) -> Int,
    onBack: () -> Unit,
    onLaunch: (ExamConfig) -> Unit
) {
    val prefs = state.progress.preferences
    val focusManager = LocalFocusManager.current
    val initialTopic = if (state.activeTopic == GrammarTopic.ARTICLE) ExamTopicFilter.ARTICLE else ExamTopicFilter.PREPOSITION
    var topicFilter by remember { mutableStateOf(initialTopic) }
    var sessionType by remember { mutableStateOf(SessionType.EXAM) }
    var selectionMode by remember { mutableStateOf(SelectionMode.RANGE) }

    var rangeStart by remember { mutableIntStateOf(1) }
    var rangeEnd by remember { mutableIntStateOf(10) }
    var rangeStartText by remember { mutableStateOf("1") }
    var rangeEndText by remember { mutableStateOf("10") }
    var showVisualRangeGrid by remember { mutableStateOf(false) }
    var pendingGridAnchor by remember { mutableStateOf<Int?>(null) }

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

    fun applyRange(start: Int, end: Int) {
        val clampedStart = start.coerceIn(1, maxId)
        val clampedEnd = end.coerceIn(1, maxId)
        rangeStart = clampedStart
        rangeEnd = clampedEnd
        rangeStartText = clampedStart.toString()
        rangeEndText = clampedEnd.toString()
    }

    val safeRangeStart = rangeStart.coerceIn(1, maxId)
    val safeRangeEnd = rangeEnd.coerceIn(1, maxId)
    val rangeLo = minOf(safeRangeStart, safeRangeEnd)
    val rangeHi = maxOf(safeRangeStart, safeRangeEnd)
    val safeRandom = randomCount.coerceIn(1, pool.size.coerceAtLeast(1))

    val builtConfig = ExamConfig(
        title = when (selectionMode) {
            SelectionMode.RANGE -> "${topicFilter.label} ${sessionType.label} (#$rangeLo–#$rangeHi)"
            SelectionMode.RANDOM -> "${topicFilter.label} ${sessionType.label} ($safeRandom Random)"
            else -> "${topicFilter.label} • ${selectionMode.label}"
        },
        topicFilter = topicFilter,
        sessionType = sessionType,
        selectionMode = selectionMode,
        rangeStart = rangeLo,
        rangeEnd = rangeHi,
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
                                    val newMax = when (tf) {
                                        ExamTopicFilter.ARTICLE -> state.articleExercises.size.coerceAtLeast(1)
                                        ExamTopicFilter.PREPOSITION -> state.prepositionExercises.size.coerceAtLeast(1)
                                        ExamTopicFilter.BOTH -> maxOf(state.articleExercises.size, state.prepositionExercises.size).coerceAtLeast(1)
                                    }
                                    val clampedS = rangeStart.coerceIn(1, newMax)
                                    val clampedE = rangeEnd.coerceIn(1, newMax)
                                    rangeStart = clampedS
                                    rangeEnd = clampedE
                                    rangeStartText = clampedS.toString()
                                    rangeEndText = clampedE.toString()
                                    pendingGridAnchor = null
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
                            // Live range banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Selected: #$rangeLo → #$rangeHi",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "$matchingCount Qs (${matchingCount * 10} blanks)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Direct instant numeric typing fields (From # -> To #)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = rangeStartText,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(3)
                                        rangeStartText = digits
                                        digits.toIntOrNull()?.let { parsed ->
                                            rangeStart = parsed.coerceIn(1, maxId)
                                        }
                                        pendingGridAnchor = null
                                    },
                                    label = { Text("From #") },
                                    placeholder = { Text("1") },
                                    singleLine = true,
                                    trailingIcon = if (rangeStartText.isNotEmpty()) {
                                        {
                                            IconButton(
                                                onClick = { rangeStartText = "" },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Clear From",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else null,
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            rangeStartText = safeRangeStart.toString()
                                            focusManager.moveFocus(FocusDirection.Right)
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "to",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )

                                OutlinedTextField(
                                    value = rangeEndText,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(3)
                                        rangeEndText = digits
                                        digits.toIntOrNull()?.let { parsed ->
                                            rangeEnd = parsed.coerceIn(1, maxId)
                                        }
                                        pendingGridAnchor = null
                                    },
                                    label = { Text("To #") },
                                    placeholder = { Text(maxId.toString()) },
                                    singleLine = true,
                                    trailingIcon = if (rangeEndText.isNotEmpty()) {
                                        {
                                            IconButton(
                                                onClick = { rangeEndText = "" },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Clear To",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else null,
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            rangeStartText = safeRangeStart.toString()
                                            rangeEndText = safeRangeEnd.toString()
                                            focusManager.clearFocus()
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            // Dual-thumb interactive RangeSlider for instant sweeping
                            if (maxId > 1) {
                                RangeSlider(
                                    value = rangeLo.toFloat()..rangeHi.toFloat(),
                                    onValueChange = { span ->
                                        val s = span.start.roundToInt().coerceIn(1, maxId)
                                        val e = span.endInclusive.roundToInt().coerceIn(1, maxId)
                                        applyRange(s, e)
                                        pendingGridAnchor = null
                                    },
                                    valueRange = 1f..maxId.toFloat(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "#1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Drag slider or type exact numbers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "#$maxId",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Quick presets in clean wrapping FlowRow
                            val presets = when (topicFilter) {
                                ExamTopicFilter.ARTICLE -> listOf(
                                    Triple(1, 10, "1–10"),
                                    Triple(11, 25, "11–25"),
                                    Triple(26, 50, "26–50"),
                                    Triple(1, 64, "Boards 1–64"),
                                    Triple(65, 112, "Practice 65–112"),
                                    Triple(1, 112, "All 1–112")
                                )
                                ExamTopicFilter.PREPOSITION -> listOf(
                                    Triple(1, 15, "1–15"),
                                    Triple(16, 35, "16–35"),
                                    Triple(36, 56, "36–56"),
                                    Triple(1, 56, "Boards 1–56"),
                                    Triple(57, 80, "Practice 57–80"),
                                    Triple(1, 80, "All 1–80")
                                )
                                ExamTopicFilter.BOTH -> listOf(
                                    Triple(1, 10, "1–10"),
                                    Triple(1, 25, "1–25"),
                                    Triple(1, 50, "1–50"),
                                    Triple(1, maxId, "All 1–$maxId")
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                presets.forEach { (a, b, label) ->
                                    val isSelected = rangeLo == a && rangeHi == b
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            applyRange(a, b)
                                            pendingGridAnchor = null
                                            focusManager.clearFocus()
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // 2-Tap Visual Range Picker toggle (Tap Start -> Tap End)
                            OutlinedButton(
                                onClick = {
                                    showVisualRangeGrid = !showVisualRangeGrid
                                    pendingGridAnchor = null
                                    focusManager.clearFocus()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (showVisualRangeGrid) "Hide 2-Tap Range Grid" else "2-Tap Visual Picker (Tap Start → Tap End)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            AnimatedVisibility(visible = showVisualRangeGrid) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        if (pendingGridAnchor == null) {
                                            "Step 1: Tap any number to set Start # (highlighted: #$rangeLo–#$rangeHi)"
                                        } else {
                                            "Start set to #${pendingGridAnchor}. Step 2: Now tap End #!"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    val allIds = (1..maxId).toList()
                                    allIds.chunked(8).forEach { rowIds ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            rowIds.forEach { num ->
                                                val anchor = pendingGridAnchor
                                                val isEndpoint = if (anchor != null) {
                                                    num == anchor
                                                } else {
                                                    num == rangeLo || num == rangeHi
                                                }
                                                val inRange = anchor == null && num in rangeLo..rangeHi
                                                val bgColor = when {
                                                    isEndpoint -> MaterialTheme.colorScheme.primary
                                                    inRange -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                                val fgColor = when {
                                                    isEndpoint -> MaterialTheme.colorScheme.onPrimary
                                                    inRange -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .clickable {
                                                            val cur = pendingGridAnchor
                                                            if (cur == null) {
                                                                pendingGridAnchor = num
                                                                applyRange(num, num)
                                                            } else {
                                                                applyRange(minOf(cur, num), maxOf(cur, num))
                                                                pendingGridAnchor = null
                                                            }
                                                        },
                                                    color = bgColor,
                                                    shape = RoundedCornerShape(7.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            num.toString(),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = if (isEndpoint) FontWeight.ExtraBold else FontWeight.SemiBold
                                                            ),
                                                            color = fgColor,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                            repeat(8 - rowIds.size) { Spacer(Modifier.weight(1f)) }
                                        }
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
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(3, 5, 10, 15, 25, 40).forEach { n ->
                                    AssistChip(
                                        onClick = { randomCount = n.coerceAtMost(pool.size) },
                                        label = { Text("$n") }
                                    )
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
                        Spacer(Modifier.height(6.dp))
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
