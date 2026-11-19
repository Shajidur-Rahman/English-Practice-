package com.articlemaster.hsc.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.BLANK_KEYS
import com.articlemaster.hsc.model.COMMON_PREPOSITIONS
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.ui.theme.AmberAccent
import com.articlemaster.hsc.ui.theme.EmeraldContainer
import com.articlemaster.hsc.ui.theme.EmeraldSuccess
import com.articlemaster.hsc.ui.theme.RoseContainer
import com.articlemaster.hsc.ui.theme.RoseError
import java.util.Locale

private val DarkPaperInvertFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -0.92f, 0f, 0f, 0f, 240f,
            0f, -0.92f, 0f, 0f, 240f,
            0f, 0f, -0.92f, 0f, 240f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

@Composable
fun FullscreenZoomDialog(
    bitmap: Bitmap,
    colorFilter: ColorFilter?,
    isDarkBg: Boolean,
    title: String = "Pinch or double-tap to zoom",
    onDismiss: () -> Unit
) {
    var scale by remember(bitmap) { mutableFloatStateOf(1.35f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkBg) Color(0xFF0A0F1A) else Color(0xFF111827))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bitmap) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.5f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.6f
                                }
                            }
                        )
                    }
                    .pointerInput(bitmap) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 5.5f)
                            scale = nextScale
                            if (nextScale > 1.02f) {
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-1200f, 1200f),
                                    y = (offset.y + pan.y).coerceIn(-900f, 900f)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Fullscreen passage",
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }

            // Top overlay bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$title • ${String.format(Locale.US, "%.1f", scale)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                scale = if (scale > 1.6f) 1f else 2.5f
                                offset = Offset.Zero
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (scale > 1.6f) Icons.Default.ZoomOutMap else Icons.Default.ZoomIn,
                                contentDescription = "Toggle zoom",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomablePassageImage(
    bitmap: Bitmap?,
    invertInDarkMode: Boolean,
    darkModeOverride: Boolean?,
    minHeight: Dp = 110.dp,
    maxHeight: Dp = 280.dp,
    fullscreenTitle: String = "Zoomed View",
    modifier: Modifier = Modifier
) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var showFullscreen by remember { mutableStateOf(false) }
    val isDark = darkModeOverride ?: isSystemInDarkTheme()
    val colorFilter = if (isDark && invertInDarkMode) DarkPaperInvertFilter else null

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (scale > 1.05f) "${String.format(Locale.US, "%.1f", scale)}x zoom" else "Double-tap or pinch zoom",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        scale = if (scale > 1.2f) 1f else 2.0f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        if (scale > 1.2f) Icons.Default.ZoomOutMap else Icons.Default.ZoomIn,
                        contentDescription = "Toggle zoom",
                        modifier = Modifier.size(17.dp)
                    )
                }
                if (bitmap != null) {
                    IconButton(
                        onClick = { showFullscreen = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen zoom",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight, max = maxHeight)
                .clip(RoundedCornerShape(10.dp))
                .clipToBounds()
                .background(if (isDark && invertInDarkMode) Color(0xFF141923) else Color.White)
                .pointerInput(bitmap) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.2f
                            }
                        }
                    )
                }
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4.2f)
                        scale = newScale
                        if (newScale > 1.02f) {
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-650f, 650f),
                                y = (offset.y + pan.y).coerceIn(-360f, 360f)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Passage card",
                    contentScale = ContentScale.FillWidth,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            } else {
                Text(
                    "Image unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showFullscreen && bitmap != null) {
        FullscreenZoomDialog(
            bitmap = bitmap,
            colorFilter = colorFilter,
            isDarkBg = isDark,
            title = fullscreenTitle,
            onDismiss = { showFullscreen = false }
        )
    }
}

@Composable
fun PassageImageCard(
    bitmap: Bitmap?,
    invertInDarkMode: Boolean,
    darkModeOverride: Boolean?,
    hasExtractedText: Boolean,
    isOcrRunning: Boolean,
    onRunOcr: () -> Unit,
    onOpenEditor: () -> Unit,
    onToggleTextView: (() -> Unit)? = null,
    showingTextView: Boolean = false,
    modifier: Modifier = Modifier
) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var showFullscreen by remember { mutableStateOf(false) }
    val isDark = darkModeOverride ?: isSystemInDarkTheme()
    val colorFilter = if (isDark && invertInDarkMode) DarkPaperInvertFilter else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onToggleTextView != null && hasExtractedText) {
                        FilledTonalButton(
                            onClick = onToggleTextView,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (showingTextView) "Book Pic" else "Text View",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else if (!hasExtractedText) {
                        TextButton(
                            onClick = onRunOcr,
                            enabled = !isOcrRunning,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            if (isOcrRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Scanning...", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Icon(
                                    Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Scan Text", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    IconButton(
                        onClick = onOpenEditor,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit text",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!showingTextView) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                scale = if (scale > 1.2f) 1f else 2.0f
                                offset = Offset.Zero
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                if (scale > 1.2f) Icons.Default.ZoomOutMap else Icons.Default.ZoomIn,
                                contentDescription = "Toggle zoom",
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        if (bitmap != null) {
                            IconButton(
                                onClick = { showFullscreen = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!showingTextView) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 135.dp, max = 270.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clipToBounds()
                        .background(if (isDark && invertInDarkMode) Color(0xFF141923) else Color.White)
                        .pointerInput(bitmap) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.1f
                                    }
                                }
                            )
                        }
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 4.0f)
                                scale = newScale
                                if (newScale > 1.02f) {
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-600f, 600f),
                                        y = (offset.y + pan.y).coerceIn(-340f, 340f)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Passage card",
                            contentScale = ContentScale.FillWidth,
                            colorFilter = colorFilter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        )
                    } else {
                        Text(
                            "Passage image unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showFullscreen && bitmap != null) {
        FullscreenZoomDialog(
            bitmap = bitmap,
            colorFilter = colorFilter,
            isDarkBg = isDark,
            title = "Passage Fullscreen",
            onDismiss = { showFullscreen = false }
        )
    }
}

@Composable
fun InteractiveInlinePassage(
    passageText: String,
    topic: GrammarTopic,
    userAnswers: Map<String, String>,
    expectedAnswers: Map<String, String>,
    selectedBlankKey: String,
    showFeedback: Boolean,
    strictCapitalization: Boolean,
    onSelectBlank: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val blankRegex = remember { Regex("\\(([a-jA-J])\\)\\s*(?:___+|—+|–+|-+|\\[\\s*\\])?") }
    val inlineMap = remember(passageText, topic, userAnswers, selectedBlankKey, showFeedback, strictCapitalization) {
        val map = mutableMapOf<String, InlineTextContent>()
        BLANK_KEYS.forEach { key ->
            val id = "blank_$key"
            val ans = userAnswers[key]
            val expected = expectedAnswers[key] ?: ""
            val isCorrect = showFeedback && AnswerEvaluator.isCorrect(ans, expected, strictCapitalization, topic)
            val isWrong = showFeedback && !ans.isNullOrBlank() && !isCorrect
            val isSelected = selectedBlankKey == key

            map[id] = InlineTextContent(
                Placeholder(
                    width = if (topic == GrammarTopic.PREPOSITION) 76.sp else 60.sp,
                    height = 24.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                val bg = when {
                    isCorrect -> EmeraldContainer
                    isWrong -> RoseContainer
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val borderCol = when {
                    isCorrect -> EmeraldSuccess
                    isWrong -> RoseError
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }
                val fg = when {
                    isCorrect -> Color(0xFF065F46)
                    isWrong -> Color(0xFF991B1B)
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .border(if (isSelected) 1.5.dp else 1.dp, borderCol, RoundedCornerShape(6.dp))
                        .clickable { onSelectBlank(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (ans != null) "($key) $ans" else "($key) __",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        map
    }

    val annotated = remember(passageText) {
        buildAnnotatedString {
            var lastIdx = 0
            val seen = mutableSetOf<String>()
            blankRegex.findAll(passageText).forEach { match ->
                val letter = match.groupValues[1].lowercase()
                if (letter in BLANK_KEYS && letter !in seen) {
                    seen.add(letter)
                    append(passageText.substring(lastIdx, match.range.first))
                    appendInlineContent("blank_$letter", "($letter)")
                    lastIdx = match.range.last + 1
                }
            }
            if (lastIdx < passageText.length) {
                append(passageText.substring(lastIdx))
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Text(
            text = annotated,
            inlineContent = inlineMap,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 27.sp),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlankAnswerPad(
    topic: GrammarTopic,
    exerciseKey: String,
    userAnswers: Map<String, String>,
    expectedAnswers: Map<String, String>,
    selectedBlankKey: String,
    showFeedback: Boolean,
    strictCapitalization: Boolean,
    isPractice: Boolean,
    showWordBankHint: Boolean,
    isSelectedBlankInFlashcards: Boolean,
    onSelectBlank: (String) -> Unit,
    onPickAnswer: (String?, Boolean) -> Unit,
    onAdvanceBlank: () -> Unit,
    onToggleWordBank: () -> Unit,
    onPinSelectedToFlashcards: () -> Unit
) {
    var capitalizeNext by remember(selectedBlankKey, exerciseKey) { mutableStateOf(false) }
    var expandAllPreps by remember(exerciseKey) { mutableStateOf(false) }
    var prepDraft by remember(selectedBlankKey, exerciseKey, userAnswers[selectedBlankKey]) {
        mutableStateOf(userAnswers[selectedBlankKey].orEmpty())
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 10 Blank slots (a)-(j) in two compact rows of 5 for small phones
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(BLANK_KEYS.take(5), BLANK_KEYS.drop(5)).forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    rowKeys.forEach { key ->
                        val ans = userAnswers[key]
                        val expected = expectedAnswers[key] ?: ""
                        val isSelected = selectedBlankKey == key
                        val isCorrect = showFeedback && AnswerEvaluator.isCorrect(ans, expected, strictCapitalization, topic)
                        val isWrong = showFeedback && !isCorrect && ans != null
                        val isMissed = showFeedback && ans == null

                        val containerColor = when {
                            isCorrect -> EmeraldContainer
                            isWrong || isMissed -> RoseContainer
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            ans != null -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val borderColor = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isCorrect -> EmeraldSuccess
                            isWrong || isMissed -> RoseError
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        }
                        val textColor = when {
                            isCorrect -> Color(0xFF065F46)
                            isWrong || isMissed -> Color(0xFF991B1B)
                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { onSelectBlank(key) },
                            shape = RoundedCornerShape(9.dp),
                            color = containerColor,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "($key)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.78f)
                                    )
                                    if (isCorrect) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(10.dp))
                                    } else if (isWrong) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = RoseError, modifier = Modifier.size(10.dp))
                                    }
                                }
                                Text(
                                    text = ans ?: "—",
                                    fontSize = if (topic == GrammarTopic.PREPOSITION) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Feedback line for currently selected blank if checked
        if (showFeedback) {
            val userAns = userAnswers[selectedBlankKey]
            val expected = expectedAnswers[selectedBlankKey] ?: ""
            val ok = AnswerEvaluator.isCorrect(userAns, expected, strictCapitalization, topic)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (ok) EmeraldContainer else RoseContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (ok) {
                            "($selectedBlankKey) Correct! Key: $expected"
                        } else {
                            "($selectedBlankKey) Yours: ${userAns ?: "Blank"}  |  Key: $expected"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (ok) Color(0xFF065F46) else Color(0xFF991B1B),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onPinSelectedToFlashcards,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isSelectedBlankInFlashcards) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Save to Flash Memory",
                            tint = if (isSelectedBlankInFlashcards) AmberAccent else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (topic == GrammarTopic.ARTICLE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val choices = if (capitalizeNext) listOf("A", "An", "The", "×") else listOf("a", "an", "the", "×")
                choices.forEach { token ->
                    val isCurrentlyPicked = userAnswers[selectedBlankKey] == token
                    FilledTonalButton(
                        onClick = { onPickAnswer(token, true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isCurrentlyPicked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            },
                            contentColor = if (isCurrentlyPicked) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    ) {
                        Text(
                            text = token,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { capitalizeNext = !capitalizeNext },
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (capitalizeNext) "Aa: A/An/The" else "aa: a/an/the",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (isPractice) {
                    OutlinedButton(
                        onClick = onPinSelectedToFlashcards,
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            if (isSelectedBlankInFlashcards) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isSelectedBlankInFlashcards) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Flash", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedButton(
                    onClick = { onPickAnswer(null, false) },
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            // ==================== PREPOSITION COMPACT INPUT PAD ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = prepDraft,
                    onValueChange = { typed ->
                        prepDraft = typed
                        onPickAnswer(typed.ifBlank { null }, false)
                    },
                    label = { Text("Blank ($selectedBlankKey)", maxLines = 1) },
                    placeholder = { Text("e.g. of, into, in front of", maxLines = 1) },
                    singleLine = true,
                    shape = RoundedCornerShape(11.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (capitalizeNext) KeyboardCapitalization.Words else KeyboardCapitalization.None,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (prepDraft.isNotBlank()) onPickAnswer(prepDraft, true) else onAdvanceBlank()
                        }
                    ),
                    trailingIcon = {
                        if (prepDraft.isNotEmpty()) {
                            IconButton(onClick = {
                                prepDraft = ""
                                onPickAnswer(null, false)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Clear", modifier = Modifier.size(17.dp))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                FilledTonalButton(
                    onClick = onAdvanceBlank,
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.height(52.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Next", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            // FlowRow wraps gracefully on 360dp Samsung phones!
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = capitalizeNext,
                    onClick = { capitalizeNext = !capitalizeNext },
                    label = { Text(if (capitalizeNext) "Aa: Cap" else "aa: lower", style = MaterialTheme.typography.labelSmall) }
                )

                if (isPractice) {
                    FilterChip(
                        selected = showWordBankHint,
                        onClick = onToggleWordBank,
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Word Bank", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = isSelectedBlankInFlashcards,
                        onClick = onPinSelectedToFlashcards,
                        leadingIcon = {
                            Icon(
                                if (isSelectedBlankInFlashcards) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isSelectedBlankInFlashcards) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text("Flash", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                FilterChip(
                    selected = expandAllPreps,
                    onClick = { expandAllPreps = !expandAllPreps },
                    trailingIcon = {
                        Icon(
                            if (expandAllPreps) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = { Text(if (expandAllPreps) "Less" else "All Chips", style = MaterialTheme.typography.labelSmall) }
                )
            }

            if (isPractice && showWordBankHint) {
                val wordBank = remember(exerciseKey, expectedAnswers) {
                    val primary = expectedAnswers.values
                        .mapNotNull { it.split("/").firstOrNull()?.trim()?.lowercase(Locale.US) }
                        .distinct()
                    val distractors = listOf("with", "from", "about", "through", "against", "upon")
                        .filter { it !in primary }
                        .take(2)
                    (primary + distractors).shuffled()
                }
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Passage Word Bank",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            wordBank.forEach { rawWord ->
                                val token = if (capitalizeNext) rawWord.replaceFirstChar { it.uppercase(Locale.US) } else rawWord
                                SuggestionChip(
                                    onClick = {
                                        prepDraft = token
                                        onPickAnswer(token, true)
                                    },
                                    label = { Text(token, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }
                    }
                }
            }

            if (expandAllPreps) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    COMMON_PREPOSITIONS.forEach { rawWord ->
                        val token = if (capitalizeNext) rawWord.replaceFirstChar { it.uppercase(Locale.US) } else rawWord
                        val picked = userAnswers[selectedBlankKey]?.equals(token, ignoreCase = !strictCapitalization) == true
                        FilterChip(
                            selected = picked,
                            onClick = {
                                prepDraft = token
                                onPickAnswer(token, true)
                            },
                            label = { Text(token, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(COMMON_PREPOSITIONS.take(22)) { rawWord ->
                        val token = if (capitalizeNext) rawWord.replaceFirstChar { it.uppercase(Locale.US) } else rawWord
                        val picked = userAnswers[selectedBlankKey]?.equals(token, ignoreCase = !strictCapitalization) == true
                        FilterChip(
                            selected = picked,
                            onClick = {
                                prepDraft = token
                                onPickAnswer(token, true)
                            },
                            label = { Text(token, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}
