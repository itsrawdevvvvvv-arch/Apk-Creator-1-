# STYLE BIBLE: "Kinetic Cream & Crimson" explainer

Source: two reference videos (45 s and 49 s, 16:9, 30 fps, H.264+AAC). Colours, sizes and layout were read from full-size frames
and pixel samples; motion timings are ESTIMATES (the references were sampled every 2.5 s, not frame by frame). See `reference/`.

## 0. PRIME DIRECTIVE: EVERYTHING IS PRESENTED VISUALLY
This is a typography-and-layout video. Nobody is ever shown plain text on an empty screen, and nothing is only spoken.
1. **Every spoken idea gets an object on screen**: a tool or product -> logo card or phone mockup; a feature -> chip/pill that lights up; a step -> numbered badge + big icon card; a number or time -> counter or progress ring; a choice -> chips with one active; an action (follow, comment, share) -> the real-looking button or typing comment box. Object appears within 0.3 s of the word.
2. **Typography is the hero**: one dominant headline per scene, in Poppins Black, huge, with a small kicker above it. Three sizes of text per scene at most.
3. **Layout is a system, not a guess**: text column on the left, object column on the right, centred stack only for interrupts and the outro. Same margins every scene.
4. **Design polish on every frame**: soft shadows, rounded cards, viewfinder corner brackets, subtle paper-grain/vignette, floating dust, a 3-segment progress bar. If a frame looks like a slide with text on it, redo it.
5. **Consistency**: only the palette, fonts and components below. Never add a new colour, font or icon style.
6. **Build, don't flash**: elements enter one after another (stagger), so the screen "assembles" in time with the voice.

## 1. Format
1920x1080, 16:9, 30 fps, H.264 yuv420p, AAC. (References are 1080x608; render at 1080p, or `--scale 0.5625` for the exact reference size.) Length 40-60 s. Average scene 2.5 s; scenes of 5-7 s are allowed when elements keep building inside them.

## 2. Palette (use only these)
| role | hex | use |
|---|---|---|
| cream (scene bg) | radial `#F2E5B6` centre -> `#E2CF98` -> `#B8985F` corners | explanation scenes (vignette is strong) |
| crimson | `#9A0B12` | headlines on cream, active chips, icons, brackets, progress fill |
| red (scene bg) | radial `#A80E15` -> `#8A0911` -> `#4A0307` | interrupts, big reveals, CTA |
| ink | `#2A0A0A` | kickers, sub-lines, dark pills, step badge |
| card | `#F8F1DC` -> `#F4ECD8` -> `#D9CBA3` | cards, light pills, buttons |
| ghost word | cream at 13% on red, crimson at 7% on cream | giant background word |
**Colour rhythm:** cream explains, red interrupts. Red was ~37% of video 1 and ~30% of video 2. Red = the "TOH RUK JAO" interrupt after the hook, the "most interesting" beat, the CTA. Switch with a hard cut.

## 3. Typography (Poppins, free on Google Fonts; put TTFs in `fonts/`)
| role | weight | size @1920 | notes |
|---|---|---|---|
| headline XL / L / M | 900 | 230 / 150 / 100 px | UPPERCASE, line-height .92, tracking -0.01em, one per scene; crimson on cream with tan offset shadow `0 9px 0 rgba(120,80,20,.28)`; cream on red with soft dark shadow |
| kicker | 800 | 50 px (small variant 34 px, tracking .22em) | the connective word above the headline: TOH, YE EK, PHIR, USKE BAAD, AUR BAS |
| sub-line | 800 | 70 px | second line under the headline (2-3 SECONDS, PROBLEM FIX) |
| pill / chip text | 800 | 34 / 30 px | uppercase |
| progress labels | 700 | 17 px, tracking .18em | |
| outline variant | 900 | any | transparent fill, 3 px cream stroke; for "second-level" words (MULTIPLE LANGUAGES) |
Pairing rule: **small dark kicker + huge crimson headline**, left-aligned, kicker sits ~70 px above the headline's cap line.

## 4. Layout grid (1920x1080)
- Left text column: x = 150, y = 130 (step badge) -> 260 (kicker) -> 330 (headline) -> 520-600 (sub-line) -> 650-720 (chips). Column width ~850.
- Right object column: card x = 1190, y = 150, 560x760 (phone: 420x850 at x = 1250). Brackets sit 34 px outside the card.
- Centre stack (interrupts, CTA, outro): x = 960 anchored centre; kicker y = 180-300, headline y = 250-380.
- Bottom: progress bar centred, 960 px wide, 34 px from the bottom. Keep everything else above y = 1000.
- Safe margin 120 px. Never let text touch the card or brackets.

## 5. Components (all in `style/components.css`, built by `engine/engine.js`)
- **Step badge / pill**: dark ink capsule "STEP 01". **Chip row**: light cream capsules; the active one is crimson.
- **Icon card**: 560x760 rounded (64 px) cream card with a soft long shadow, huge crimson icon, optional red variant, optional counter ring (0:00 -> 0:03 with arc fill).
- **Viewfinder brackets**: four crimson L-corners around the card. They appear 0.1 s after it.
- **Phone mockup**: black body, 70 px radius, shows a real screenshot of the tool (the reference does exactly this).
- **Formula row**: numbered mini-cards joined by crimson "+" signs, building left to right.
- **Strike-through word** (NA SUBSCRIPTION): word pops, an ink line wipes across it, a round X badge pops.
- **Buttons**: Follow / Share light buttons; Following dark pill with check; round bell; typed comment box with caret and POST.
- **Ghost word**: giant background word of the scene's key term, slow 6% push-in over the scene.
- **Dust**: 16 tiny drifting dots on cream scenes. **Progress bar**: 3 labelled segments (INTRO / STEPS or FEATURES / CTA or LINK) filling continuously.

## 6. Motion (estimated)
Hard cuts between scenes (cream<->red swap). Inside a scene: headline words pop word by word (stagger .12-.2 s), pop = scale .55 -> 1 with ~7% overshoot over .28 s; cards rise 46 px + fade over .4 s or pop; chips pop with .12 s stagger; brackets fade in; counters tick; ghost word drifts; dust floats up. No camera shake, no 3D, no wipes between scenes.

## 7. Script / copy pattern (language of the references: Hinglish)
Kicker = connective, headline = the one key word. Typical headline words are 1-2 words. Arc: hook -> red interrupt -> explanation (feature/steps with visuals) -> "most interesting" red beat -> CTA (follow + comment-keyword / "link DM mein") -> cream outro with Following button and bell. Re-write in whatever language the creator uses; keep the structure.

## 8. Audio (only level was measured)
Voice-forward mix: mean about -17 dB, peaks -1 to -2.6 dB (volumedetect). Music content and sound effects were NOT analysed. Recommendation (not measured): quiet music bed at ~14% of voice level, soft pop/whoosh on card entrances, final mix loudnorm -16 LUFS. `tools/render.py` does this mix when you pass `--voice` and `--music`.

## 9. Visual QA before delivery (render `--sheet`, then look at it)
[ ] every sentence of narration has a visible object  [ ] one headline per scene, max 3 text sizes  [ ] text never touches cards/brackets/progress bar
[ ] only palette colours  [ ] cream/red rhythm followed, red scenes at interrupt / reveal / CTA  [ ] kicker + headline pair present  [ ] progress bar fills in order
[ ] icons all from the same set  [ ] spelling checked  [ ] final scene has Follow button + bell  [ ] nothing is a plain text-on-blank scene
