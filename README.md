# raw_edit v2 — 100% Offline Android Typography Reel Engine

`raw_edit` turns a voiceover (plus optional `.srt` subtitles and script/topic notes) into a **1920×1080, 30 fps H.264 + AAC MP4 typography reel**, rendered completely offline on-device.

## Key Highlights

- **Strictly 100% Offline**: Zero `android.permission.INTERNET` in `AndroidManifest.xml`. Works in airplane mode from first launch. All cloud AI authoring and fix loops operate via manual copy-paste.
- **Android 9 (API 28) Minimum to Android 16 (API 36) Target**: Tuned for butter-smooth execution on low-end Android 9 devices (`DevicePerformanceOptimizer`, pre-packed primitive `IntArray` pixel sprites with zero per-frame allocations, `Modifier.graphicsLayer` animations, windowed monospace code viewer for >20k chars, and single reused `Bitmap` capture buffers).
- **Editorial Typography-Led Home Screen**: Brutalist stacked typography (`YOUR VOICE BECOMES TYPE.`), single primary `START CREATING` CTA, quiet monospace counter strip, and ambient drifting white pixel mascots. All projects, drafts, and renders live exclusively in **My Reels**.
- **Layer A 5-Stage Pipeline**:
  1. **Stage 1 — Live DOM Autofit Preview**: Real-time 1920×1080 virtual WebView canvas with non-destructive `#raw-edit-autofit-wrapper` scale/translate transforms and DOM overflow/contrast reporting.
  2. **Stage 2 — Pre-Render Keyframe Contact Sheet**: Generates timestamped A4 landscape 4×3 **PDF** contact sheets (with Appendix table) and **ZIP** archives (`keyframes/*.png`, `sheet/sheet_01.pdf`, `sheet/manifest.json`, `sheet/index.html`, `FIX_PROMPT.txt`) with permanent burned-in monospace caption bars.
  3. **Stage 3 — In-App Sheet Review**: Paged 12-tile viewer with pinch-to-zoom, `ALL` / `FLAGGED` filtering, DOM snapshot inspection, and frame jump.
  4. **Stage 4 — Manual Cloud-AI Fix Loop**: Copyable structured fix prompt with exact measured overflow/flag coordinates, paste-back validation, fast flagged-frame re-check, and fix attempt history log.
  5. **Stage 5 — Final On-Device MP4 Render**: Foreground service (`mediaProcessing|dataSync`) encoding 1920×1080 @ 30 fps H.264 muxed with 128 kbps AAC audio transcoded via Media3 Transformer.
- **16 Procedural White Pixel Mascots & 34 Custom Canvas Icons**: Zero binary `.png`, `.ttf`, or `.jks` files in the repository.
