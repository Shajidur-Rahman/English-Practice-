package com.articlemaster.hsc.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.articlemaster.hsc.data.GitHubUpdateChecker
import com.articlemaster.hsc.model.AppPreferences
import com.articlemaster.hsc.model.BLANK_KEYS
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.ui.MainUiState
import com.articlemaster.hsc.ui.theme.EmeraldSuccess
import com.articlemaster.hsc.ui.theme.RoseError

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SolutionReferenceScreen(
    state: MainUiState,
    onSelectTopic: (GrammarTopic) -> Unit,
    onBack: () -> Unit,
    onPracticePassage: (ExerciseItem) -> Unit,
    onPinBlankToFlashcards: (ExerciseItem, String) -> Unit,
    onUpdatePreferences: ((AppPreferences) -> AppPreferences) -> Unit,
    onRunBatchOcr: () -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit,
    onResetStats: () -> Unit,
    onClearFlashcards: () -> Unit,
    onCheckForUpdates: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearFlashConfirm by remember { mutableStateOf(false) }
    var copiedNotice by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val topic = state.activeTopic

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Keys, Rules & Settings", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            PrimaryTabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Keys", maxLines = 1) })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Rules", maxLines = 1) })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Settings", maxLines = 1) })
            }

            when (tabIndex) {
                0 -> {
                    val topicExercises = state.exercisesFor(topic)
                    val filtered = remember(topicExercises, search) {
                        val q = search.trim().lowercase()
                        if (q.isEmpty()) topicExercises
                        else topicExercises.filter { ex ->
                            ex.id.toString() == q ||
                                "#${ex.id}".contains(q) ||
                                ex.boardInfo.lowercase().contains(q) ||
                                ex.answers.values.any { it.lowercase().contains(q) }
                        }
                    }

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        GrammarTopic.entries.forEachIndexed { idx, t ->
                            SegmentedButton(
                                selected = topic == t,
                                onClick = { onSelectTopic(t) },
                                shape = SegmentedButtonDefaults.itemShape(idx, GrammarTopic.entries.size)
                            ) {
                                Text(
                                    "${t.label} (${state.exercisesFor(t).size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Jump to #1..${topicExercises.size}, board, or key...", maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    Text(
                        "Tap any (a)–(j) answer pill to pin it directly to Flash Memory.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.key }) { ex ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${ex.id}. ${ex.boardInfo} (p.${ex.page})",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onPracticePassage(ex) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Practice",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        BLANK_KEYS.forEach { k ->
                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.clickable { onPinBlankToFlashcards(ex, k) }
                                            ) {
                                                Text(
                                                    "($k) ${ex.answers[k]}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        GrammarTopic.entries.forEachIndexed { idx, t ->
                            SegmentedButton(
                                selected = topic == t,
                                onClick = { onSelectTopic(t) },
                                shape = SegmentedButtonDefaults.itemShape(idx, GrammarTopic.entries.size)
                            ) {
                                Text(
                                    t.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    val rules = if (topic == GrammarTopic.ARTICLE) {
                        listOf(
                            "1. 'a' vs 'an' (Sound, Not Spelling)" to
                                "Use 'an' before vowel sounds: an apple, an umbrella, an honest man (silent h), an hour, an MBA / MBBS / MA / HSC / SOS / NGO. Use 'a' before consonant sounds: a European ('yu'), a university ('yu'), a unique ('yu'), a one-eyed man ('wa').",
                            "2. Definite Article 'the'" to
                                "Use 'the' before specific/previously mentioned nouns, superlatives (the largest, the best), double comparatives (the more..., the more...), unique things (the sun, the moon, the earth, the sky), rivers/seas/mountain ranges/newspapers, union country names (the USA, the UN), and adjectives representing a whole class (the rich, the poor).",
                            "3. Zero Article (×)" to
                                "Put '×' before: (a) possessive/demonstrative determiners already before a noun (his × work, this × novel); (b) uncountable/abstract nouns in general sense (× honesty, × greed, × time); (c) plural nouns in general sense; (d) proper names of people, towns, years.",
                            "4. Slash Variants in Answer Key" to
                                "When the book solution key lists alternatives (a/the, A/The, the/×, ×/the, a/×), English Practice accepts any listed choice as 100% correct!"
                        )
                    } else {
                        listOf(
                            "1. Time Prepositions (at / on / in / since / for)" to
                                "• AT: exact times, noon, night, age (at 10 a.m., at noon, at the age of fifteen).\n• ON: days and dates (on April 10, on a rainy day, on 27 September).\n• IN: months, seasons, years, long periods (in October, in 1913, in the autumn, in the long run).\n• SINCE / FOR: point of time (since childhood) vs duration (for several months).",
                            "2. Place & Movement (in / on / at / into / through / beyond)" to
                                "• IN (enclosed/large area: in Bangladesh, in a village), ON (surface: on earth, on the table), AT (specific point: at home, at the door).\n• INTO (motion inside: sink into oblivion), THROUGH (passage: blow through), BEYOND (past limits: beyond description).",
                            "3. High-Yield HSC Prepositions (TO)" to
                                "accustomed to, addicted to, adhere to, averse to, cling to, confine to, contribute to, devoted to, indifferent to, injurious to, key to, listen to, pay heed to, prejudicial to, refer to, submit to, succumb to, used to, vulnerable to.",
                            "4. High-Yield HSC Prepositions (OF / FROM / WITH)" to
                                "• OF: aware of, boastful of, conscious of, deprived of, devoid of, doubtful of, fond of, ignorant of, proud of, sanguine of, tired of, worthy of.\n• FROM: abstain from, aloof from, different from, escape from, free from, prevent from, relieved from, suffer from.\n• WITH: beset with, blessed with, covered with, endowed with, filled with, satisfied with, teeming with.",
                            "5. Phrasal Prepositions (BY / FOR / UP / DOWN)" to
                                "• abide by, learn by rote, by dint of\n• hanker after, look down upon, give up, put off, jot down, lay down, swoop upon/on, burst into tears, speak ill of, take pride in, absorb in, preside over, depend on/upon, rely on, in front of, in order to.",
                            "6. Multi-Option Slash Keys & Strict Case" to
                                "Many HSC Preposition blanks accept multiple prepositions (e.g. of/for/about, before/in/in front of). Typing or tapping ANY listed option is graded 100% correct!"
                        )
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rules) { (title, body) ->
                            Card(
                                shape = RoundedCornerShape(13.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Text(body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val prefs = state.progress.preferences
                    val themeIdx = when (prefs.darkMode) {
                        null -> 0
                        false -> 1
                        true -> 2
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // GitHub Release Auto-Updater Card
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "App Updates (GitHub Releases)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                "Installed: v${state.appVersionName} • ${GitHubUpdateChecker.REPO_OWNER}/${GitHubUpdateChecker.REPO_NAME}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    state.availableUpdate?.let { upd ->
                                        Button(
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(upd.downloadUrl)))
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Download New Update (${upd.latestTag})", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = onCheckForUpdates,
                                            enabled = !state.isCheckingUpdate,
                                            modifier = Modifier.weight(1.2f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            if (state.isCheckingUpdate) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Checking...", maxLines = 1)
                                            } else {
                                                Text("Check Updates", maxLines = 1)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(GitHubUpdateChecker.RELEASES_PAGE_URL))
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(0.9f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Releases", maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Appearance & Defaults", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Spacer(Modifier.height(8.dp))
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        listOf("System", "Light", "Dark").forEachIndexed { i, lbl ->
                                            SegmentedButton(
                                                selected = themeIdx == i,
                                                onClick = {
                                                    val mode = when (i) {
                                                        1 -> false
                                                        2 -> true
                                                        else -> null
                                                    }
                                                    onUpdatePreferences { it.copy(darkMode = mode) }
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(i, 3)
                                            ) {
                                                Text(lbl, maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    PrefToggle(
                                        "Auto-save mistakes to Flash Memory DB",
                                        prefs.autoSaveMistakesToFlashcards
                                    ) { v -> onUpdatePreferences { it.copy(autoSaveMistakesToFlashcards = v) } }
                                    PrefToggle(
                                        "Invert passage colors in Dark Mode",
                                        prefs.invertPassageInDark
                                    ) { v -> onUpdatePreferences { it.copy(invertPassageInDark = v) } }
                                    PrefToggle(
                                        "Auto-advance to next blank",
                                        prefs.autoAdvanceBlank
                                    ) { v -> onUpdatePreferences { it.copy(autoAdvanceBlank = v) } }
                                    PrefToggle(
                                        "Default Strict Capitalization",
                                        prefs.defaultStrictCase
                                    ) { v -> onUpdatePreferences { it.copy(defaultStrictCase = v) } }
                                    PrefToggle(
                                        "Default HSC 0.5 mark/blank scale",
                                        prefs.defaultHscHalfMark
                                    ) { v -> onUpdatePreferences { it.copy(defaultHscHalfMark = v) } }
                                    PrefToggle(
                                        "Prefer Interactive Text when scanned",
                                        prefs.preferTextViewWhenAvailable
                                    ) { v -> onUpdatePreferences { it.copy(preferTextViewWhenAvailable = v) } }
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "OCR & Backup (${state.progress.customTexts.size}/${state.exercises.size} texts • ${state.flashcards.size} cards)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    state.batchOcrProgress?.let { (d, t) ->
                                        LinearProgressIndicator(
                                            progress = { d.toFloat() / t.coerceAtLeast(1) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text("Scanning $d of $t...", style = MaterialTheme.typography.labelSmall)
                                    }
                                    FilledTonalButton(
                                        onClick = onRunBatchOcr,
                                        enabled = state.batchOcrProgress == null,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Scan Missing Passages (ML Kit)", maxLines = 1)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("EnglishPracticeBackup", onExportJson()))
                                                copiedNotice = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (copiedNotice) "Copied!" else "Export", maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { showImportDialog = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Import", maxLines = 1)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showResetConfirm = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Reset Study Stats & History", color = RoseError, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { showClearFlashConfirm = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Clear Flash Memory DB (${state.flashcards.size})", color = RoseError, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Full Backup JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    placeholder = { Text("Paste exported JSON here...") },
                    minLines = 5,
                    maxLines = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onImportJson(jsonInput)
                    showImportDialog = false
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Study Stats?") },
            text = { Text("This clears attempt history, mastery status, and bookmarks. Extracted texts and Flash Memory cards are kept.") },
            confirmButton = {
                Button(onClick = {
                    onResetStats()
                    showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearFlashConfirm) {
        AlertDialog(
            onDismissRequest = { showClearFlashConfirm = false },
            title = { Text("Clear All Flashcards?") },
            text = { Text("This deletes all ${state.flashcards.size} cards from your SQLite Flash Memory database.") },
            confirmButton = {
                Button(onClick = {
                    onClearFlashcards()
                    showClearFlashConfirm = false
                }) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearFlashConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PrefToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
