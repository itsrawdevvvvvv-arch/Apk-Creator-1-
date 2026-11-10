package com.rawedit.app

import com.rawedit.app.domain.CodeValidator
import com.rawedit.app.domain.SrtParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtAndValidatorTest {

    @Test
    fun srtParser_toleratesBomMissingIndicesAndHtmlTags() {
        val raw = "\uFEFF00:00:00,000 --> 00:00:01,500\r\n<b>Hello</b> world\r\n\r\n2\n00:00:01.200 --> 00:00:03.000\nSecond line"
        val res = SrtParser.parse(raw)
        assertEquals(2, res.cues.size)
        assertEquals("Hello world", res.cues[0].text)
        assertEquals(1500L, res.cues[0].endMs)
        assertTrue(res.warnings.isNotEmpty())
    }

    @Test
    fun codeValidator_bridgesProjectShapeAndCatchesExternalUrls() {
        val projectHtml = """
            ```html
            <!DOCTYPE html><html><body>
            <script>
              window.PROJECT = { duration: 8 };
              window.seek = function(t) {};
            </script>
            </body></html>
            ```
        """.trimIndent()

        val (sanitized, bridged) = CodeValidator.sanitizeAndBridgeHtml(projectHtml, 6.0)
        assertTrue(bridged)
        assertTrue(sanitized.contains("window.RAW"))
        val issues = CodeValidator.runStaticChecks(sanitized)
        assertTrue(issues.isEmpty())

        val badHtml = "<!DOCTYPE html><html><body><script src='https://cdn.example.com/app.js'></script></body></html>"
        val badIssues = CodeValidator.runStaticChecks(badHtml)
        assertFalse(badIssues.isEmpty())
    }
}
