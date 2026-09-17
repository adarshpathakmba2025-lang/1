package com.bits.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.bits.app.R
import kotlin.math.ceil

/**
 * Draws a category heading into a small bitmap using the pixel face.
 *
 * Widgets are rendered inside the launcher's process, which only reliably has the system
 * typefaces to hand. Pointing a widget TextView at an app font works on some launchers
 * and is quietly ignored on others. Drawing the text here and sending an image means the
 * face is already baked in by the time the launcher sees it, so it looks the same
 * everywhere.
 *
 * The cost is that the text can no longer reflow to the widget's real width, so the size
 * steps down until the name fits two lines, and anything still too long is clipped with
 * three dots.
 */
internal object PixelHeading {

    /** Roughly the usable width of a four-cell widget, minus the padding around the list. */
    private const val MAX_WIDTH_DP = 260f
    private const val MAX_LINES = 2
    private const val LINE_GAP_DP = 3f
    private val SIZES_SP = floatArrayOf(11f, 10f, 9f, 8f)
    private const val ELLIPSIS = "..."

    /** Headings repeat on every refresh and rarely change, so a handful are kept ready. */
    private val cache = LruCache<String, Bitmap>(24)

    fun render(context: Context, text: String, color: Int): Bitmap? {
        if (text.isBlank()) return null

        val metrics = context.resources.displayMetrics
        val key = "$text|$color|${metrics.densityDpi}|${metrics.scaledDensity}"
        cache.get(key)?.let { if (!it.isRecycled) return it }

        // A missing or unreadable font is not worth crashing a home screen over; the
        // caller falls back to the ordinary heading.
        val face = runCatching { ResourcesCompat.getFont(context, R.font.press_start_2p) }
            .getOrNull() ?: return null

        val maxWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_DP, metrics)
        // Set field by field rather than inside an apply block: inside one, the bare
        // names `typeface` and `color` would resolve to Paint's own properties rather
        // than to these values, quietly assigning each field to itself.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.typeface = face
        paint.color = color

        var lines = listOf(text)
        for (sizeSp in SIZES_SP) {
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
            lines = wrap(paint, text, maxWidth)
            if (lines.size <= MAX_LINES) break
        }
        lines = lines.take(MAX_LINES).map { clip(paint, it, maxWidth) }

        val fontMetrics = paint.fontMetrics
        val lineHeight = ceil(fontMetrics.descent - fontMetrics.ascent).toInt().coerceAtLeast(1)
        val gap = if (lines.size > 1) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, LINE_GAP_DP, metrics).toInt()
        } else {
            0
        }

        val width = ceil(lines.maxOf { paint.measureText(it) }).toInt().coerceAtLeast(1)
        val height = lineHeight * lines.size + gap * (lines.size - 1)

        val bitmap = runCatching {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }.getOrNull() ?: return null

        val canvas = Canvas(bitmap)
        var baseline = -fontMetrics.ascent
        lines.forEach { line ->
            canvas.drawText(line, 0f, baseline, paint)
            baseline += lineHeight + gap
        }

        cache.put(key, bitmap)
        return bitmap
    }

    /** Greedy word wrap. A single word wider than the line is left alone for [clip]. */
    private fun wrap(paint: Paint, text: String, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)

        val lines = mutableListOf<String>()
        var current = ""
        text.split(" ").filter { it.isNotEmpty() }.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (current.isEmpty() || paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return if (lines.isEmpty()) listOf(text) else lines
    }

    private fun clip(paint: Paint, line: String, maxWidth: Float): String {
        if (paint.measureText(line) <= maxWidth) return line
        val room = maxWidth - paint.measureText(ELLIPSIS)
        if (room <= 0f) return ELLIPSIS
        val kept = paint.breakText(line, true, room, null)
        return line.take(kept).trimEnd() + ELLIPSIS
    }
}
