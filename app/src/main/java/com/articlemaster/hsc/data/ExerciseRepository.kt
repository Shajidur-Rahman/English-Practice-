package com.articlemaster.hsc.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.articlemaster.hsc.model.AppPreferences
import com.articlemaster.hsc.model.BLANK_KEYS
import com.articlemaster.hsc.model.ExamHistoryEntry
import com.articlemaster.hsc.model.ExamTopicFilter
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.ExerciseStats
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.MothersLayoutMode
import com.articlemaster.hsc.model.SessionType
import com.articlemaster.hsc.model.UserProgressData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ExerciseRepository(private val context: Context) {

    private val progressFile = File(context.filesDir, "user_progress.json")
    private val bitmapCache = object : LruCache<String, Bitmap>(20) {}

    val flashcardDb = FlashcardDatabaseHelper(context)

    fun loadAllExercises(): List<ExerciseItem> {
        val articles = loadTopicAsset("answer_keys.json", GrammarTopic.ARTICLE)
        val prepositions = loadTopicAsset("preposition_answer_keys.json", GrammarTopic.PREPOSITION)
        return articles + prepositions
    }

    private fun loadTopicAsset(assetName: String, topic: GrammarTopic): List<ExerciseItem> {
        val rawJson = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val root = JSONObject(rawJson)
        val array = root.getJSONArray("exercises")
        val list = ArrayList<ExerciseItem>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val ansObj = obj.getJSONObject("answers")
            val ansMap = LinkedHashMap<String, String>(10)
            for (k in BLANK_KEYS) {
                ansMap[k] = ansObj.getString(k)
            }
            val note = if (obj.isNull("note")) null else obj.optString("note").takeIf { it.isNotBlank() }
            val pageVal = obj.optString("page", "0").toIntOrNull() ?: obj.optInt("page", 0)
            list.add(
                ExerciseItem(
                    id = obj.getInt("id"),
                    topic = topic,
                    page = pageVal,
                    boardInfo = obj.getString("boardInfo"),
                    isBoardQuestion = obj.getBoolean("isBoardQuestion"),
                    imageAsset = obj.getString("imageAsset"),
                    answers = ansMap,
                    note = note
                )
            )
        }
        return list
    }

    fun loadPassageBitmap(assetPath: String): Bitmap? {
        bitmapCache.get(assetPath)?.let { return it }
        return try {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.also { bmp ->
                    bitmapCache.put(assetPath, bmp)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun loadProgress(): UserProgressData {
        if (!progressFile.exists()) return UserProgressData()
        return try {
            parseProgressJson(progressFile.readText())
        } catch (_: Exception) {
            UserProgressData()
        }
    }

    fun saveProgress(data: UserProgressData) {
        progressFile.writeText(serializeProgressJson(data, includeFlashcards = false))
    }

    fun exportAllDataJson(data: UserProgressData): String =
        serializeProgressJson(data, includeFlashcards = true)

    fun importDataJson(rawJson: String): Result<UserProgressData> {
        return runCatching {
            val root = JSONObject(rawJson)
            val parsed = parseProgressJson(rawJson)
            saveProgress(parsed)
            root.optJSONArray("flashcards")?.let { arr ->
                flashcardDb.importFromJsonArray(arr)
            }
            parsed
        }
    }

    private fun serializeProgressJson(data: UserProgressData, includeFlashcards: Boolean): String {
        val root = JSONObject()
        root.put("schemaVersion", 2)

        val statsObj = JSONObject()
        data.statsByKey.forEach { (key, st) ->
            val obj = JSONObject()
            obj.put("attempts", st.attempts)
            obj.put("bestCorrect", st.bestCorrect)
            obj.put("lastCorrect", st.lastCorrect)
            obj.put("wrongBlankKeys", JSONArray(st.wrongBlankKeys.toList()))
            obj.put("bookmarked", st.bookmarked)
            obj.put("mastered", st.mastered)
            obj.put("lastAttemptTimestamp", st.lastAttemptTimestamp)
            statsObj.put(key, obj)
        }
        root.put("statsByKey", statsObj)

        val historyArr = JSONArray()
        data.history.take(120).forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("title", entry.title)
            obj.put("timestamp", entry.timestamp)
            obj.put("topicFilter", entry.topicFilter.name)
            obj.put("sessionType", entry.sessionType.name)
            obj.put("passageKeys", JSONArray(entry.passageKeys))
            obj.put("totalBlanks", entry.totalBlanks)
            obj.put("correctCount", entry.correctCount)
            obj.put("wrongCount", entry.wrongCount)
            obj.put("blankCount", entry.blankCount)
            obj.put("rawScore", entry.rawScore)
            obj.put("maxRawScore", entry.maxRawScore)
            obj.put("durationSeconds", entry.durationSeconds)
            obj.put("strictCapitalization", entry.strictCapitalization)
            obj.put("hscHalfMarkScale", entry.hscHalfMarkScale)
            historyArr.put(obj)
        }
        root.put("history", historyArr)

        val textsObj = JSONObject()
        data.customTexts.forEach { (key, text) ->
            if (text.isNotBlank()) textsObj.put(key, text)
        }
        root.put("customTexts", textsObj)

        val prefObj = JSONObject()
        data.preferences.darkMode?.let { prefObj.put("darkMode", it) }
        prefObj.put("invertPassageInDark", data.preferences.invertPassageInDark)
        prefObj.put("autoAdvanceBlank", data.preferences.autoAdvanceBlank)
        prefObj.put("defaultStrictCase", data.preferences.defaultStrictCase)
        prefObj.put("defaultHscHalfMark", data.preferences.defaultHscHalfMark)
        prefObj.put("preferTextViewWhenAvailable", data.preferences.preferTextViewWhenAvailable)
        prefObj.put("autoSaveMistakesToFlashcards", data.preferences.autoSaveMistakesToFlashcards)
        prefObj.put("mothersLayout", data.preferences.mothersLayout.name)
        prefObj.put("mothersTextScale", data.preferences.mothersTextScale.toDouble())
        prefObj.put("mothersMinimalHeader", data.preferences.mothersMinimalHeader)
        root.put("preferences", prefObj)

        if (includeFlashcards) {
            root.put("flashcards", flashcardDb.exportToJsonArray())
        }

        return root.toString(2)
    }

    private fun migrateKey(rawKey: String): String {
        val asInt = rawKey.toIntOrNull()
        return if (asInt != null) ExerciseItem.makeExerciseKey(GrammarTopic.ARTICLE, asInt) else rawKey
    }

    private fun parseProgressJson(raw: String): UserProgressData {
        val root = JSONObject(raw)

        val statsMap = HashMap<String, ExerciseStats>()
        val statsNode = root.optJSONObject("statsByKey") ?: root.optJSONObject("statsById")
        statsNode?.let { statsObj ->
            val keys = statsObj.keys()
            while (keys.hasNext()) {
                val rawKey = keys.next()
                val key = migrateKey(rawKey)
                val obj = statsObj.getJSONObject(rawKey)
                val wrongSet = HashSet<String>()
                obj.optJSONArray("wrongBlankKeys")?.let { arr ->
                    for (i in 0 until arr.length()) wrongSet.add(arr.getString(i))
                }
                statsMap[key] = ExerciseStats(
                    attempts = obj.optInt("attempts", 0),
                    bestCorrect = obj.optInt("bestCorrect", 0),
                    lastCorrect = obj.optInt("lastCorrect", 0),
                    wrongBlankKeys = wrongSet,
                    bookmarked = obj.optBoolean("bookmarked", false),
                    mastered = obj.optBoolean("mastered", false),
                    lastAttemptTimestamp = obj.optLong("lastAttemptTimestamp", 0L)
                )
            }
        }

        val historyList = ArrayList<ExamHistoryEntry>()
        root.optJSONArray("history")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pKeys = ArrayList<String>()
                val keyArr = obj.optJSONArray("passageKeys")
                if (keyArr != null) {
                    for (j in 0 until keyArr.length()) pKeys.add(keyArr.getString(j))
                } else {
                    obj.optJSONArray("passageIds")?.let { idArr ->
                        for (j in 0 until idArr.length()) {
                            pKeys.add(ExerciseItem.makeExerciseKey(GrammarTopic.ARTICLE, idArr.getInt(j)))
                        }
                    }
                }
                val sType = runCatching {
                    SessionType.valueOf(obj.optString("sessionType", SessionType.PRACTICE.name))
                }.getOrDefault(SessionType.PRACTICE)
                val tFilter = runCatching {
                    ExamTopicFilter.valueOf(obj.optString("topicFilter", ExamTopicFilter.ARTICLE.name))
                }.getOrDefault(ExamTopicFilter.ARTICLE)

                historyList.add(
                    ExamHistoryEntry(
                        id = obj.optString("id", i.toString()),
                        title = obj.optString("title", "Session"),
                        timestamp = obj.optLong("timestamp", 0L),
                        topicFilter = tFilter,
                        sessionType = sType,
                        passageKeys = pKeys,
                        totalBlanks = obj.optInt("totalBlanks", 10),
                        correctCount = obj.optInt("correctCount", 0),
                        wrongCount = obj.optInt("wrongCount", 0),
                        blankCount = obj.optInt("blankCount", 0),
                        rawScore = obj.optDouble("rawScore", 0.0),
                        maxRawScore = obj.optDouble("maxRawScore", 5.0),
                        durationSeconds = obj.optInt("durationSeconds", 0),
                        strictCapitalization = obj.optBoolean("strictCapitalization", false),
                        hscHalfMarkScale = obj.optBoolean("hscHalfMarkScale", true)
                    )
                )
            }
        }

        val textsMap = HashMap<String, String>()
        root.optJSONObject("customTexts")?.let { tObj ->
            val keys = tObj.keys()
            while (keys.hasNext()) {
                val rawKey = keys.next()
                val key = migrateKey(rawKey)
                val txt = tObj.optString(rawKey)
                if (txt.isNotBlank()) textsMap[key] = txt
            }
        }

        val pObj = root.optJSONObject("preferences")
        val prefs = if (pObj != null) {
            AppPreferences(
                darkMode = if (pObj.has("darkMode") && !pObj.isNull("darkMode")) pObj.getBoolean("darkMode") else null,
                invertPassageInDark = pObj.optBoolean("invertPassageInDark", true),
                autoAdvanceBlank = pObj.optBoolean("autoAdvanceBlank", true),
                defaultStrictCase = pObj.optBoolean("defaultStrictCase", false),
                defaultHscHalfMark = pObj.optBoolean("defaultHscHalfMark", true),
                preferTextViewWhenAvailable = pObj.optBoolean("preferTextViewWhenAvailable", true),
                autoSaveMistakesToFlashcards = pObj.optBoolean("autoSaveMistakesToFlashcards", true),
                mothersLayout = runCatching {
                    MothersLayoutMode.valueOf(pObj.optString("mothersLayout", MothersLayoutMode.SPLIT_5_5.name))
                }.getOrDefault(MothersLayoutMode.SPLIT_5_5),
                mothersTextScale = pObj.optDouble("mothersTextScale", 1.15).toFloat().coerceIn(0.85f, 1.8f),
                mothersMinimalHeader = pObj.optBoolean("mothersMinimalHeader", false)
            )
        } else {
            AppPreferences()
        }

        return UserProgressData(
            statsByKey = statsMap,
            history = historyList,
            customTexts = textsMap,
            preferences = prefs
        )
    }
}
