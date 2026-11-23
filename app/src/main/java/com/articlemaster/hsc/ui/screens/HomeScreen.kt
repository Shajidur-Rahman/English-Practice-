package com.articlemaster.hsc.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.ExamConfig
import com.articlemaster.hsc.model.ExamHistoryEntry
import com.articlemaster.hsc.model.ExamTopicFilter
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.SelectionMode
import com.articlemaster.hsc.model.SessionType
import com.articlemaster.hsc.model.TimerMode
import com.articlemaster.hsc.ui.AppRoute
import com.articlemaster.hsc.ui.MainUiState
import com.articlemaster.hsc.ui.theme.AmberAccent
import com.articlemaster.hsc.ui.theme.EmeraldContainer
import com.articlemaster.hsc.ui.theme.EmeraldSuccess
import com.articlemaster.hsc.ui.theme.IndigoPrimary
import com.articlemaster.hsc.ui.theme.RoseError
import com.articlemaster.hsc.ui.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MainUiState,
    onSelectTopic: (GrammarTopic) -> Unit,
    onOpenTopicBrowse: (GrammarTopic) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onStartQuickSession: (ExamConfig) -> Unit,
    onContinueNext: (ExerciseItem) -> Unit,
    onToggleTheme: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismissUpdate: () -> Unit
) {
    val context = LocalContext.current
    val activeTopic = state.activeTopic
    val topicFilter = if (activeTopic == GrammarTopic.ARTICLE) ExamTopicFilter.ARTICLE else ExamTopicFilter.PREPOSITION
    val activeList = state.exercisesFor(activeTopic)
    val statsMap = state.progress.statsByKey

    val artTotal = state.articleExercises.size.coerceAtLeast(1)
    val artAttempted = state.articleExercises.count { (statsMap[it.key]?.attempts ?: 0) > 0 }
    val artMastered = state.articleExercises.count { statsMap[it.key]?.mastered == true }

    val prepTotal = state.prepositionExercises.size.coerceAtLeast(1)
    val prepAttempted = state.prepositionExercises.count { (statsMap[it.key]?.attempts ?: 0) > 0 }
    val prepMastered = state.prepositionExercises.count { statsMap[it.key]?.mastered == true }

    val activeTotal = activeList.size.coerceAtLeast(1)
    val activeAttempted = activeList.count { (statsMap[it.key]?.attempts ?: 0) > 0 }
    val activeMastered = activeList.count { statsMap[it.key]?.mastered == true }
    val activeMistakes = activeList.count { statsMap[it.key]?.wrongBlankKeys?.isNotEmpty() == true }
    val activeBookmarked = activeList.count { statsMap[it.key]?.bookmarked == true }
    val nextUnattempted = activeList.firstOrNull { (statsMap[it.key]?.attempts ?: 0) == 0 } ?: activeList.firstOrNull()

    val now = System.currentTimeMillis()
    val totalFlashcards = state.flashcards.size
    val dueFlashcards = state.flashcards.count { it.isDue(now) }
    val artFlashCount = state.flashcards.count { it.topic == GrammarTopic.ARTICLE }
    val prepFlashCount = state.flashcards.count { it.topic == GrammarTopic.PREPOSITION }

    val isDark = state.progress.preferences.darkMode ?: isSystemInDarkTheme()

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "English Practice",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            "Articles ${state.articleExercises.size} • Prepositions ${state.prepositionExercises.size} • v${state.appVersionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GitHub Release Auto-Update Banner (only visible when a newer release exists!)
            state.availableUpdate?.let { update ->
                item(key = "update_banner") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                        border = BorderStroke(1.5.dp, EmeraldSuccess)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = Color(0xFF065F46),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "New Update Available (${update.latestTag})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF065F46)
                                        )
                                        Text(
                                            update.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF065F46).copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onDismissUpdate,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF065F46), modifier = Modifier.size(16.dp))
                                }
                            }
                            if (update.releaseNotes.isNotBlank()) {
                                Text(
                                    update.releaseNotes.take(140),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF064E3B),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val dlProg = state.updateDownloadProgress
                            if (dlProg != null) {
                                LinearProgressIndicator(
                                    progress = { dlProg },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Text(
                                    "Downloading update (${(dlProg * 100).toInt()}%)...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF065F46)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onInstallUpdate,
                                        modifier = Modifier.weight(1.3f),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text("Install Update (${update.latestTag})", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)))
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text("Browser", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Practice Menu: Articles vs Prepositions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopicMenuCard(
                        title = "Articles",
                        subtitle = "${state.articleExercises.size} Qs • ${state.articleExercises.size * 10} Blanks",
                        progressText = "$artMastered mastered • $artAttempted done",
                        progress = artAttempted.toFloat() / artTotal.toFloat(),
                        accent = IndigoPrimary,
                        selected = activeTopic == GrammarTopic.ARTICLE,
                        modifier = Modifier.weight(1f),
                        onSelect = { onSelectTopic(GrammarTopic.ARTICLE) },
                        onBrowse = { onOpenTopicBrowse(GrammarTopic.ARTICLE) }
                    )
                    TopicMenuCard(
                        title = "Prepositions",
                        subtitle = "${state.prepositionExercises.size} Qs • 800 Blanks",
                        progressText = "$prepMastered mastered • $prepAttempted done",
                        progress = prepAttempted.toFloat() / prepTotal.toFloat(),
                        accent = TealSecondary,
                        selected = activeTopic == GrammarTopic.PREPOSITION,
                        modifier = Modifier.weight(1f),
                        onSelect = { onSelectTopic(GrammarTopic.PREPOSITION) },
                        onBrowse = { onOpenTopicBrowse(GrammarTopic.PREPOSITION) }
                    )
                }
            }

            // Featured Quick Tools: Mother's Test + Flash Memory
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppRoute.MothersTest) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7C3AED).copy(alpha = 0.12f)),
                    border = BorderStroke(1.2.dp, Color(0xFF7C3AED).copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF7C3AED).copy(alpha = 0.18f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Mother's Test",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    "Clean 5+5 oral answers & ✨ real book check",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = Color(0xFF7C3AED)
                        ) {
                            Text(
                                "Open",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppRoute.Flashcards) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    border = BorderStroke(1.dp, TealSecondary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TealSecondary.copy(alpha = 0.18f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Style,
                                        contentDescription = null,
                                        tint = TealSecondary,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Flash Memory DB",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                                Text(
                                    if (totalFlashcards == 0) {
                                        "Auto-saves mistakes for both topics"
                                    } else {
                                        "$dueFlashcards due • $totalFlashcards cards ($artFlashCount Art, $prepFlashCount Prep)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (dueFlashcards > 0) RoseError else TealSecondary
                        ) {
                            Text(
                                text = if (dueFlashcards > 0) "Flash $dueFlashcards" else "Open",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Active Topic Progress Banner
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${activeTopic.label} Progress",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "$activeAttempted/$activeTotal done • $activeMastered mastered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${activeMastered * 100 / activeTotal}%",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { activeAttempted.toFloat() / activeTotal.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatPill("Mastered", activeMastered.toString(), EmeraldSuccess, Modifier.weight(1f))
                            StatPill("Mistakes", activeMistakes.toString(), RoseError, Modifier.weight(1f))
                            StatPill("Saved", activeBookmarked.toString(), AmberAccent, Modifier.weight(1f))
                        }

                        if (nextUnattempted != null) {
                            FilledTonalButton(
                                onClick = { onContinueNext(nextUnattempted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(11.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (activeAttempted == 0) {
                                        "Start ${activeTopic.label} #1"
                                    } else {
                                        "Continue ${activeTopic.label} #${nextUnattempted.id}"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Primary Modes for Selected Topic
            item {
                Text(
                    "${activeTopic.label} • Quick Modes",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeCard(
                        title = "Custom Exam",
                        subtitle = "Art, Prep or Mixed + timer",
                        icon = Icons.Default.Tune,
                        tint = IndigoPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppRoute.ExamBuilder) }
                    )
                    ModeCard(
                        title = "Browse All",
                        subtitle = "${activeList.size} passages & OCR",
                        icon = Icons.AutoMirrored.Filled.LibraryBooks,
                        tint = TealSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppRoute.Browse) }
                    )
                }
            }

            item {
                val boardRangeText = if (activeTopic == GrammarTopic.ARTICLE) "1–64" else "1–56"
                val boardYearsText = if (activeTopic == GrammarTopic.ARTICLE) "2009–2026 Boards" else "2016–2025 Boards"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeCard(
                        title = "Board ($boardRangeText)",
                        subtitle = boardYearsText,
                        icon = Icons.Default.School,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onStartQuickSession(
                                ExamConfig(
                                    title = "${activeTopic.label} Board Qs ($boardRangeText)",
                                    topicFilter = topicFilter,
                                    sessionType = SessionType.PRACTICE,
                                    selectionMode = SelectionMode.BOARD_ONLY,
                                    instantFeedback = true,
                                    strictCapitalization = state.progress.preferences.defaultStrictCase,
                                    hscHalfMarkScale = state.progress.preferences.defaultHscHalfMark
                                )
                            )
                        }
                    )
                    ModeCard(
                        title = "Quick 5 Mock",
                        subtitle = "5 random • 8 min timer",
                        icon = Icons.Default.Shuffle,
                        tint = AmberAccent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onStartQuickSession(
                                ExamConfig(
                                    title = "Quick 5 ${activeTopic.label} Mock",
                                    topicFilter = topicFilter,
                                    sessionType = SessionType.EXAM,
                                    selectionMode = SelectionMode.RANDOM,
                                    randomCount = 5,
                                    timerMode = TimerMode.TOTAL_MINUTES,
                                    totalMinutes = 8,
                                    instantFeedback = false,
                                    strictCapitalization = state.progress.preferences.defaultStrictCase,
                                    hscHalfMarkScale = state.progress.preferences.defaultHscHalfMark
                                )
                            )
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeCard(
                        title = "Mistakes ($activeMistakes)",
                        subtitle = if (activeMistakes > 0) "Retry past errors" else "No errors yet",
                        icon = Icons.Default.ErrorOutline,
                        tint = RoseError,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onStartQuickSession(
                                ExamConfig(
                                    title = "${activeTopic.label} Mistake Drill",
                                    topicFilter = topicFilter,
                                    sessionType = SessionType.PRACTICE,
                                    selectionMode = SelectionMode.MISTAKES,
                                    instantFeedback = true,
                                    strictCapitalization = state.progress.preferences.defaultStrictCase,
                                    hscHalfMarkScale = state.progress.preferences.defaultHscHalfMark
                                )
                            )
                        }
                    )
                    ModeCard(
                        title = "Keys & Rules",
                        subtitle = "All 156 keys & settings",
                        icon = Icons.AutoMirrored.Filled.Rule,
                        tint = EmeraldSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppRoute.Reference) }
                    )
                }
            }

            if (activeBookmarked > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onStartQuickSession(
                                    ExamConfig(
                                        title = "Bookmarked ${activeTopic.label}",
                                        topicFilter = topicFilter,
                                        sessionType = SessionType.PRACTICE,
                                        selectionMode = SelectionMode.BOOKMARKED,
                                        instantFeedback = true,
                                        strictCapitalization = state.progress.preferences.defaultStrictCase,
                                        hscHalfMarkScale = state.progress.preferences.defaultHscHalfMark
                                    )
                                )
                            },
                        shape = RoundedCornerShape(13.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Practice Bookmarked ($activeBookmarked)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                                Text(
                                    "Review your starred ${activeTopic.label.lowercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                    }
                }
            }

            if (state.progress.history.isNotEmpty()) {
                item {
                    Text(
                        "Recent Attempts",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                items(state.progress.history.take(5), key = { it.id }) { entry ->
                    HistoryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun TopicMenuCard(
    title: String,
    subtitle: String,
    progressText: String,
    progress: Float,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onBrowse: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (selected) 1.8.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(onClick = onBrowse)
                ) {
                    Text(
                        "List",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Text(
                progressText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .heightIn(min = 102.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: ExamHistoryEntry) {
    val dateFmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.Quiz,
                    contentDescription = null,
                    tint = if (entry.sessionType == SessionType.EXAM) IndigoPrimary else TealSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${dateFmt.format(Date(entry.timestamp))} • ${AnswerEvaluator.formatDuration(entry.durationSeconds)} • ${entry.passageKeys.size} Qs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${AnswerEvaluator.formatScore(entry.rawScore)}/${AnswerEvaluator.formatScore(entry.maxRawScore)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${entry.correctCount}/${entry.totalBlanks}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
