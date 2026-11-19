package com.articlemaster.hsc.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.FlashcardEntity
import com.articlemaster.hsc.model.FlashcardStatus
import com.articlemaster.hsc.model.GrammarTopic
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class FlashcardDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_FLASHCARDS (
                $COL_CARD_KEY TEXT PRIMARY KEY NOT NULL,
                $COL_TOPIC TEXT NOT NULL,
                $COL_EXERCISE_ID INTEGER NOT NULL,
                $COL_BLANK_KEY TEXT NOT NULL,
                $COL_BOARD_INFO TEXT NOT NULL,
                $COL_IMAGE_ASSET TEXT,
                $COL_CUSTOM_PROMPT TEXT,
                $COL_CORRECT_ANSWER TEXT NOT NULL,
                $COL_LAST_WRONG TEXT,
                $COL_MISTAKE_COUNT INTEGER NOT NULL DEFAULT 1,
                $COL_REVIEW_COUNT INTEGER NOT NULL DEFAULT 0,
                $COL_STREAK INTEGER NOT NULL DEFAULT 0,
                $COL_EASE_FACTOR REAL NOT NULL DEFAULT 2.5,
                $COL_INTERVAL_DAYS INTEGER NOT NULL DEFAULT 0,
                $COL_NEXT_REVIEW_AT INTEGER NOT NULL DEFAULT 0,
                $COL_STATUS TEXT NOT NULL DEFAULT 'LEARNING',
                $COL_IS_MANUAL INTEGER NOT NULL DEFAULT 0,
                $COL_USER_NOTE TEXT NOT NULL DEFAULT '',
                $COL_CREATED_AT INTEGER NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX idx_flashcards_topic_due ON $TABLE_FLASHCARDS ($COL_TOPIC, $COL_STATUS, $COL_NEXT_REVIEW_AT)"
        )
        db.execSQL(
            "CREATE INDEX idx_flashcards_mistakes ON $TABLE_FLASHCARDS ($COL_MISTAKE_COUNT DESC, $COL_UPDATED_AT DESC)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Initial version 1
    }

    fun getAllCards(): List<FlashcardEntity> {
        val list = ArrayList<FlashcardEntity>()
        val db = readableDatabase
        db.query(
            TABLE_FLASHCARDS,
            null,
            null,
            null,
            null,
            null,
            "$COL_NEXT_REVIEW_AT ASC, $COL_MISTAKE_COUNT DESC, $COL_UPDATED_AT DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToEntity(cursor))
            }
        }
        return list
    }

    fun getCardByKey(cardKey: String): FlashcardEntity? {
        val db = readableDatabase
        db.query(
            TABLE_FLASHCARDS,
            null,
            "$COL_CARD_KEY = ?",
            arrayOf(cardKey),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursorToEntity(cursor) else null
        }
    }

    fun upsertCard(card: FlashcardEntity) {
        writableDatabase.insertWithOnConflict(
            TABLE_FLASHCARDS,
            null,
            entityToValues(card),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun recordMistake(
        exercise: ExerciseItem,
        blankKey: String,
        wrongAnswer: String?,
        expectedAnswer: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val key = FlashcardEntity.makeCardKey(exercise.topic, exercise.id, blankKey)
        val existing = getCardByKey(key)
        val updated = if (existing != null) {
            existing.copy(
                boardInfo = exercise.boardInfo,
                imageAsset = exercise.imageAsset,
                correctAnswer = expectedAnswer,
                lastWrongAnswer = wrongAnswer ?: existing.lastWrongAnswer,
                mistakeCount = existing.mistakeCount + 1,
                streak = 0,
                intervalDays = 0,
                nextReviewAt = nowMs,
                status = FlashcardStatus.LEARNING,
                updatedAt = nowMs
            )
        } else {
            FlashcardEntity(
                cardKey = key,
                topic = exercise.topic,
                exerciseId = exercise.id,
                blankKey = blankKey,
                boardInfo = exercise.boardInfo,
                imageAsset = exercise.imageAsset,
                customPrompt = null,
                correctAnswer = expectedAnswer,
                lastWrongAnswer = wrongAnswer,
                mistakeCount = 1,
                reviewCount = 0,
                streak = 0,
                easeFactor = 2.5,
                intervalDays = 0,
                nextReviewAt = nowMs,
                status = FlashcardStatus.LEARNING,
                isManual = false,
                userNote = "",
                createdAt = nowMs,
                updatedAt = nowMs
            )
        }
        upsertCard(updated)
    }

    fun pinPassageBlank(
        exercise: ExerciseItem,
        blankKey: String,
        expectedAnswer: String,
        note: String = "",
        nowMs: Long = System.currentTimeMillis()
    ): FlashcardEntity {
        val key = FlashcardEntity.makeCardKey(exercise.topic, exercise.id, blankKey)
        val existing = getCardByKey(key)
        val card = if (existing != null) {
            existing.copy(
                isManual = true,
                userNote = if (note.isNotBlank()) note else existing.userNote,
                nextReviewAt = minOf(existing.nextReviewAt, nowMs),
                updatedAt = nowMs
            )
        } else {
            FlashcardEntity(
                cardKey = key,
                topic = exercise.topic,
                exerciseId = exercise.id,
                blankKey = blankKey,
                boardInfo = exercise.boardInfo,
                imageAsset = exercise.imageAsset,
                customPrompt = null,
                correctAnswer = expectedAnswer,
                lastWrongAnswer = null,
                mistakeCount = 0,
                reviewCount = 0,
                streak = 0,
                easeFactor = 2.5,
                intervalDays = 0,
                nextReviewAt = nowMs,
                status = FlashcardStatus.LEARNING,
                isManual = true,
                userNote = note,
                createdAt = nowMs,
                updatedAt = nowMs
            )
        }
        upsertCard(card)
        return card
    }

    fun addCustomCard(
        topic: GrammarTopic,
        prompt: String,
        answer: String,
        note: String,
        nowMs: Long = System.currentTimeMillis()
    ): FlashcardEntity {
        val card = FlashcardEntity(
            cardKey = "CUSTOM_${UUID.randomUUID()}",
            topic = topic,
            exerciseId = 0,
            blankKey = "",
            boardInfo = "Custom Flashcard",
            imageAsset = null,
            customPrompt = prompt.trim(),
            correctAnswer = answer.trim(),
            lastWrongAnswer = null,
            mistakeCount = 0,
            reviewCount = 0,
            streak = 0,
            easeFactor = 2.5,
            intervalDays = 0,
            nextReviewAt = nowMs,
            status = FlashcardStatus.LEARNING,
            isManual = true,
            userNote = note.trim(),
            createdAt = nowMs,
            updatedAt = nowMs
        )
        upsertCard(card)
        return card
    }

    fun updateUserNote(cardKey: String, note: String, nowMs: Long = System.currentTimeMillis()) {
        val cv = ContentValues().apply {
            put(COL_USER_NOTE, note.trim())
            put(COL_UPDATED_AT, nowMs)
        }
        writableDatabase.update(TABLE_FLASHCARDS, cv, "$COL_CARD_KEY = ?", arrayOf(cardKey))
    }

    fun deleteCard(cardKey: String) {
        writableDatabase.delete(TABLE_FLASHCARDS, "$COL_CARD_KEY = ?", arrayOf(cardKey))
    }

    fun clearCards(topic: GrammarTopic? = null) {
        if (topic == null) {
            writableDatabase.delete(TABLE_FLASHCARDS, null, null)
        } else {
            writableDatabase.delete(TABLE_FLASHCARDS, "$COL_TOPIC = ?", arrayOf(topic.name))
        }
    }

    fun exportToJsonArray(): JSONArray {
        val arr = JSONArray()
        getAllCards().forEach { c ->
            val obj = JSONObject()
            obj.put("cardKey", c.cardKey)
            obj.put("topic", c.topic.name)
            obj.put("exerciseId", c.exerciseId)
            obj.put("blankKey", c.blankKey)
            obj.put("boardInfo", c.boardInfo)
            c.imageAsset?.let { obj.put("imageAsset", it) }
            c.customPrompt?.let { obj.put("customPrompt", it) }
            obj.put("correctAnswer", c.correctAnswer)
            c.lastWrongAnswer?.let { obj.put("lastWrongAnswer", it) }
            obj.put("mistakeCount", c.mistakeCount)
            obj.put("reviewCount", c.reviewCount)
            obj.put("streak", c.streak)
            obj.put("easeFactor", c.easeFactor)
            obj.put("intervalDays", c.intervalDays)
            obj.put("nextReviewAt", c.nextReviewAt)
            obj.put("status", c.status.name)
            obj.put("isManual", c.isManual)
            obj.put("userNote", c.userNote)
            obj.put("createdAt", c.createdAt)
            obj.put("updatedAt", c.updatedAt)
            arr.put(obj)
        }
        return arr
    }

    fun importFromJsonArray(arr: JSONArray) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val topic = runCatching {
                    GrammarTopic.valueOf(obj.optString("topic", GrammarTopic.ARTICLE.name))
                }.getOrDefault(GrammarTopic.ARTICLE)
                val status = runCatching {
                    FlashcardStatus.valueOf(obj.optString("status", FlashcardStatus.LEARNING.name))
                }.getOrDefault(FlashcardStatus.LEARNING)
                val card = FlashcardEntity(
                    cardKey = obj.getString("cardKey"),
                    topic = topic,
                    exerciseId = obj.optInt("exerciseId", 0),
                    blankKey = obj.optString("blankKey", ""),
                    boardInfo = obj.optString("boardInfo", ""),
                    imageAsset = if (obj.isNull("imageAsset")) null else obj.optString("imageAsset").takeIf { it.isNotBlank() },
                    customPrompt = if (obj.isNull("customPrompt")) null else obj.optString("customPrompt").takeIf { it.isNotBlank() },
                    correctAnswer = obj.getString("correctAnswer"),
                    lastWrongAnswer = if (obj.isNull("lastWrongAnswer")) null else obj.optString("lastWrongAnswer").takeIf { it.isNotBlank() },
                    mistakeCount = obj.optInt("mistakeCount", 1),
                    reviewCount = obj.optInt("reviewCount", 0),
                    streak = obj.optInt("streak", 0),
                    easeFactor = obj.optDouble("easeFactor", 2.5),
                    intervalDays = obj.optInt("intervalDays", 0),
                    nextReviewAt = obj.optLong("nextReviewAt", now),
                    status = status,
                    isManual = obj.optBoolean("isManual", false),
                    userNote = obj.optString("userNote", ""),
                    createdAt = obj.optLong("createdAt", now),
                    updatedAt = obj.optLong("updatedAt", now)
                )
                db.insertWithOnConflict(
                    TABLE_FLASHCARDS,
                    null,
                    entityToValues(card),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun entityToValues(c: FlashcardEntity): ContentValues = ContentValues().apply {
        put(COL_CARD_KEY, c.cardKey)
        put(COL_TOPIC, c.topic.name)
        put(COL_EXERCISE_ID, c.exerciseId)
        put(COL_BLANK_KEY, c.blankKey)
        put(COL_BOARD_INFO, c.boardInfo)
        put(COL_IMAGE_ASSET, c.imageAsset)
        put(COL_CUSTOM_PROMPT, c.customPrompt)
        put(COL_CORRECT_ANSWER, c.correctAnswer)
        put(COL_LAST_WRONG, c.lastWrongAnswer)
        put(COL_MISTAKE_COUNT, c.mistakeCount)
        put(COL_REVIEW_COUNT, c.reviewCount)
        put(COL_STREAK, c.streak)
        put(COL_EASE_FACTOR, c.easeFactor)
        put(COL_INTERVAL_DAYS, c.intervalDays)
        put(COL_NEXT_REVIEW_AT, c.nextReviewAt)
        put(COL_STATUS, c.status.name)
        put(COL_IS_MANUAL, if (c.isManual) 1 else 0)
        put(COL_USER_NOTE, c.userNote)
        put(COL_CREATED_AT, c.createdAt)
        put(COL_UPDATED_AT, c.updatedAt)
    }

    private fun cursorToEntity(cursor: Cursor): FlashcardEntity {
        val topicStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_TOPIC))
        val statusStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS))
        return FlashcardEntity(
            cardKey = cursor.getString(cursor.getColumnIndexOrThrow(COL_CARD_KEY)),
            topic = runCatching { GrammarTopic.valueOf(topicStr) }.getOrDefault(GrammarTopic.ARTICLE),
            exerciseId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EXERCISE_ID)),
            blankKey = cursor.getString(cursor.getColumnIndexOrThrow(COL_BLANK_KEY)),
            boardInfo = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOARD_INFO)),
            imageAsset = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_ASSET)),
            customPrompt = cursor.getString(cursor.getColumnIndexOrThrow(COL_CUSTOM_PROMPT)),
            correctAnswer = cursor.getString(cursor.getColumnIndexOrThrow(COL_CORRECT_ANSWER)),
            lastWrongAnswer = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAST_WRONG)),
            mistakeCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MISTAKE_COUNT)),
            reviewCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REVIEW_COUNT)),
            streak = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STREAK)),
            easeFactor = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_EASE_FACTOR)),
            intervalDays = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INTERVAL_DAYS)),
            nextReviewAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_NEXT_REVIEW_AT)),
            status = runCatching { FlashcardStatus.valueOf(statusStr) }.getOrDefault(FlashcardStatus.LEARNING),
            isManual = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_MANUAL)) == 1,
            userNote = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NOTE)) ?: "",
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
        )
    }

    companion object {
        private const val DB_NAME = "english_practice_flashcards.db"
        private const val DB_VERSION = 1
        private const val TABLE_FLASHCARDS = "flashcards"

        private const val COL_CARD_KEY = "card_key"
        private const val COL_TOPIC = "topic"
        private const val COL_EXERCISE_ID = "exercise_id"
        private const val COL_BLANK_KEY = "blank_key"
        private const val COL_BOARD_INFO = "board_info"
        private const val COL_IMAGE_ASSET = "image_asset"
        private const val COL_CUSTOM_PROMPT = "custom_prompt"
        private const val COL_CORRECT_ANSWER = "correct_answer"
        private const val COL_LAST_WRONG = "last_wrong"
        private const val COL_MISTAKE_COUNT = "mistake_count"
        private const val COL_REVIEW_COUNT = "review_count"
        private const val COL_STREAK = "streak"
        private const val COL_EASE_FACTOR = "ease_factor"
        private const val COL_INTERVAL_DAYS = "interval_days"
        private const val COL_NEXT_REVIEW_AT = "next_review_at"
        private const val COL_STATUS = "status"
        private const val COL_IS_MANUAL = "is_manual"
        private const val COL_USER_NOTE = "user_note"
        private const val COL_CREATED_AT = "created_at"
        private const val COL_UPDATED_AT = "updated_at"
    }
}
