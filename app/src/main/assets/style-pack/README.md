# Kinetic Explainer Style (editing-only pack)
Copies the editing style of the two reference videos: cream and crimson, giant Poppins headlines, icon cards with viewfinder brackets, phone mockups, chips, counters, 3-part progress bar, Follow/Comment CTAs. **Everything is presented visually.**
- `SKILL.md` for any AI (Claude, ChatGPT, etc.): give it this folder and your script + voiceover.
- `docs/STYLE_BIBLE.md` colours, type scale, layout grid, components, motion, QA. `docs/BEAT_SHEETS.md` the two reference timelines.
- `style/` CSS tokens + components. `engine/engine.js` deterministic scene player. `template/scenes.js` a working rebuild of reference 2.
- `tools/render.py` renders to MP4 (optional `--voice`, `--music`). `reference/` the evidence images; `rebuild_proof_sheet.jpg` is the template rendered.
Setup: Python 3, `pip install playwright`, `playwright install chromium`, ffmpeg, Poppins TTFs in `fonts/`.
Honest limits: motion curves and audio effects are estimates; the music and sound effects were not analysed; phone screens need your own screenshots.
