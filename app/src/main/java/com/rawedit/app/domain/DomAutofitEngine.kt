package com.rawedit.app.domain

import android.webkit.WebView
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

@Immutable
@Serializable
data class AutofitViolation(
    val selector: String,
    val kind: String, // "OVERFLOW" | "LOWCONTRAST" | "MISSINGIMG" | "BLANK"
    val detail: String
)

@Immutable
@Serializable
data class OverflowEdges(
    val l: Int = 0,
    val t: Int = 0,
    val r: Int = 0,
    val b: Int = 0
)

@Immutable
@Serializable
data class AutofitReport(
    val fits: Boolean = true,
    val scaleApplied: Float = 1.0f,
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val overflowPx: OverflowEdges = OverflowEdges(),
    val textNodesChecked: Int = 0,
    val isBlankFrame: Boolean = false,
    val domSnapshotSummary: String = "",
    val violations: List<AutofitViolation> = emptyList()
)

object DomAutofitEngine {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Injected once per WebView page load. Wraps body children inside #raw-edit-autofit-wrapper
     * without mutating the user's own CSS or inline styles, measures all visible text/SVG/image nodes
     * against the 1920x1080 canvas and 96px safe margin, and applies a corrective scale+translate
     * transform when needed.
     */
    val INJECT_SCRIPT: String = """
        (function() {
          if (window.__rawAutofitInstalled) return true;
          window.__rawAutofitInstalled = true;

          function ensureWrapper() {
            var wrap = document.getElementById('raw-edit-autofit-wrapper');
            if (wrap) return wrap;
            wrap = document.createElement('div');
            wrap.id = 'raw-edit-autofit-wrapper';
            wrap.style.cssText = 'position:relative;width:1920px;height:1080px;transform-origin:0 0;will-change:transform;';
            var body = document.body;
            if (!body) return null;
            while (body.firstChild) {
              wrap.appendChild(body.firstChild);
            }
            body.appendChild(wrap);
            return wrap;
          }

          function parseRgb(str) {
            if (!str) return null;
            var m = str.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/i);
            if (!m) return null;
            return [Number(m[1]), Number(m[2]), Number(m[3])];
          }

          function relLum(rgb) {
            var s = rgb.map(function(v) {
              v /= 255;
              return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
            });
            return 0.2126 * s[0] + 0.7152 * s[1] + 0.0722 * s[2];
          }

          window.__rawMeasureAndAutofit = function(applyCorrection) {
            var wrap = ensureWrapper();
            if (!wrap) {
              return JSON.stringify({
                fits: false, scaleApplied: 1.0, translateX: 0, translateY: 0,
                overflowPx: {l:0,t:0,r:0,b:0}, textNodesChecked: 0, isBlankFrame: true,
                domSnapshotSummary: 'No body', violations: [{selector:'body',kind:'BLANK',detail:'Missing body element'}]
              });
            }

            // Reset wrapper transform before measuring natural bounding boxes
            wrap.style.transform = 'none';

            var SAFE_L = 96, SAFE_T = 96, SAFE_R = 1824, SAFE_B = 984;
            var SAFE_W = SAFE_R - SAFE_L;
            var SAFE_H = SAFE_B - SAFE_T;

            var minX = 1920, minY = 1080, maxX = 0, maxY = 0;
            var hasVisibleContent = false;
            var textNodesCount = 0;
            var violations = [];

            var elements = wrap.querySelectorAll('*');
            for (var i = 0; i < elements.length; i++) {
              var el = elements[i];
              var style = window.getComputedStyle(el);
              if (style.display === 'none' || style.visibility === 'hidden' || parseFloat(style.opacity || '1') < 0.04) {
                continue;
              }
              if (el.tagName === 'IMG') {
                if (!el.complete || el.naturalWidth === 0) {
                  violations.push({
                    selector: el.tagName + (el.id ? '#' + el.id : ''),
                    kind: 'MISSINGIMG',
                    detail: 'Broken or unloaded <img> asset'
                  });
                }
              }

              var directText = '';
              for (var c = 0; c < el.childNodes.length; c++) {
                if (el.childNodes[c].nodeType === 3) {
                  directText += el.childNodes[c].nodeValue;
                }
              }
              directText = directText.trim();
              var isGraphic = (el.tagName === 'SVG' || el.tagName === 'CANVAS' || el.tagName === 'IMG');

              if (directText.length > 0 || isGraphic) {
                var rect = el.getBoundingClientRect();
                if (rect.width > 1 && rect.height > 1) {
                  hasVisibleContent = true;
                  if (directText.length > 0) textNodesCount++;
                  if (rect.left < minX) minX = rect.left;
                  if (rect.top < minY) minY = rect.top;
                  if (rect.right > maxX) maxX = rect.right;
                  if (rect.bottom > maxY) maxY = rect.bottom;

                  var ident = el.tagName.toLowerCase() + (el.id ? '#' + el.id : (el.className && typeof el.className === 'string' ? '.' + el.className.split(' ')[0] : ''));
                  if (rect.left < SAFE_L - 2 || rect.top < SAFE_T - 2 || rect.right > SAFE_R + 2 || rect.bottom > SAFE_B + 2) {
                    violations.push({
                      selector: ident,
                      kind: 'OVERFLOW',
                      detail: 'Bounds [' + Math.round(rect.left) + ',' + Math.round(rect.top) + ' -> ' + Math.round(rect.right) + ',' + Math.round(rect.bottom) + '] exceed safe area'
                    });
                  }

                  if (directText.length > 0) {
                    var fg = parseRgb(style.color);
                    var bg = parseRgb(style.backgroundColor);
                    if (fg && bg && (bg[0] > 0 || bg[1] > 0 || bg[2] > 0)) {
                      var l1 = relLum(fg), l2 = relLum(bg);
                      var ratio = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
                      if (ratio < 2.5) {
                        violations.push({
                          selector: ident,
                          kind: 'LOWCONTRAST',
                          detail: 'Contrast ratio ' + ratio.toFixed(2) + ':1 on text "' + directText.slice(0, 18) + '"'
                        });
                      }
                    }
                  }
                }
              }
            }

            if (!hasVisibleContent) {
              violations.push({
                selector: '#stage',
                kind: 'BLANK',
                detail: 'No visible text or vector elements rendered at this timestamp'
              });
              return JSON.stringify({
                fits: false, scaleApplied: 1.0, translateX: 0, translateY: 0,
                overflowPx: {l:0,t:0,r:0,b:0}, textNodesChecked: 0, isBlankFrame: true,
                domSnapshotSummary: 'Blank canvas', violations: violations
              });
            }

            var overL = Math.max(0, Math.round(SAFE_L - minX));
            var overT = Math.max(0, Math.round(SAFE_T - minY));
            var overR = Math.max(0, Math.round(maxX - SAFE_R));
            var overB = Math.max(0, Math.round(maxY - SAFE_B));
            var fitsNaturally = (overL === 0 && overT === 0 && overR === 0 && overB === 0);

            var scale = 1.0, tx = 0.0, ty = 0.0;
            if (!fitsNaturally && applyCorrection) {
              var contentW = Math.max(1, maxX - minX);
              var contentH = Math.max(1, maxY - minY);
              var scaleX = SAFE_W / contentW;
              var scaleY = SAFE_H / contentH;
              scale = Math.max(0.5, Math.min(1.0, Math.min(scaleX, scaleY)));
              var scaledW = contentW * scale;
              var scaledH = contentH * scale;
              var targetLeft = SAFE_L + (SAFE_W - scaledW) * 0.5;
              var targetTop = SAFE_T + (SAFE_H - scaledH) * 0.5;
              tx = targetLeft - minX * scale;
              ty = targetTop - minY * scale;
              wrap.style.transform = 'translate(' + tx.toFixed(1) + 'px, ' + ty.toFixed(1) + 'px) scale(' + scale.toFixed(4) + ')';
            }

            var snap = 'bbox=[' + Math.round(minX) + ',' + Math.round(minY) + ',' + Math.round(maxX) + ',' + Math.round(maxY) + '] nodes=' + textNodesCount + ' scale=' + scale.toFixed(2);
            return JSON.stringify({
              fits: fitsNaturally,
              scaleApplied: Number(scale.toFixed(3)),
              translateX: Number(tx.toFixed(1)),
              translateY: Number(ty.toFixed(1)),
              overflowPx: { l: overL, t: overT, r: overR, b: overB },
              textNodesChecked: textNodesCount,
              isBlankFrame: false,
              domSnapshotSummary: snap,
              violations: violations.slice(0, 12)
            });
          };
          return true;
        })();
    """.trimIndent()

    suspend fun seekAndMeasure(
        webView: WebView,
        timeSeconds: Double,
        applyAutofitTransform: Boolean = true
    ): AutofitReport = suspendCancellableCoroutine { cont ->
        val js = """
            (function() {
              try {
                ${INJECT_SCRIPT}
                if (window.RAW && typeof window.RAW.seek === 'function') {
                  window.RAW.seek(${String.format("%.4f", timeSeconds)});
                }
                return window.__rawMeasureAndAutofit(${if (applyAutofitTransform) "true" else "false"});
              } catch (e) {
                return JSON.stringify({
                  fits: false, scaleApplied: 1.0, translateX: 0, translateY: 0,
                  overflowPx: {l:0,t:0,r:0,b:0}, textNodesChecked: 0, isBlankFrame: false,
                  domSnapshotSummary: 'JS Error',
                  violations: [{selector: 'window.RAW.seek', kind: 'JSERR', detail: String(e && e.message || e)}]
                });
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { rawJson ->
            if (!cont.isActive) return@evaluateJavascript
            val cleaned = (rawJson ?: "")
                .trim()
                .removeSurrounding(""")
                .replace("\\"", """)
                .replace("\\\\", "\\")
            val report = runCatching {
                json.decodeFromString<AutofitReport>(cleaned)
            }.getOrElse {
                AutofitReport(fits = true)
            }
            cont.resume(report)
        }
    }
}
