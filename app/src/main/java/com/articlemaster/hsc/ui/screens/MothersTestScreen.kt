package com.articlemaster.hsc.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.articlemaster.hsc.model.AppPreferences
import com.articlemaster.hsc.model.BLANK_KEYS
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.MothersLayoutMode
import com.articlemaster.hsc.ui.MainUiState
import com.articlemaster.hsc.ui.components.ZoomablePassageImage
import com.articlemaster.hsc.ui.theme.IndigoPrimary
import com.articlemaster.hsc.ui.theme.TealSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MothersTestScreen(
    state: MainUiState,
    loadBitmap: (String) -> Bitmap?,
    onSelectTopic: (GrammarTopic) -> Unit,
    onUpdatePreferences: ((AppPreferences) -> AppPreferences) -> Unit,
    onBack: () -> Unit
) {
    val topic = state.activeTopic
    val topicList = state.exercisesFor(topic)
    val prefs = state.progress.preferences
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember(topic) { mutableStateOf<Int?>(null) }
    var showCustomizeSheet by remember { mutableStateOf(false) }

    val currentIndex = selectedIndex
    if (currentIndex != null && currentIndex in topicList.indices && prefs.mothersLayout != MothersLayoutMode.FULL_PAGE) {
        BackHandler { selectedIndex = null }
        MothersAnswerDetailView(
            exercise = topicList[currentIndex],
            index = currentIndex,
            total = topicList.size,
            prefs = prefs,
            loadBitmap = loadBitmap,
            onBackToList = { selectedIndex = null },
            onPrev = { if (currentIndex > 0) selectedIndex = currentIndex - 1 },
            onNext = { if (currentIndex < topicList.lastIndex) selectedIndex = currentIndex + 1 },
            onOpenCustomize = { showCustomizeSheet = true }
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
        val isFullPage = prefs.mothersLayout == MothersLayoutMode.FULL_PAGE

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
                                "${prefs.mothersLayout.label} • ${(prefs.mothersTextScale * 100).roundToInt()}% size",
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
                    },
                    actions = {
                        OutlinedButton(
                            onClick = { showCustomizeSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(34.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Customize", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
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
                    verticalArrangement = Arrangement.spacedBy(if (isFullPage) 12.dp else 8.dp)
                ) {
                    if (isFullPage) {
                        // Continuous Full-Page Sheet: all passages rendered read-only with inline Magic check
                        items(filtered, key = { it.key }) { ex ->
                            val realIdx = topicList.indexOfFirst { it.key == ex.key }
                            FullPagePassageAnswerCard(
                                exercise = ex,
                                prefs = prefs,
                                accent = accent,
                                loadBitmap = loadBitmap,
                                onOpenSingle = {
                                    if (realIdx >= 0) {
                                        onUpdatePreferences { it.copy(mothersLayout = MothersLayoutMode.SPLIT_5_5) }
                                        selectedIndex = realIdx
                                    }
                                }
                            )
                        }
                    } else {
                        // Clean List View of all questions
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

    if (showCustomizeSheet) {
        MothersCustomizeSheet(
            prefs = prefs,
            onUpdatePreferences = onUpdatePreferences,
            onDismiss = { showCustomizeSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MothersAnswerDetailView(
    exercise: ExerciseItem,
    index: Int,
    total: Int,
    prefs: AppPreferences,
    loadBitmap: (String) -> Bitmap?,
    onBackToList: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpenCustomize: () -> Unit
) {
    var showMagicSolutionPic by remember(exercise.key) { mutableStateOf(false) }
    var showPassagePic by remember(exercise.key) { mutableStateOf(false) }

    val accent = if (exercise.topic == GrammarTopic.ARTICLE) IndigoPrimary else TealSecondary
    val leftKeys = BLANK_KEYS.take(5)
    val rightKeys = BLANK_KEYS.drop(5)
    val scale = prefs.mothersTextScale

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "${exercise.topic.label} #${exercise.id}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1
                        )
                        if (!prefs.mothersMinimalHeader) {
                            Text(
                                "${exercise.boardInfo} • p.${exercise.page}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToList) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All List")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onOpenCustomize,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(34.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Customize", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Read-Only Answer View according to chosen customization
            when (prefs.mothersLayout) {
                MothersLayoutMode.SPLIT_5_5, MothersLayoutMode.FULL_PAGE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (rowIdx in 0 until 5) {
                            val leftKey = leftKeys[rowIdx]
                            val rightKey = rightKeys[rowIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReadOnlyAnswerTile(
                                    blankKey = leftKey,
                                    answer = exercise.answers[leftKey] ?: "",
                                    textScale = scale,
                                    accent = accent,
                                    isSplitColumn = true,
                                    modifier = Modifier.weight(1f)
                                )
                                ReadOnlyAnswerTile(
                                    blankKey = rightKey,
                                    answer = exercise.answers[rightKey] ?: "",
                                    textScale = scale,
                                    accent = accent,
                                    isSplitColumn = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                MothersLayoutMode.ONE_PER_LINE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        BLANK_KEYS.forEach { k ->
                            ReadOnlyAnswerTile(
                                blankKey = k,
                                answer = exercise.answers[k] ?: "",
                                textScale = scale,
                                accent = accent,
                                isSplitColumn = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                MothersLayoutMode.TWO_LINE_SHEET -> {
                    TwoLineSheetAnswerBlock(
                        exercise = exercise,
                        textScale = scale,
                        accent = accent
                    )
                }
            }

            // ✨ MAGIC BUTTON (Real Printed Book Solution Pic) + Passage Toggle
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
private fun ReadOnlyAnswerTile(
    blankKey: String,
    answer: String,
    textScale: Float,
    accent: Color,
    isSplitColumn: Boolean,
    modifier: Modifier = Modifier
) {
    val prettyAnswer = answer.replace("/", " / ")
    val baseSp = if (isSplitColumn) {
        when {
            prettyAnswer.length > 22 -> 12f
            prettyAnswer.length > 13 -> 13.5f
            else -> 16f
        }
    } else {
        17.5f
    }
    val scaledSp = (baseSp * textScale).coerceIn(12f, 28f).sp
    val lineH = (scaledSp.value + 4.5f).sp
    val badgeSize = (26f * textScale.coerceIn(0.9f, 1.35f)).dp

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.3.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSplitColumn) 8.dp else 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(badgeSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        blankKey,
                        fontSize = (13f * textScale.coerceIn(0.9f, 1.3f)).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }
            }

            Text(
                text = prettyAnswer,
                fontSize = scaledSp,
                lineHeight = lineH,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isSplitColumn) 3 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TwoLineSheetAnswerBlock(
    exercise: ExerciseItem,
    textScale: Float,
    accent: Color
) {
    val fontSize = (15f * textScale).coerceIn(13f, 26f).sp
    val lineH = (fontSize.value + 8f).sp

    fun buildHalfLine(keys: List<String>) = buildAnnotatedString {
        keys.forEachIndexed { idx, k ->
            val ans = (exercise.answers[k] ?: "").replace("/", "/")
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) {
                append("($k) ")
            }
            withStyle(SpanStyle(color = Color.Unspecified, fontWeight = FontWeight.Bold)) {
                append(ans)
            }
            if (idx < keys.lastIndex) {
                withStyle(SpanStyle(color = accent.copy(alpha = 0.55f), fontWeight = FontWeight.Normal)) {
                    append(";  ")
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.4.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buildHalfLine(BLANK_KEYS.take(5)),
                    fontSize = fontSize,
                    lineHeight = lineH,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buildHalfLine(BLANK_KEYS.drop(5)),
                    fontSize = fontSize,
                    lineHeight = lineH,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun FullPagePassageAnswerCard(
    exercise: ExerciseItem,
    prefs: AppPreferences,
    accent: Color,
    loadBitmap: (String) -> Bitmap?,
    onOpenSingle: () -> Unit
) {
    var showMagic by remember(exercise.key) { mutableStateOf(false) }
    val scale = prefs.mothersTextScale
    val fontSize = (14.5f * scale).coerceIn(13f, 24f).sp
    val lineH = (fontSize.value + 7f).sp

    val line1 = buildAnnotatedString {
        BLANK_KEYS.take(5).forEachIndexed { idx, k ->
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) { append("($k) ") }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(exercise.answers[k] ?: "") }
            if (idx < 4) append(";  ")
        }
    }
    val line2 = buildAnnotatedString {
        BLANK_KEYS.drop(5).forEachIndexed { idx, k ->
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) { append("($k) ") }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(exercise.answers[k] ?: "") }
            if (idx < 4) append(";  ")
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (prefs.mothersMinimalHeader) {
                            "#${exercise.id}"
                        } else {
                            "#${exercise.id} • ${exercise.boardInfo} (p.${exercise.page})"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C3AED).copy(alpha = if (showMagic) 1f else 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMagic = !showMagic }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoFixHigh,
                                contentDescription = "Real Solution",
                                tint = if (showMagic) Color.White else Color(0xFF7C3AED),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                if (showMagic) "Hide" else "Magic",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (showMagic) Color.White else Color(0xFF7C3AED)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenSingle)
                    ) {
                        Text(
                            "Focus",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text(
                text = line1,
                fontSize = fontSize,
                lineHeight = lineH,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = line2,
                fontSize = fontSize,
                lineHeight = lineH,
                color = MaterialTheme.colorScheme.onSurface
            )

            AnimatedVisibility(visible = showMagic) {
                val solBmp = remember(exercise.solutionImageAsset) { loadBitmap(exercise.solutionImageAsset) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ZoomablePassageImage(
                        bitmap = solBmp,
                        invertInDarkMode = prefs.invertPassageInDark,
                        darkModeOverride = prefs.darkMode,
                        minHeight = 75.dp,
                        maxHeight = 175.dp,
                        fullscreenTitle = "Real Solution #${exercise.id}"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MothersCustomizeSheet(
    prefs: AppPreferences,
    onUpdatePreferences: ((AppPreferences) -> AppPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Customize Mother's Test",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // 1. Answer View Layout
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Answer Layout",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                MothersLayoutMode.entries.forEach { mode ->
                    val selected = prefs.mothersLayout == mode
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdatePreferences { it.copy(mothersLayout = mode) } },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                mode.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                mode.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Make Text Big
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Answer Text Size",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${(prefs.mothersTextScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.95f to "Normal", 1.15f to "Large", 1.40f to "Big", 1.65f to "Huge").forEach { (sc, lbl) ->
                        FilterChip(
                            selected = kotlin.math.abs(prefs.mothersTextScale - sc) < 0.08f,
                            onClick = { onUpdatePreferences { it.copy(mothersTextScale = sc) } },
                            label = { Text(lbl) }
                        )
                    }
                }

                Slider(
                    value = prefs.mothersTextScale,
                    onValueChange = { v -> onUpdatePreferences { it.copy(mothersTextScale = v) } },
                    valueRange = 0.85f..1.75f
                )
            }

            // 3. Minimal Header Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdatePreferences { it.copy(mothersMinimalHeader = !it.mothersMinimalHeader) } }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Minimal Header", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Hide board year & page info for distraction-free viewing",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = prefs.mothersMinimalHeader,
                    onCheckedChange = { checked -> onUpdatePreferences { it.copy(mothersMinimalHeader = checked) } }
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
