package com.rawedit.app.domain

import com.rawedit.app.data.FrameFlag
import com.rawedit.app.data.ProjectEntity

object PromptGenerator {

    fun buildAuthoringPrompt(
        project: ProjectEntity,
        srtSnippet: String,
        resolvedScript: String
    ): String {
        val durationSec = (project.audioDurationMs.coerceAtLeast(1000L) / 1000.0)
        val totalFrames = (durationSec * 30.0).toInt()
        val scriptPreview = if (resolvedScript.length > 24_000) {
            resolvedScript.take(24_000) + "\n...[truncated at 24k chars]"
        } else {
            resolvedScript
        }

        return buildString {
            appendLine("================================================================================")
            appendLine("CRITICAL CREDIT & TOKEN BUDGET GUARDRAIL FOR CLAUDE / CLOUD AI (READ FIRST!)")
            appendLine("================================================================================")
            appendLine("- DO NOT waste tokens or burn through the user's Claude message/usage credits!")
            appendLine("- DO NOT run bash commands, Playwright renders, Python scripts, or multi-step tool loops.")
            appendLine("- DO NOT install packages (`pip`, `npm`, `playwright`, `ffmpeg`) or generate intermediate files.")
            appendLine("- DO NOT ask follow-up questions or pause for a plan approval table — build immediately in ONE turn.")
            appendLine("- DO NOT write any conversational intro, explanation, or postamble. Output ONLY the final single-file `<!DOCTYPE html>...</html>` code.")
            appendLine("- The Android app `raw_edit` will render, validate, DOM-autofit, and encode the MP4 locally on the user's phone.")
            appendLine()
            appendLine("================================================================================")
            appendLine("1. LEARN THE STYLE FROM THE ATTACHED `kinetic-explainer-style.zip`")
            appendLine("================================================================================")
            appendLine("The user has attached `kinetic-explainer-style.zip` (exported from `raw_edit`). Before writing code, inspect its bundled files:")
            appendLine("  1. `kinetic-explainer-style/SKILL.md` — Core workflow & element types (`ghost`, `kicker`, `headline`, `sub`, `pill`, `chips`, `card`, `row`, `phone`, `buttons`, `comment`, `round`).")
            appendLine("  2. `kinetic-explainer-style/docs/STYLE_BIBLE.md` — Section 0 (Prime Directive), exact hex palette, Poppins 900 typography scale, 1920x1080 layout coordinates, and visual QA rules.")
            appendLine("  3. `kinetic-explainer-style/docs/BEAT_SHEETS.md` — Exact pacing and scene breakdowns of Reference Video 1 ('ElevenLabs ke paise / VoiceStudio', 44.8s) and Reference Video 2 ('Script ka formula', 49.1s).")
            appendLine("  4. `kinetic-explainer-style/style/tokens.css` & `style/components.css` — Exact CSS variables, radial vignettes, viewfinder corner brackets, cards, pills, and 3-part progress bar.")
            appendLine("  5. `kinetic-explainer-style/engine/engine.js` & `template/scenes.js` — Deterministic scene engine (`window.PROJECT` + `window.seek(t)`) and complete working scene array.")
            appendLine()
            appendLine("================================================================================")
            appendLine("2. REFERENCE VIDEO VISUAL STYLE ('KINETIC CREAM & CRIMSON')")
            appendLine("================================================================================")
            appendLine("- PRIME DIRECTIVE (EVERY SPOKEN IDEA IS SHOWN VISUALLY): Never show plain text on an empty background. Every spoken phrase MUST trigger a visual object within 0.3s of the word:")
            appendLine("  * Mentioning a tool/product -> Rounded cream/red Icon Card (`560x760` at `x:1190, y:150`) with 4 crimson L-shaped Viewfinder Brackets (`34px` outside) OR a Phone Mockup (`420x850` at `x:1250, y:150`).")
            appendLine("  * Mentioning a formula/process -> Horizontal numbered `row` of mini-cards (`01 HOOK + 02 PROBLEM + 03 SOLUTION + 04 RESULT + 05 CTA`) building left-to-right with crimson `+` signs.")
            appendLine("  * Mentioning features/options -> Staggered `chips` row where the active spoken chip lights up in crimson (`#9A0B12`) while others stay warm cream (`#F6EDD2`).")
            appendLine("  * Mentioning time/numbers -> Animated circular `timer` ring (`0:00 -> 0:03` with SVG stroke-dashoffset arc fill).")
            appendLine("  * Mentioning a negative/problem (e.g. 'No subscription') -> `headline` with `strike` wipe bar across the word + round `x` badge.")
            appendLine("  * Call-To-Action (CTA) -> Typing `comment` box (`VOICE` + `POST` pill), `Follow` / `Share` light buttons, and `Following` dark capsule with check icon + round `bell` + `rocket`.")
            appendLine("- COLOR RHYTHM (CREAM EXPLAINS, RED INTERRUPTS):")
            appendLine("  * `cream` scene background (~65-70% of video): `radial-gradient(ellipse 80% 75% at 50% 44%, #F2E5B6 0%, #E2CF98 55%, #B8985F 100%)` with 16 drifting ambient dust dots.")
            appendLine("  * `red` scene background (~30-35% of video, used for pattern-interrupts like 'TOH RUK JAO!', 'DEKHO', 'BAS!', key reveals, and CTA): `radial-gradient(ellipse 80% 75% at 50% 40%, #A80E15 0%, #8A0911 58%, #4A0307 100%)`.")
            appendLine("  * Giant `ghost` background watermark word (`640px`, weight 900, `rgba(255,226,200,0.13)` on red, `rgba(154,11,18,0.07)` on cream) slowly pushing in `scale(1 -> 1.06)` across the scene.")
            appendLine("- TYPOGRAPHY HIERARCHY (`Poppins`, `Inter`, `Arial Black`, sans-serif):")
            appendLine("  * Pair a small uppercase connective `kicker` (`50px` weight 800, or `34px` small with `0.22em` tracking; `#2A0A0A` ink on cream, `#F2E5B6` on red) ~70px above ONE dominant uppercase `headline` (`xl: 230px`, `l: 150px`, `m: 100px`, weight 900, line-height `0.92`, tracking `-0.01em`).")
            appendLine("  * Max 3 text sizes per scene. Never let text touch cards, viewfinder brackets, or the bottom progress bar (keep all content above `y = 980`, safe margin `96px..120px`).")
            appendLine("- PERSISTENT 3-SEGMENT BOTTOM PROGRESS BAR:")
            appendLine("  * Centered at `bottom: 34px`, width `960px`, with 3 labeled chapters (e.g. `INTRO` | `FEATURES` or `STEPS` | `CTA` or `LINK`) that fill continuously based on current time `t`.")
            appendLine()
            appendLine("================================================================================")
            appendLine("3. CODE LANGUAGE & SELF-CONTAINED HTML5/CSS3/JS ARCHITECTURE")
            appendLine("================================================================================")
            appendLine("- Language: Standard **HTML5 + CSS3 + Vanilla ES6 JavaScript** bundled into ONE single `<!DOCTYPE html>...</html>` file.")
            appendLine("- Why single-file: `raw_edit` loads your code inside a 100% offline, network-blocked Android `WebView` (`1920x1080` viewport). Relative links like `<link href=\"../style/tokens.css\">` or `<script src=\"../engine/engine.js\">` MUST be inlined directly into `<style>` and `<script>` blocks inside the HTML file.")
            appendLine("- STRICTLY FORBIDDEN: Do NOT use `http://`, `https://`, `<script src=...>`, `<link href=...>`, `fetch()`, `XMLHttpRequest`, `WebSocket`, external images, or CSS `transition`/`@keyframes` (all motion must be computed deterministically in JS from timestamp `t`).")
            appendLine("- REQUIRED GLOBAL JS CONTRACT (`window.RAW` + `window.PROJECT`):")
            appendLine("  Include the `tokens.css` + `components.css` styles inside `<style>`, define `window.PROJECT = { duration: ${String.format("%.2f", durationSec)}, progress: [...], scenes: [...] }`, include the `engine.js` scene builder & `window.seek(t)` function, and expose `window.RAW` at the end of `<script>`:")
            appendLine("  ```js")
            appendLine("  window.RAW = {")
            appendLine("    duration: window.PROJECT.duration,")
            appendLine("    fps: 30,")
            appendLine("    width: 1920,")
            appendLine("    height: 1080,")
            appendLine("    seek: function(tSeconds) {")
            appendLine("      window.seek(Number(tSeconds) || 0);")
            appendLine("      return true;")
            appendLine("    }")
            appendLine("  };")
            appendLine("  window.RAW.seek(0);")
            appendLine("  ```")
            appendLine("- You may add new inline filled `<svg viewBox=\"0 0 24 24\" fill=\"currentColor\">` icons to the `ICON` dictionary in `engine.js` to match specific nouns in the user's script.")
            appendLine()
            appendLine("================================================================================")
            appendLine("4. PROJECT METADATA & TIMELINE INPUTS")
            appendLine("================================================================================")
            appendLine("- Project Title: ${project.title}")
            appendLine("- Voiceover Duration: ${String.format("%.3f", durationSec)} seconds ($totalFrames frames @ 30 fps)")
            appendLine()
            if (srtSnippet.isNotBlank()) {
                appendLine("================================================================================")
                appendLine("5. UPLOADED .SRT SUBTITLE CUE TIMINGS (LOCK SCENES TO THESE EXACT TIMESTAMPS!)")
                appendLine("================================================================================")
                appendLine("The user uploaded an `.srt` subtitle file synchronized to their voiceover. Use these exact `start -> end` timestamps (in seconds) to set each scene's `start`, `end`, and internal element `at` delays:")
                appendLine(srtSnippet.trim())
                appendLine()
            } else {
                appendLine("================================================================================")
                appendLine("5. SUBTITLE TIMINGS")
                appendLine("================================================================================")
                appendLine("No separate `.srt` file was uploaded; pace the scenes naturally across the ${String.format("%.2f", durationSec)}s voiceover duration (~2.5s–5.0s per scene with staggered element entrances).")
                appendLine()
            }
            if (scriptPreview.isNotBlank()) {
                appendLine("================================================================================")
                appendLine("6. USER SCRIPT / TOPIC NOTES")
                appendLine("================================================================================")
                appendLine(scriptPreview.trim())
                appendLine()
            }
            appendLine("================================================================================")
            appendLine("FINAL INSTRUCTION (CONSERVE CLAUDE CREDITS — SINGLE RESPONSE ONLY):")
            appendLine("Output ONLY the complete, self-contained `<!DOCTYPE html>...</html>` code now, with zero markdown chatter before or after:")
        }
    }

    fun buildFixPrompt(
        project: ProjectEntity,
        flags: List<FrameFlag>,
        codeHash: String,
        htmlSourceOrSlice: String,
        includeFullHtml: Boolean = true
    ): String {
        val durationSec = (project.audioDurationMs.coerceAtLeast(1000L) / 1000.0)
        return buildString {
            appendLine("================================================================================")
            appendLine("CLAUDE CREDIT GUARDRAIL: SINGLE-TURN HTML FIX ONLY (NO TOOL LOOPS, NO CHATTER)")
            appendLine("================================================================================")
            appendLine("You are fixing layout/validation issues detected by `raw_edit` Layer A pre-render keyframe analysis.")
            appendLine("Return ONLY the complete corrected `<!DOCTYPE html>...</html>` document in ONE single response with NO commentary.")
            appendLine()
            appendLine("=== 1. CONTRACT & STYLE TO PRESERVE ===")
            appendLine("- Keep the 'Kinetic Cream & Crimson' style from `kinetic-explainer-style.zip` (`window.PROJECT`, `engine.js`, and `window.RAW = { duration: ${String.format("%.3f", durationSec)}, fps: 30, width: 1920, height: 1080, seek(tSeconds) }`).")
            appendLine("- Keep 100% offline execution (no external URLs, no network calls, deterministic `seek(t)`).")
            appendLine("- Keep all visible DOM bounding boxes inside the 1920x1080 safe area (`96px..120px` margin on all sides; never let left-column headlines collide with right-column cards/brackets at `x = 1190`).")
            appendLine("- Current Code SHA-256 (first 12): ${codeHash.take(12)}")
            appendLine()
            appendLine("=== 2. FLAGGED FRAMES TO FIX (${flags.size} ISSUES) ===")
            if (flags.isEmpty()) {
                appendLine("- No hard violations flagged; tighten typography spacing and ensure zero edge clipping across all scenes.")
            } else {
                flags.forEach { flag ->
                    val tc = SrtParser.formatTimecode(flag.timeMs)
                    appendLine("- $tc -> F${flag.frameIndex} -> ${flag.tileId} -> ${flag.rule} -> ${flag.detail}")
                    if (flag.domSnapshot.isNotBlank()) {
                        appendLine("  DOM snapshot: ${flag.domSnapshot}")
                    }
                }
            }
            appendLine()
            appendLine("=== 3. CURRENT ${if (includeFullHtml) "FULL HTML" else "RELEVANT CSS/JS SLICE"} ===")
            appendLine(htmlSourceOrSlice.trim())
            appendLine()
            appendLine("Return ONLY the complete corrected `<!DOCTYPE html>...</html>` file now:")
        }
    }
}
