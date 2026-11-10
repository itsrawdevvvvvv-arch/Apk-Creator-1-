---
name: kinetic-explainer-style
description: Make short (40-60 s) 16:9 kinetic-typography explainers in the "cream and crimson" style - giant Poppins headlines, icon cards with viewfinder brackets, phone mockups, chips, counters, follow/comment CTAs - where every spoken idea is shown visually. Editing only; the user supplies the voiceover audio. Use for AI-tool showcases, "formula/steps" explainers, or any video that should look like the reference style.
---
# Kinetic Explainer Style (editing only)
You build the video from the user's script and voiceover audio. No voice generation, no transcription, no model downloads.
Needs: Node-free. Python 3, `pip install playwright && playwright install chromium`, ffmpeg, Poppins TTFs in `fonts/` (free, Google Fonts).
**Read `docs/STYLE_BIBLE.md` first (especially section 0), then `docs/BEAT_SHEETS.md`. Look at `reference/*.jpg`.**

## Workflow
1. **Intake (one message)**: topic, script text, voiceover file (and scene start times, or say "I'll listen and time it"), language, optional music file, handle/CTA keyword. Defaults: Hinglish-style structure, 45 s.
2. **Plan (one screen, then wait)**: a table `narration line | scene type + bg colour | visual object(s) on screen | start s`. Every line MUST have a visual object (Prime Directive). Check cream/red rhythm. Ask "Build it?".
3. **Build**: copy `template/scenes.js`, rewrite scenes (bg, start/end, els with `at`). Element types: ghost, kicker, headline (size xl/l/m, color crimson/ink, outline, strike), sub, pill, chips, card (icon/timer/red/brackets/num), row, phone (img), buttons, comment, round (icon). Icons: bulb doc q check cursor smile x bell follow share mic play rocket. Add new icons to `engine.js` in the same filled style.
4. **QA**: `python3 tools/render.py --sheet review.jpg` and LOOK at it; run the checklist in STYLE_BIBLE section 9. Fix and re-render. Single frame: open `template/index.html?t=12.5`.
5. **Deliver**: `python3 tools/render.py --out final.mp4 --voice voice.mp3 [--music bed.mp3]`. Report what you caught and fixed.

## Never
Plain text-only scenes; new colours/fonts; copying another creator's script or screenshots; claiming a frame is fine without looking at it; installing speech or AI models.
