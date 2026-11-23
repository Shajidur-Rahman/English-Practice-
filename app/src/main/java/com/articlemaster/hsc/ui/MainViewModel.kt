package com.articlemaster.hsc.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.articlemaster.hsc.data.AppUpdateInstaller
import com.articlemaster.hsc.data.ExerciseRepository
import com.articlemaster.hsc.data.GitHubUpdateChecker
import com.articlemaster.hsc.data.ReleaseUpdateInfo
import com.articlemaster.hsc.data.UpdateCheckResult
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.AppPreferences
import com.articlemaster.hsc.model.BLANK_KEYS
import com.articlemaster.hsc.model.ExamConfig
import com.articlemaster.hsc.model.ExamHistoryEntry
import com.articlemaster.hsc.model.ExamResult
import com.articlemaster.hsc.model.ExamTopicFilter
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.ExerciseStats
import com.articlemaster.hsc.model.FlashcardEntity
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.ReviewRating
import com.articlemaster.hsc.model.SelectionMode
import com.articlemaster.hsc.model.SessionType
import com.articlemaster.hsc.model.SpacedRepetitionEngine
import com.articlemaster.hsc.model.TimerMode
import com.articlemaster.hsc.model.UserProgressData
import com.articlemaster.hsc.ocr.OnDeviceOcrHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface AppRoute {
    data object Home : AppRoute
    data object Browse : AppRoute
    data object ExamBuilder : AppRoute
    data object Flashcards : AppRoute
    data object MothersTest : AppRoute
    data object Session : AppRoute
    data object Result : AppRoute
    data object Reference : AppRoute
}

data class ActiveSessionState(
    val config: ExamConfig,
    val exercises: List<ExerciseItem>,
    val currentIndex: Int = 0,
    val selectedBlankKey: String = "a",
    val answersByKey: Map<String, Map<String, String>> = emptyMap(),
    val checkedExerciseKeys: Set<String> = emptySet(),
    val revealedBlanksByKey: Map<String, Set<String>> = emptyMap(),
    val flaggedExerciseKeys: Set<String> = emptySet(),
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int? = null,
    val perPassageRemainingSeconds: Int? = null,
    val isPaused: Boolean = false,
    val timeExpiredNotice: Boolean = false,
    val showWordBankHint: Boolean = config.showWordBankHint
) {
    val currentExercise: ExerciseItem? get() = exercises.getOrNull(currentIndex)
}

data class MainUiState(
    val isLoading: Boolean = true,
    val route: AppRoute = AppRoute.Home,
    val activeTopic: GrammarTopic = GrammarTopic.ARTICLE,
    val exercises: List<ExerciseItem> = emptyList(),
    val progress: UserProgressData = UserProgressData(),
    val flashcards: List<FlashcardEntity> = emptyList(),
    val activeSession: ActiveSessionState? = null,
    val lastResult: ExamResult? = null,
    val ocrBusyForKey: String? = null,
    val batchOcrProgress: Pair<Int, Int>? = null,
    val statusBanner: String? = null,
    val appVersionName: String = "2.4.1",
    val availableUpdate: ReleaseUpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val updateDownloadProgress: Float? = null
) {
    val articleExercises: List<ExerciseItem> get() = exercises.filter { it.topic == GrammarTopic.ARTICLE }
    val prepositionExercises: List<ExerciseItem> get() = exercises.filter { it.topic == GrammarTopic.PREPOSITION }
    fun exercisesFor(topic: GrammarTopic): List<ExerciseItem> =
        if (topic == GrammarTopic.ARTICLE) articleExercises else prepositionExercises
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExerciseRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        val currentVer = runCatching {
            application.packageManager.getPackageInfo(application.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "2.4.1" }

        viewModelScope.launch {
            val loadedExercises = withContext(Dispatchers.IO) { repo.loadAllExercises() }
            val loadedProgress = withContext(Dispatchers.IO) { repo.loadProgress() }
            val loadedCards = withContext(Dispatchers.IO) { repo.flashcardDb.getAllCards() }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    exercises = loadedExercises,
                    progress = loadedProgress,
                    flashcards = loadedCards,
                    appVersionName = currentVer
                )
            }
            // Silently check GitHub Releases for newer app update
            checkForUpdates(manual = false)
        }
    }

    fun checkForUpdates(manual: Boolean = true) {
        if (_uiState.value.isCheckingUpdate) return
        val currentVer = _uiState.value.appVersionName
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            val result = withContext(Dispatchers.IO) {
                GitHubUpdateChecker.checkLatestRelease(currentVer)
            }
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = result.info,
                            showUpdateDialog = true,
                            statusBanner = if (manual) "New update ${result.info.latestTag} is available!" else it.statusBanner
                        )
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = null,
                            statusBanner = if (manual) "You are up to date (v$currentVer)." else it.statusBanner
                        )
                    }
                }
                UpdateCheckResult.NoReleasePublished -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            statusBanner = if (manual) "No GitHub release published yet (current v$currentVer)." else it.statusBanner
                        )
                    }
                }
                is UpdateCheckResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            statusBanner = if (manual) "Could not check updates: ${result.message}" else it.statusBanner
                        )
                    }
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun dismissUpdateBanner() {
        _uiState.update { it.copy(availableUpdate = null, showUpdateDialog = false) }
    }

    fun downloadAndInstallUpdate() {
        val update = _uiState.value.availableUpdate ?: return
        if (_uiState.value.updateDownloadProgress != null) return
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch {
            _uiState.update { it.copy(updateDownloadProgress = 0.01f) }
            val result = withContext(Dispatchers.IO) {
                AppUpdateInstaller.downloadApk(appContext, update.downloadUrl) { frac ->
                    _uiState.update { it.copy(updateDownloadProgress = frac) }
                }
            }
            _uiState.update { it.copy(updateDownloadProgress = null) }
            result.onSuccess { file ->
                val started = runCatching {
                    AppUpdateInstaller.launchApkInstaller(appContext, file)
                }.getOrDefault(false)
                if (!started) {
                    _uiState.update {
                        it.copy(statusBanner = "Allow 'Install unknown apps' for English Practice, then tap Update again.")
                    }
                } else {
                    _uiState.update { it.copy(showUpdateDialog = false) }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(statusBanner = "Download failed (${err.message}). Try Browser download.")
                }
            }
        }
    }

    fun recordMothersTestMistakes(exercise: ExerciseItem, wrongBlankKeys: List<String>) {
        if (wrongBlankKeys.isEmpty()) return
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            wrongBlankKeys.forEach { bKey ->
                val expected = exercise.answers[bKey] ?: return@forEach
                repo.flashcardDb.recordMistake(
                    exercise = exercise,
                    blankKey = bKey,
                    wrongAnswer = "Mother's Test",
                    expectedAnswer = expected,
                    nowMs = now
                )
            }
            val updated = repo.flashcardDb.getAllCards()
            _uiState.update {
                it.copy(
                    flashcards = updated,
                    statusBanner = "Saved ${wrongBlankKeys.size} Mother's Test mistake${if (wrongBlankKeys.size == 1) "" else "s"} to Flash Memory."
                )
            }
        }
    }

    fun navigateTo(route: AppRoute) {
        _uiState.update { it.copy(route = route) }
    }

    fun selectTopic(topic: GrammarTopic) {
        _uiState.update { it.copy(activeTopic = topic) }
    }

    fun openTopicBrowse(topic: GrammarTopic) {
        _uiState.update { it.copy(activeTopic = topic, route = AppRoute.Browse) }
    }

    fun openTopicExamBuilder(topic: GrammarTopic) {
        _uiState.update { it.copy(activeTopic = topic, route = AppRoute.ExamBuilder) }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun loadPassageBitmap(assetPath: String): Bitmap? = repo.loadPassageBitmap(assetPath)

    fun toggleBookmark(exerciseKey: String) {
        updateProgress { current ->
            val prev = current.statsByKey[exerciseKey] ?: ExerciseStats()
            val updated = prev.copy(bookmarked = !prev.bookmarked)
            current.copy(statsByKey = current.statsByKey + (exerciseKey to updated))
        }
    }

    fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        updateProgress { current ->
            current.copy(preferences = transform(current.preferences))
        }
    }

    fun saveCustomPassageText(exerciseKey: String, text: String) {
        val cleaned = text.trim()
        updateProgress { current ->
            val nextMap = if (cleaned.isEmpty()) {
                current.customTexts - exerciseKey
            } else {
                current.customTexts + (exerciseKey to cleaned)
            }
            current.copy(customTexts = nextMap)
        }
    }

    fun runOcrForExercise(exercise: ExerciseItem) {
        if (_uiState.value.ocrBusyForKey != null || _uiState.value.batchOcrProgress != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(ocrBusyForKey = exercise.key) }
            val bmp = withContext(Dispatchers.IO) { repo.loadPassageBitmap(exercise.imageAsset) }
            if (bmp != null) {
                val result = OnDeviceOcrHelper.extractPassageText(bmp)
                result.onSuccess { text ->
                    if (text.isNotBlank()) {
                        saveCustomPassageText(exercise.key, text)
                        _uiState.update {
                            it.copy(statusBanner = "Extracted text for ${exercise.topic.shortLabel} #${exercise.id}. Review or edit anytime.")
                        }
                    } else {
                        _uiState.update { it.copy(statusBanner = "No text detected for #${exercise.id}.") }
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(statusBanner = "OCR failed: ${err.message}") }
                }
            }
            _uiState.update { it.copy(ocrBusyForKey = null) }
        }
    }

    fun runBatchOcrAllMissing(topicOnly: GrammarTopic? = null) {
        if (_uiState.value.batchOcrProgress != null) return
        viewModelScope.launch {
            val state = _uiState.value
            val pool = if (topicOnly != null) state.exercisesFor(topicOnly) else state.exercises
            val targets = pool.filter { !state.progress.customTexts.containsKey(it.key) }
            if (targets.isEmpty()) {
                _uiState.update { it.copy(statusBanner = "All passages in this set already have extracted text.") }
                return@launch
            }
            val extracted = HashMap<String, String>()
            targets.forEachIndexed { idx, ex ->
                _uiState.update { it.copy(batchOcrProgress = (idx + 1) to targets.size) }
                val bmp = withContext(Dispatchers.IO) { repo.loadPassageBitmap(ex.imageAsset) }
                if (bmp != null) {
                    OnDeviceOcrHelper.extractPassageText(bmp).onSuccess { txt ->
                        if (txt.isNotBlank()) extracted[ex.key] = txt
                    }
                }
            }
            if (extracted.isNotEmpty()) {
                updateProgress { cur -> cur.copy(customTexts = cur.customTexts + extracted) }
            }
            _uiState.update {
                it.copy(
                    batchOcrProgress = null,
                    statusBanner = "On-device scan complete (${extracted.size}/${targets.size} passages)."
                )
            }
        }
    }

    fun buildExerciseList(config: ExamConfig): List<ExerciseItem> {
        val state = _uiState.value
        val topicPool = when (config.topicFilter) {
            ExamTopicFilter.ARTICLE -> state.articleExercises
            ExamTopicFilter.PREPOSITION -> state.prepositionExercises
            ExamTopicFilter.BOTH -> state.exercises
        }
        val stats = state.progress.statsByKey

        val filtered = when (config.selectionMode) {
            SelectionMode.ALL -> topicPool
            SelectionMode.BOARD_ONLY -> topicPool.filter { it.isBoardQuestion }
            SelectionMode.PRACTICE_ONLY -> topicPool.filter { !it.isBoardQuestion }
            SelectionMode.RANGE -> {
                val lo = minOf(config.rangeStart, config.rangeEnd)
                val hi = maxOf(config.rangeStart, config.rangeEnd)
                topicPool.filter { it.id in lo..hi }
            }
            SelectionMode.RANDOM -> topicPool.shuffled().take(config.randomCount.coerceIn(1, topicPool.size.coerceAtLeast(1)))
            SelectionMode.MISTAKES -> topicPool.filter { (stats[it.key]?.wrongBlankKeys?.isNotEmpty()) == true }
            SelectionMode.BOOKMARKED -> topicPool.filter { stats[it.key]?.bookmarked == true }
            SelectionMode.UNATTEMPTED -> topicPool.filter { (stats[it.key]?.attempts ?: 0) == 0 }
            SelectionMode.CUSTOM -> topicPool.filter { it.key in config.customSelectedKeys }
        }

        return if (config.shuffleOrder && config.selectionMode != SelectionMode.RANDOM) {
            filtered.shuffled()
        } else {
            filtered
        }
    }

    fun startSinglePractice(exercise: ExerciseItem) {
        val prefs = _uiState.value.progress.preferences
        val filter = if (exercise.topic == GrammarTopic.ARTICLE) ExamTopicFilter.ARTICLE else ExamTopicFilter.PREPOSITION
        startSessionWithList(
            config = ExamConfig(
                title = "${exercise.topic.label} #${exercise.id} Practice",
                topicFilter = filter,
                sessionType = SessionType.PRACTICE,
                selectionMode = SelectionMode.RANGE,
                rangeStart = exercise.id,
                rangeEnd = exercise.id,
                timerMode = TimerMode.NONE,
                instantFeedback = true,
                strictCapitalization = prefs.defaultStrictCase,
                hscHalfMarkScale = prefs.defaultHscHalfMark
            ),
            items = listOf(exercise)
        )
    }

    fun startSinglePracticeByKey(exerciseKey: String) {
        val ex = _uiState.value.exercises.find { it.key == exerciseKey } ?: return
        startSinglePractice(ex)
    }

    fun startSession(config: ExamConfig): Boolean {
        val items = buildExerciseList(config)
        if (items.isEmpty()) {
            _uiState.update { it.copy(statusBanner = "No passages match this filter.") }
            return false
        }
        startSessionWithList(config, items)
        return true
    }

    fun retryWrongPassages(result: ExamResult) {
        val wrongItems = result.passageResults
            .filter { it.correctCount < BLANK_KEYS.size }
            .map { it.exercise }
        if (wrongItems.isEmpty()) return
        startSessionWithList(
            config = result.config.copy(
                title = "Retry Mistakes (${wrongItems.size})",
                sessionType = SessionType.PRACTICE,
                instantFeedback = true,
                timerMode = TimerMode.NONE
            ),
            items = wrongItems
        )
    }

    private fun startSessionWithList(config: ExamConfig, items: List<ExerciseItem>) {
        timerJob?.cancel()
        val totalRem = if (config.timerMode == TimerMode.TOTAL_MINUTES) config.totalMinutes * 60 else null
        val perRem = if (config.timerMode == TimerMode.PER_PASSAGE_SECONDS) config.perPassageSeconds else null

        _uiState.update {
            it.copy(
                route = AppRoute.Session,
                activeSession = ActiveSessionState(
                    config = config,
                    exercises = items,
                    currentIndex = 0,
                    selectedBlankKey = "a",
                    remainingSeconds = totalRem,
                    perPassageRemainingSeconds = perRem,
                    showWordBankHint = config.showWordBankHint
                )
            )
        }
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val session = _uiState.value.activeSession ?: break
                if (_uiState.value.route != AppRoute.Session || session.isPaused) continue

                val nextElapsed = session.elapsedSeconds + 1
                var expired = false
                var nextTotalRem = session.remainingSeconds
                var nextPerRem = session.perPassageRemainingSeconds

                if (nextTotalRem != null) {
                    nextTotalRem = (nextTotalRem - 1).coerceAtLeast(0)
                    if (nextTotalRem == 0) expired = true
                }
                if (nextPerRem != null) {
                    nextPerRem = (nextPerRem - 1).coerceAtLeast(0)
                    if (nextPerRem == 0) {
                        if (session.currentIndex < session.exercises.lastIndex) {
                            _uiState.update { st ->
                                val s = st.activeSession ?: return@update st
                                st.copy(
                                    activeSession = s.copy(
                                        currentIndex = s.currentIndex + 1,
                                        selectedBlankKey = "a",
                                        elapsedSeconds = nextElapsed,
                                        remainingSeconds = nextTotalRem,
                                        perPassageRemainingSeconds = s.config.perPassageSeconds
                                    )
                                )
                            }
                            continue
                        } else {
                            expired = true
                        }
                    }
                }

                if (expired && session.config.autoSubmitOnTimeout) {
                    _uiState.update { st ->
                        st.copy(
                            activeSession = st.activeSession?.copy(
                                elapsedSeconds = nextElapsed,
                                remainingSeconds = nextTotalRem,
                                perPassageRemainingSeconds = nextPerRem
                            )
                        )
                    }
                    submitSession(timedOut = true)
                    break
                } else {
                    _uiState.update { st ->
                        st.copy(
                            activeSession = st.activeSession?.copy(
                                elapsedSeconds = nextElapsed,
                                remainingSeconds = nextTotalRem,
                                perPassageRemainingSeconds = nextPerRem,
                                timeExpiredNotice = expired || (st.activeSession?.timeExpiredNotice == true)
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectBlank(blankKey: String) {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            st.copy(activeSession = s.copy(selectedBlankKey = blankKey))
        }
    }

    fun setAnswerForSelectedBlank(value: String?, advanceIfEnabled: Boolean = true) {
        val autoAdvance = _uiState.value.progress.preferences.autoAdvanceBlank && advanceIfEnabled
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            val ex = s.currentExercise ?: return@update st
            val currentMap = s.answersByKey[ex.key].orEmpty().toMutableMap()
            val normalized = AnswerEvaluator.normalizeInput(value, ex.topic)
            if (normalized == null) {
                currentMap.remove(s.selectedBlankKey)
            } else {
                currentMap[s.selectedBlankKey] = normalized
            }

            val nextBlank = if (autoAdvance && normalized != null) {
                val curIdx = BLANK_KEYS.indexOf(s.selectedBlankKey)
                val nextUnfilled = (1..BLANK_KEYS.size).firstNotNullOfOrNull { offset ->
                    val candidate = BLANK_KEYS[(curIdx + offset) % BLANK_KEYS.size]
                    if (!currentMap.containsKey(candidate)) candidate else null
                }
                nextUnfilled ?: BLANK_KEYS[(curIdx + 1).coerceAtMost(BLANK_KEYS.lastIndex)]
            } else {
                s.selectedBlankKey
            }

            st.copy(
                activeSession = s.copy(
                    answersByKey = s.answersByKey + (ex.key to currentMap),
                    selectedBlankKey = nextBlank
                )
            )
        }
    }

    fun advanceToNextBlank() {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            val ex = s.currentExercise ?: return@update st
            val currentMap = s.answersByKey[ex.key].orEmpty()
            val curIdx = BLANK_KEYS.indexOf(s.selectedBlankKey)
            val nextUnfilled = (1..BLANK_KEYS.size).firstNotNullOfOrNull { offset ->
                val candidate = BLANK_KEYS[(curIdx + offset) % BLANK_KEYS.size]
                if (!currentMap.containsKey(candidate)) candidate else null
            }
            val nextBlank = nextUnfilled ?: BLANK_KEYS[(curIdx + 1).coerceAtMost(BLANK_KEYS.lastIndex)]
            st.copy(activeSession = s.copy(selectedBlankKey = nextBlank))
        }
    }

    fun toggleWordBankHint() {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            st.copy(activeSession = s.copy(showWordBankHint = !s.showWordBankHint))
        }
    }

    fun toggleCurrentPassageCheck() {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            val ex = s.currentExercise ?: return@update st
            val nextSet = if (ex.key in s.checkedExerciseKeys) {
                s.checkedExerciseKeys - ex.key
            } else {
                s.checkedExerciseKeys + ex.key
            }
            st.copy(activeSession = s.copy(checkedExerciseKeys = nextSet))
        }
    }

    fun revealSingleHint() {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            val ex = s.currentExercise ?: return@update st
            val blankKey = s.selectedBlankKey
            val expected = ex.answers[blankKey]?.split("/")?.firstOrNull() ?: return@update st
            val currentMap = s.answersByKey[ex.key].orEmpty() + (blankKey to expected)
            val revealedForEx = s.revealedBlanksByKey[ex.key].orEmpty() + blankKey
            st.copy(
                activeSession = s.copy(
                    answersByKey = s.answersByKey + (ex.key to currentMap),
                    revealedBlanksByKey = s.revealedBlanksByKey + (ex.key to revealedForEx)
                )
            )
        }
    }

    fun revealAllSolutionsForCurrent() {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            val ex = s.currentExercise ?: return@update st
            val filled = BLANK_KEYS.associateWith { key ->
                ex.answers[key]?.split("/")?.firstOrNull() ?: ""
            }
            st.copy(
                activeSession = s.copy(
                    answersByKey = s.answersByKey + (ex.key to filled),
                    checkedExerciseKeys = s.checkedExerciseKeys + ex.key,
                    revealedBlanksByKey = s.revealedBlanksByKey + (ex.key to BLANK_KEYS.toSet())
                )
            )
        }
    }

    fun clearCurrentPassageAnswers() {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            val ex = s.currentExercise ?: return@update st
            st.copy(
                activeSession = s.copy(
                    answersByKey = s.answersByKey - ex.key,
                    checkedExerciseKeys = s.checkedExerciseKeys - ex.key,
                    revealedBlanksByKey = s.revealedBlanksByKey - ex.key,
                    selectedBlankKey = "a"
                )
            )
        }
    }

    fun toggleFlagCurrent() {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            val ex = s.currentExercise ?: return@update st
            val next = if (ex.key in s.flaggedExerciseKeys) s.flaggedExerciseKeys - ex.key else s.flaggedExerciseKeys + ex.key
            st.copy(activeSession = s.copy(flaggedExerciseKeys = next))
        }
    }

    fun togglePauseTimer() {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            st.copy(activeSession = s.copy(isPaused = !s.isPaused))
        }
    }

    fun jumpToPassage(index: Int) {
        _uiState.update { st ->
            val s = st.activeSession ?: return@update st
            val clamped = index.coerceIn(0, s.exercises.lastIndex)
            val perReset = if (s.config.timerMode == TimerMode.PER_PASSAGE_SECONDS) {
                s.config.perPassageSeconds
            } else {
                s.perPassageRemainingSeconds
            }
            st.copy(
                activeSession = s.copy(
                    currentIndex = clamped,
                    selectedBlankKey = "a",
                    perPassageRemainingSeconds = perReset
                )
            )
        }
    }

    fun submitSession(timedOut: Boolean = false) {
        val session = _uiState.value.activeSession ?: return
        timerJob?.cancel()

        val passageResults = session.exercises.map { ex ->
            val ans = session.answersByKey[ex.key].orEmpty()
            AnswerEvaluator.evaluatePassage(ex, ans, session.config)
        }
        val totalCorrect = passageResults.sumOf { it.correctCount }
        val totalWrong = passageResults.sumOf { it.wrongCount }
        val totalUnanswered = passageResults.sumOf { it.unansweredCount }
        val totalBlanks = passageResults.size * BLANK_KEYS.size
        val totalScore = passageResults.sumOf { it.rawScore }
        val maxScore = passageResults.sumOf { it.maxRawScore }

        val now = System.currentTimeMillis()
        val autoSaveFlash = _uiState.value.progress.preferences.autoSaveMistakesToFlashcards

        // Count how many non-correct blanks will be recorded into Flashcard SQLite DB
        val mistakeTriples = if (autoSaveFlash) {
            passageResults.flatMap { pr ->
                pr.blankOutcomes.filter { !it.isCorrect }.map { bo ->
                    Triple(pr.exercise, bo.blankKey, bo)
                }
            }
        } else {
            emptyList()
        }

        if (mistakeTriples.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                mistakeTriples.forEach { (ex, bKey, bo) ->
                    repo.flashcardDb.recordMistake(
                        exercise = ex,
                        blankKey = bKey,
                        wrongAnswer = bo.userAnswer,
                        expectedAnswer = bo.expectedAnswer,
                        nowMs = now
                    )
                }
                val refreshed = repo.flashcardDb.getAllCards()
                _uiState.update { it.copy(flashcards = refreshed) }
            }
        }

        val result = ExamResult(
            config = session.config,
            passageResults = passageResults,
            totalCorrect = totalCorrect,
            totalWrong = totalWrong,
            totalUnanswered = totalUnanswered,
            totalBlanks = totalBlanks,
            totalScore = totalScore,
            maxScore = maxScore,
            elapsedSeconds = session.elapsedSeconds,
            timedOut = timedOut,
            flashcardsAddedOrUpdated = mistakeTriples.size
        )

        updateProgress { current ->
            val nextStats = current.statsByKey.toMutableMap()
            passageResults.forEach { pr ->
                val exKey = pr.exercise.key
                val prev = nextStats[exKey] ?: ExerciseStats()
                val wrongKeys = pr.blankOutcomes
                    .filter { !it.isCorrect }
                    .map { it.blankKey }
                    .toSet()
                nextStats[exKey] = prev.copy(
                    attempts = prev.attempts + 1,
                    bestCorrect = maxOf(prev.bestCorrect, pr.correctCount),
                    lastCorrect = pr.correctCount,
                    wrongBlankKeys = wrongKeys,
                    mastered = prev.mastered || (pr.correctCount == BLANK_KEYS.size),
                    lastAttemptTimestamp = now
                )
            }
            val historyEntry = ExamHistoryEntry(
                id = UUID.randomUUID().toString(),
                title = session.config.title,
                timestamp = now,
                topicFilter = session.config.topicFilter,
                sessionType = session.config.sessionType,
                passageKeys = session.exercises.map { it.key },
                totalBlanks = totalBlanks,
                correctCount = totalCorrect,
                wrongCount = totalWrong,
                blankCount = totalUnanswered,
                rawScore = totalScore,
                maxRawScore = maxScore,
                durationSeconds = session.elapsedSeconds,
                strictCapitalization = session.config.strictCapitalization,
                hscHalfMarkScale = session.config.hscHalfMarkScale
            )
            current.copy(
                statsByKey = nextStats,
                history = listOf(historyEntry) + current.history.take(119)
            )
        }

        _uiState.update {
            it.copy(
                route = AppRoute.Result,
                activeSession = null,
                lastResult = result
            )
        }
    }

    fun abandonSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(route = AppRoute.Home, activeSession = null) }
    }

    // ==================== FLASHCARD / FLASH MEMORY ACTIONS ====================

    fun pinBlankToFlashcards(exercise: ExerciseItem, blankKey: String, note: String = "") {
        val expected = exercise.answers[blankKey] ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repo.flashcardDb.pinPassageBlank(exercise, blankKey, expected, note)
            val updated = repo.flashcardDb.getAllCards()
            _uiState.update {
                it.copy(
                    flashcards = updated,
                    statusBanner = "Saved ${exercise.topic.shortLabel} #${exercise.id} ($blankKey) to Flash Memory."
                )
            }
        }
    }

    fun addCustomFlashcard(topic: GrammarTopic, prompt: String, answer: String, note: String) {
        if (prompt.isBlank() || answer.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repo.flashcardDb.addCustomCard(topic, prompt, answer, note)
            val updated = repo.flashcardDb.getAllCards()
            _uiState.update {
                it.copy(
                    flashcards = updated,
                    statusBanner = "Added custom ${topic.label} flashcard."
                )
            }
        }
    }

    fun reviewFlashcard(card: FlashcardEntity, rating: ReviewRating) {
        val updatedCard = SpacedRepetitionEngine.applyReview(card, rating)
        viewModelScope.launch(Dispatchers.IO) {
            repo.flashcardDb.upsertCard(updatedCard)
            val updated = repo.flashcardDb.getAllCards()
            _uiState.update { it.copy(flashcards = updated) }
        }
    }

    fun updateFlashcardNote(cardKey: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.flashcardDb.updateUserNote(cardKey, note)
            val updated = repo.flashcardDb.getAllCards()
            _uiState.update { it.copy(flashcards = updated) }
        }
    }

    fun deleteFlashcard(cardKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.flashcardDb.deleteCard(cardKey)
            val updated = repo.flashcardDb.getAllCards()
            _uiState.update {
                it.copy(
                    flashcards = updated,
                    statusBanner = "Flashcard removed."
                )
            }
        }
    }

    fun clearFlashcards(topic: GrammarTopic? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.flashcardDb.clearCards(topic)
            val updated = repo.flashcardDb.getAllCards()
            _uiState.update {
                it.copy(
                    flashcards = updated,
                    statusBanner = if (topic == null) "All flashcards cleared." else "${topic.label} flashcards cleared."
                )
            }
        }
    }

    // ==================== BACKUP & RESET ====================

    fun exportProgressJson(): String = repo.exportAllDataJson(_uiState.value.progress)

    fun importProgressJson(raw: String) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) { repo.importDataJson(raw) }
            res.onSuccess { loaded ->
                val cards = withContext(Dispatchers.IO) { repo.flashcardDb.getAllCards() }
                _uiState.update {
                    it.copy(
                        progress = loaded,
                        flashcards = cards,
                        statusBanner = "Imported progress, texts, and ${cards.size} flashcards."
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(statusBanner = "Invalid JSON: ${err.message}") }
            }
        }
    }

    fun clearAllProgress() {
        val prefs = _uiState.value.progress.preferences
        val texts = _uiState.value.progress.customTexts
        val clean = UserProgressData(customTexts = texts, preferences = prefs)
        viewModelScope.launch(Dispatchers.IO) { repo.saveProgress(clean) }
        _uiState.update { it.copy(progress = clean, statusBanner = "Practice & exam statistics reset.") }
    }

    private fun updateProgress(transform: (UserProgressData) -> UserProgressData) {
        val updated = transform(_uiState.value.progress)
        _uiState.update { it.copy(progress = updated) }
        viewModelScope.launch(Dispatchers.IO) { repo.saveProgress(updated) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        repo.flashcardDb.close()
        super.onCleared()
    }
}
