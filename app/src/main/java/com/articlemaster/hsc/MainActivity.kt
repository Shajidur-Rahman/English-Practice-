package com.articlemaster.hsc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.articlemaster.hsc.ui.AppRoute
import com.articlemaster.hsc.ui.MainViewModel
import com.articlemaster.hsc.ui.screens.ExamBuilderScreen
import com.articlemaster.hsc.ui.screens.FlashcardScreen
import com.articlemaster.hsc.ui.screens.HomeScreen
import com.articlemaster.hsc.ui.screens.MothersTestScreen
import com.articlemaster.hsc.ui.screens.PracticeBrowseScreen
import com.articlemaster.hsc.ui.screens.ResultScreen
import com.articlemaster.hsc.ui.screens.SessionScreen
import com.articlemaster.hsc.ui.screens.SolutionReferenceScreen
import com.articlemaster.hsc.ui.theme.ArticleMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishPracticeApp()
        }
    }
}

@Composable
fun EnglishPracticeApp(vm: MainViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusBanner) {
        val msg = state.statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.clearStatusBanner()
        }
    }

    BackHandler(enabled = state.route != AppRoute.Home && state.route != AppRoute.Session) {
        vm.navigateTo(AppRoute.Home)
    }

    val showBottomBar = !state.isLoading && state.route in setOf(
        AppRoute.Home,
        AppRoute.Browse,
        AppRoute.ExamBuilder,
        AppRoute.Flashcards,
        AppRoute.Reference
    )
    val dueFlashCount = state.flashcards.count { it.isDue() }

    ArticleMasterTheme(darkThemeOverride = state.progress.preferences.darkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = state.route == AppRoute.Home,
                                onClick = { vm.navigateTo(AppRoute.Home) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            NavigationBarItem(
                                selected = state.route == AppRoute.Browse,
                                onClick = { vm.navigateTo(AppRoute.Browse) },
                                icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Practice") },
                                label = { Text("Practice", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            NavigationBarItem(
                                selected = state.route == AppRoute.ExamBuilder,
                                onClick = { vm.navigateTo(AppRoute.ExamBuilder) },
                                icon = { Icon(Icons.Default.Tune, contentDescription = "Exam") },
                                label = { Text("Exam", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            NavigationBarItem(
                                selected = state.route == AppRoute.Flashcards,
                                onClick = { vm.navigateTo(AppRoute.Flashcards) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dueFlashCount > 0) {
                                                Badge { Text(if (dueFlashCount > 99) "99+" else dueFlashCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Style, contentDescription = "Flash")
                                    }
                                },
                                label = { Text("Flash", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            NavigationBarItem(
                                selected = state.route == AppRoute.Reference,
                                onClick = { vm.navigateTo(AppRoute.Reference) },
                                icon = { Icon(Icons.AutoMirrored.Filled.Rule, contentDescription = "Keys") },
                                label = { Text("Keys", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val systemDark = isSystemInDarkTheme()
                        when (state.route) {
                            AppRoute.Home -> HomeScreen(
                                state = state,
                                onSelectTopic = vm::selectTopic,
                                onOpenTopicBrowse = vm::openTopicBrowse,
                                onNavigate = vm::navigateTo,
                                onStartQuickSession = { vm.startSession(it) },
                                onContinueNext = vm::startSinglePractice,
                                onToggleTheme = {
                                    vm.updatePreferences { prefs ->
                                        val currentIsDark = prefs.darkMode ?: systemDark
                                        prefs.copy(darkMode = !currentIsDark)
                                    }
                                },
                                onDismissUpdate = vm::dismissUpdateBanner
                            )

                            AppRoute.Browse -> PracticeBrowseScreen(
                                state = state,
                                onSelectTopic = vm::selectTopic,
                                onBack = { vm.navigateTo(AppRoute.Home) },
                                onStartPassage = vm::startSinglePractice,
                                onToggleBookmark = vm::toggleBookmark,
                                onRunBatchOcr = { topic -> vm.runBatchOcrAllMissing(topic) }
                            )

                            AppRoute.ExamBuilder -> ExamBuilderScreen(
                                state = state,
                                previewCountForConfig = { vm.buildExerciseList(it).size },
                                onBack = { vm.navigateTo(AppRoute.Home) },
                                onLaunch = { vm.startSession(it) }
                            )

                            AppRoute.Flashcards -> FlashcardScreen(
                                state = state,
                                loadBitmap = vm::loadPassageBitmap,
                                onBack = { vm.navigateTo(AppRoute.Home) },
                                onReviewCard = vm::reviewFlashcard,
                                onAddCustomCard = vm::addCustomFlashcard,
                                onUpdateNote = vm::updateFlashcardNote,
                                onDeleteCard = vm::deleteFlashcard,
                                onOpenPassage = vm::startSinglePracticeByKey
                            )

                            AppRoute.MothersTest -> MothersTestScreen(
                                state = state,
                                loadBitmap = vm::loadPassageBitmap,
                                onSelectTopic = vm::selectTopic,
                                onUpdatePreferences = vm::updatePreferences,
                                onBack = { vm.navigateTo(AppRoute.Home) }
                            )

                            AppRoute.Session -> {
                                val session = state.activeSession
                                if (session != null) {
                                    SessionScreen(
                                        state = state,
                                        session = session,
                                        loadBitmap = vm::loadPassageBitmap,
                                        onSelectBlank = vm::selectBlank,
                                        onPickAnswer = vm::setAnswerForSelectedBlank,
                                        onAdvanceBlank = vm::advanceToNextBlank,
                                        onToggleWordBank = vm::toggleWordBankHint,
                                        onCheckPassage = vm::toggleCurrentPassageCheck,
                                        onHint = vm::revealSingleHint,
                                        onRevealAll = vm::revealAllSolutionsForCurrent,
                                        onClearPassage = vm::clearCurrentPassageAnswers,
                                        onToggleBookmark = vm::toggleBookmark,
                                        onToggleFlag = vm::toggleFlagCurrent,
                                        onTogglePause = vm::togglePauseTimer,
                                        onJumpTo = vm::jumpToPassage,
                                        onSubmit = { vm.submitSession(timedOut = false) },
                                        onAbandon = vm::abandonSession,
                                        onRunOcr = vm::runOcrForExercise,
                                        onSaveCustomText = vm::saveCustomPassageText,
                                        onPinBlankToFlashcards = { ex, bKey -> vm.pinBlankToFlashcards(ex, bKey) }
                                    )
                                } else {
                                    vm.navigateTo(AppRoute.Home)
                                }
                            }

                            AppRoute.Result -> {
                                val res = state.lastResult
                                if (res != null) {
                                    ResultScreen(
                                        state = state,
                                        result = res,
                                        loadBitmap = vm::loadPassageBitmap,
                                        onBackHome = { vm.navigateTo(AppRoute.Home) },
                                        onRetryMistakes = { vm.retryWrongPassages(res) },
                                        onOpenFlashcards = { vm.navigateTo(AppRoute.Flashcards) },
                                        onPinBlankToFlashcards = { ex, bKey -> vm.pinBlankToFlashcards(ex, bKey) }
                                    )
                                } else {
                                    vm.navigateTo(AppRoute.Home)
                                }
                            }

                            AppRoute.Reference -> SolutionReferenceScreen(
                                state = state,
                                onSelectTopic = vm::selectTopic,
                                onBack = { vm.navigateTo(AppRoute.Home) },
                                onPracticePassage = vm::startSinglePractice,
                                onPinBlankToFlashcards = { ex, bKey -> vm.pinBlankToFlashcards(ex, bKey) },
                                onUpdatePreferences = vm::updatePreferences,
                                onRunBatchOcr = { vm.runBatchOcrAllMissing(null) },
                                onExportJson = vm::exportProgressJson,
                                onImportJson = vm::importProgressJson,
                                onResetStats = vm::clearAllProgress,
                                onClearFlashcards = { vm.clearFlashcards(null) },
                                onCheckForUpdates = { vm.checkForUpdates(manual = true) }
                            )
                        }
                    }
                }
            }
        }
    }
}
