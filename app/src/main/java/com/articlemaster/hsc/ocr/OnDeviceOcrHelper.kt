package com.articlemaster.hsc.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OnDeviceOcrHelper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractPassageText(bitmap: Bitmap): Result<String> = runCatching {
        val rawText = recognizeText(bitmap)
        cleanOcrPassage(rawText)
    }

    private suspend fun recognizeText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result.text)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }

    /**
     * Normalizes recognized text: removes boilerplate prompt line if detected and
     * standardizes blank markers like "(a) —", "(a) ___", "(a)-" to "(a) ___".
     */
    fun cleanOcrPassage(raw: String): String {
        val lines = raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Skip top page header / prompt instruction lines until board tag or body begins
        val bodyStartIdx = lines.indexOfFirst { line ->
            !line.contains("Fill in the blanks", ignoreCase = true) &&
                !line.contains("may not require an article", ignoreCase = true) &&
                !line.contains("Put a cross", ignoreCase = true) &&
                !line.contains("HSC Communicative", ignoreCase = true) &&
                !line.contains("Second Paper", ignoreCase = true) &&
                !line.contains("EXERCISE FOR PRACTICE", ignoreCase = true) &&
                !line.contains("NCTB", ignoreCase = true) &&
                !line.matches(Regex("^\\d+\\.?$"))
        }.takeIf { it >= 0 } ?: 0

        val filtered = lines.drop(bodyStartIdx).filterNot { line ->
            // Ignore trailing clipped line of next question's prompt if captured
            line.contains("Fill in the blanks", ignoreCase = true) ||
                line.contains("may not require an article", ignoreCase = true) ||
                line.startsWith("[") && line.endsWith("]")
        }

        val joined = filtered.joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\(([a-jA-J])\\)\\s*[—_\\-–]+"), "($1) ___")
            .replace(Regex("\\(([a-jA-J])\\)\\s+"), "($1) ")

        return joined.trim()
    }
}
