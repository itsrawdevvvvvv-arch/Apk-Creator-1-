package com.rawedit.app.domain

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume

@Immutable
@Serializable
data class ValidationIssue(
    val rule: String,
    val detail: String,
    val timeMs: Long? = null,
    val isFatal: Boolean = true
)

@Immutable
data class ValidationReport(
    val passed: Boolean,
    val sanitizedHtml: String,
    val bridgedProjectContract: Boolean,
    val detectedDurationSec: Double,
    val issues: List<ValidationIssue>,
    val consoleLogs: List<String>
)

object CodeValidator {

    private val EXTERNAL_URL_REGEX = Regex(
        "https?://(?!(?:raw\\.local|assets\\.local)\\b)[a-zA-Z0-9.-]+",
        RegexOption.IGNORE_CASE
    )
    private val FETCH_REGEX = Regex("\\b(?:window\\.)?fetch\\s*\\(")
    private val XHR_REGEX = Regex("\\bXMLHttpRequest\\b")
    private val WEBSOCKET_REGEX = Regex("\\bWebSocket\\b")
    private val SENDBEACON_REGEX = Regex("navigator\\.sendBeacon\\b")
    private val IMPORTSCRIPTS_REGEX = Regex("\\bimportScripts\\s*\\(")
    private val EVAL_REMOTE_REGEX = Regex("\\beval\\s*\\(\\s*['\"`]https?://", RegexOption.IGNORE_CASE)

    /**
     * Strips markdown code fences, conversational preamble/postamble around <!DOCTYPE html>...</html>,
     * and auto-bridges window.PROJECT + window.seek(t) into window.RAW when needed.
     */
    fun sanitizeAndBridgeHtml(rawInput: String, fallbackDurationSec: Double = 6.0): Pair<String, Boolean> {
        var text = rawInput.trim()

        // Strip markdown fences if present
        if (text.contains("```")) {
            val fenceMatch = Regex("```(?:html|HTML)?\\s*([\\s\\S]*?)```").find(text)
            if (fenceMatch != null) {
                text = fenceMatch.groupValues[1].trim()
            }
        }

        // Extract from <!DOCTYPE html> or <html to </html>
        val lower = text.lowercase()
        val startIdx = lower.indexOf("<!doctype html").takeIf { it >= 0 }
            ?: lower.indexOf("<html").takeIf { it >= 0 }
            ?: 0
        val endIdx = lower.lastIndexOf("</html>")
        if (endIdx > startIdx) {
            text = text.substring(startIdx, endIdx + 7)
        }

        val hasWindowRaw = text.contains("window.RAW") || text.contains("RAW.seek")
        val hasProjectShape = (text.contains("window.PROJECT") || text.contains("window.seek")) && !hasWindowRaw

        if (hasProjectShape) {
            val bridgeScript = """
                <script id="raw-edit-auto-bridge">
                (function() {
                  if (!window.RAW) {
                    var proj = window.PROJECT || {};
                    var dur = Number(proj.duration || proj.totalDuration || $fallbackDurationSec) || $fallbackDurationSec;
                    window.RAW = {
                      duration: dur,
                      fps: Number(proj.fps || 30) || 30,
                      width: 1920,
                      height: 1080,
                      seek: function(t) {
                        if (typeof window.seek === 'function') {
                          window.seek(t);
                        } else if (proj && typeof proj.seek === 'function') {
                          proj.seek(t);
                        }
                      }
                    };
                  }
                })();
                </script>
            """.trimIndent()
            val closeBodyIdx = text.lowercase().lastIndexOf("</body>")
            text = if (closeBodyIdx >= 0) {
                text.substring(0, closeBodyIdx) + "
" + bridgeScript + "
" + text.substring(closeBodyIdx)
            } else {
                text + "
" + bridgeScript
            }
        }

        return text to hasProjectShape
    }

    fun runStaticChecks(html: String): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (html.isBlank() || !html.lowercase().contains("<html")) {
            issues.add(ValidationIssue("HTML_STRUCTURE", "Document is missing <html> root structure."))
        }
        EXTERNAL_URL_REGEX.find(html)?.let { m ->
            issues.add(ValidationIssue("NO_EXTERNAL_URLS", "Forbidden external URL detected: ${m.value}"))
        }
        if (FETCH_REGEX.containsMatchIn(html)) {
            issues.add(ValidationIssue("NO_FETCH", "Forbidden network API `fetch(...)` detected."))
        }
        if (XHR_REGEX.containsMatchIn(html)) {
            issues.add(ValidationIssue("NO_XHR", "Forbidden `XMLHttpRequest` usage detected."))
        }
        if (WEBSOCKET_REGEX.containsMatchIn(html)) {
            issues.add(ValidationIssue("NO_WEBSOCKET", "Forbidden `WebSocket` usage detected."))
        }
        if (SENDBEACON_REGEX.containsMatchIn(html)) {
            issues.add(ValidationIssue("NO_SENDBEACON", "Forbidden `navigator.sendBeacon` detected."))
        }
        if (IMPORTSCRIPTS_REGEX.containsMatchIn(html)) {
            issues.add(ValidationIssue("NO_IMPORTSCRIPTS", "Forbidden `importScripts` detected."))
        }
        if (EVAL_REMOTE_REGEX.containsMatchIn(html)) {
            issues.add(ValidationIssue("NO_REMOTE_EVAL", "Forbidden remote string `eval(...)` detected."))
        }
        if (!html.contains("window.RAW") && !html.contains("RAW")) {
            issues.add(ValidationIssue("RAW_CONTRACT", "Missing `window.RAW` object with `duration` and `seek(t)`."))
        }
        return issues
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun validateFull(
        context: Context,
        rawInput: String,
        fallbackDurationSec: Double = 6.0
    ): ValidationReport = withContext(Dispatchers.Main) {
        val (sanitized, bridged) = sanitizeAndBridgeHtml(rawInput, fallbackDurationSec)
        val issues = runStaticChecks(sanitized).toMutableList()
        val consoleLogs = mutableListOf<String>()

        val assetServer = LocalAssetServer(context.applicationContext, sanitized)
        val webView = WebView(context.applicationContext)
        var detectedDuration = fallbackDurationSec

        try {
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = false
                databaseEnabled = false
                allowFileAccess = false
                allowContentAccess = false
                blockNetworkLoads = true
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            webView.layout(0, 0, 1920, 1080)

            val startWallMs = System.currentTimeMillis()
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    if (consoleMessage != null) {
                        val elapsed = System.currentTimeMillis() - startWallMs
                        val line = "[+${elapsed}ms] ${consoleMessage.messageLevel()}: ${consoleMessage.message()} (L${consoleMessage.lineNumber()})"
                        consoleLogs.add(line)
                        if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                            issues.add(
                                ValidationIssue(
                                    rule = "JS_CONSOLE_ERROR",
                                    detail = "${consoleMessage.message()} (line ${consoleMessage.lineNumber()})",
                                    timeMs = elapsed,
                                    isFatal = true
                                )
                            )
                        }
                    }
                    return true
                }
            }

            // Up to 20s dry-run budget
            val dryRunResult = withTimeoutOrNull(20_000L) {
                suspendCancellableCoroutine<String> { cont ->
                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (request == null) return null
                            val resp = assetServer.intercept(request)
                            if (resp.statusCode == 403) {
                                issues.add(
                                    ValidationIssue(
                                        rule = "NETWORK_BLOCKED_403",
                                        detail = "Blocked request to ${request.url}",
                                        timeMs = System.currentTimeMillis() - startWallMs,
                                        isFatal = true
                                    )
                                )
                            }
                            return resp
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            val probeJs = """
                                (function() {
                                  window.__rawUnhandled = [];
                                  window.addEventListener('unhandledrejection', function(e) {
                                    window.__rawUnhandled.push(String(e.reason || 'Promise rejection'));
                                  });
                                  try {
                                    var raw = window.RAW;
                                    if (!raw || typeof raw.seek !== 'function') {
                                      return JSON.stringify({ ok: false, reason: 'window.RAW.seek(t) is missing' });
                                    }
                                    var dur = Number(raw.duration) || 6.0;
                                    var samples = [0, dur * 0.25, dur * 0.5, dur * 0.75, Math.max(0, dur - 0.05)];
                                    var nonBlankCount = 0;
                                    for (var i = 0; i < samples.length; i++) {
                                      raw.seek(samples[i]);
                                      var textLen = (document.body && document.body.innerText) ? document.body.innerText.trim().length : 0;
                                      var svgCount = document.querySelectorAll('svg, canvas, img, div').length;
                                      if (textLen > 0 || svgCount > 1) nonBlankCount++;
                                    }
                                    return JSON.stringify({
                                      ok: true,
                                      duration: dur,
                                      nonBlankCount: nonBlankCount,
                                      unhandled: window.__rawUnhandled
                                    });
                                  } catch (err) {
                                    return JSON.stringify({ ok: false, reason: String(err && err.stack || err) });
                                  }
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(probeJs) { resultJson ->
                                if (cont.isActive) {
                                    cont.resume(resultJson ?: "")
                                }
                            }
                        }
                    }
                    webView.loadUrl(LocalAssetServer.BASE_URL)
                }
            }

            if (dryRunResult == null) {
                issues.add(
                    ValidationIssue(
                        rule = "DRY_RUN_TIMEOUT",
                        detail = "20s dry-run timed out waiting for composition initialization.",
                        isFatal = true
                    )
                )
            } else {
                val unescaped = dryRunResult
                    .trim()
                    .removeSurrounding(""")
                    .replace("\\"", """)
                    .replace("\\\\", "\\")
                if (unescaped.contains(""ok":false")) {
                    issues.add(
                        ValidationIssue(
                            rule = "RAW_RUNTIME_ERROR",
                            detail = unescaped,
                            isFatal = true
                        )
                    )
                } else {
                    val durMatch = Regex(""duration":([0-9.]+)").find(unescaped)
                    if (durMatch != null) {
                        detectedDuration = durMatch.groupValues[1].toDoubleOrNull() ?: fallbackDurationSec
                    }
                    if (unescaped.contains(""nonBlankCount":0")) {
                        issues.add(
                            ValidationIssue(
                                rule = "BLANK_CANVAS",
                                detail = "Canvas remained blank across all sampled dry-run timestamps.",
                                isFatal = true
                            )
                        )
                    }
                }
            }
        } finally {
            runCatching {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }

        val fatalCount = issues.count { it.isFatal }
        ValidationReport(
            passed = fatalCount == 0,
            sanitizedHtml = sanitized,
            bridgedProjectContract = bridged,
            detectedDurationSec = detectedDuration,
            issues = issues,
            consoleLogs = consoleLogs
        )
    }
}
