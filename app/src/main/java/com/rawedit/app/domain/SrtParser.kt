package com.rawedit.app.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SrtCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

@Immutable
data class SrtParseResult(
    val cues: List<SrtCue>,
    val warnings: List<String>,
    val promptSnippet: String
)

object SrtParser {
    private const val MAX_CUES = 20_000
    private const val MAX_SNIPPET_BYTES = 60 * 1024 // <= 60 KB

    private val TIMECODE_REGEX = Regex(
        "(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{1,3})\\s*-->\\s*(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{1,3})"
    )
    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val CONTROL_CHAR_REGEX = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")

    fun parse(rawInput: String): SrtParseResult {
        if (rawInput.isBlank()) {
            return SrtParseResult(emptyList(), emptyList(), "")
        }
        val warnings = mutableListOf<String>()

        val normalized = rawInput
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val lines = normalized.lines()
        val parsedCues = ArrayList<SrtCue>(minOf(lines.size / 3 + 1, 2048))

        var i = 0
        var autoIndex = 1
        while (i < lines.size) {
            if (parsedCues.size >= MAX_CUES) {
                warnings.add("Capped at $MAX_CUES cues limit.")
                break
            }
            val line = lines[i].trim()
            if (line.isEmpty()) {
                i++
                continue
            }

            var timeLine = line
            var cueIdx = autoIndex
            val maybeIdx = line.toIntOrNull()
            if (maybeIdx != null && i + 1 < lines.size && TIMECODE_REGEX.containsMatchIn(lines[i + 1])) {
                cueIdx = maybeIdx
                i++
                timeLine = lines[i].trim()
            }

            val match = TIMECODE_REGEX.find(timeLine)
            if (match == null) {
                i++
                continue
            }

            val startMs = toMillis(
                match.groupValues[1],
                match.groupValues[2],
                match.groupValues[3],
                match.groupValues[4]
            )
            var endMs = toMillis(
                match.groupValues[5],
                match.groupValues[6],
                match.groupValues[7],
                match.groupValues[8]
            )
            if (endMs <= startMs) {
                endMs = startMs + 500L
                warnings.add("Cue #$cueIdx had non-positive duration; adjusted to +500ms.")
            }

            i++
            val textBuilder = StringBuilder()
            while (i < lines.size && lines[i].trim().isNotEmpty()) {
                if (TIMECODE_REGEX.containsMatchIn(lines[i])) break
                if (textBuilder.isNotEmpty()) textBuilder.append(' ')
                textBuilder.append(lines[i].trim())
                i++
            }

            val cleanText = textBuilder.toString()
                .replace(HTML_TAG_REGEX, "")
                .replace(CONTROL_CHAR_REGEX, "")
                .trim()

            if (cleanText.isNotEmpty()) {
                if (parsedCues.isNotEmpty() && startMs < parsedCues.last().endMs) {
                    warnings.add("Overlapping cue at ${formatTimecode(startMs)} normalized.")
                }
                parsedCues.add(
                    SrtCue(
                        index = autoIndex++,
                        startMs = startMs,
                        endMs = endMs,
                        text = cleanText
                    )
                )
            }
        }

        val sorted = parsedCues.sortedBy { it.startMs }
        val snippet = buildSnippet(sorted)
        return SrtParseResult(
            cues = sorted,
            warnings = warnings,
            promptSnippet = snippet
        )
    }

    private fun toMillis(h: String, m: String, s: String, ms: String): Long {
        val hours = h.toLongOrNull() ?: 0L
        val mins = m.toLongOrNull() ?: 0L
        val secs = s.toLongOrNull() ?: 0L
        val millisPadded = ms.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return (((hours * 60L) + mins) * 60L + secs) * 1000L + millisPadded
    }

    fun formatTimecode(timeMs: Long): String {
        val clamped = timeMs.coerceAtLeast(0L)
        val totalSec = clamped / 1000L
        val ms = clamped % 1000L
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        return String.format("%02d:%02d:%02d.%03d", h, m, s, ms)
    }

    private fun buildSnippet(cues: List<SrtCue>): String {
        if (cues.isEmpty()) return ""
        val sb = StringBuilder()
        var currentBytes = 0
        for (cue in cues) {
            val startSec = String.format("%.2f", cue.startMs / 1000.0)
            val endSec = String.format("%.2f", cue.endMs / 1000.0)
            val line = "Cue #${String.format("%03d", cue.index)} | ${startSec}s -> ${endSec}s [${formatTimecode(cue.startMs)} -> ${formatTimecode(cue.endMs)}] | \"${cue.text}\"\n"
            val lineBytes = line.toByteArray(Charsets.UTF_8).size
            if (currentBytes + lineBytes > MAX_SNIPPET_BYTES) {
                sb.append("... (truncated to 60 KB prompt safety cap)\n")
                break
            }
            sb.append(line)
            currentBytes += lineBytes
        }
        return sb.toString()
    }
}
