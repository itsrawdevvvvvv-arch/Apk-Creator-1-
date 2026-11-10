#!/usr/bin/env python3
"""Render template/index.html (scenes.js) to MP4. Needs: python3, playwright (+ its Chromium), ffmpeg. No AI models.
Usage: python3 tools/render.py --out out.mp4 [--voice voice.mp3] [--music bed.mp3] [--fps 30] [--scale 1] [--start 0 --end 5]
  --scale 1 = 1920x1080, 0.5625 = 1080x608 (the reference size). --voice/--music are YOUR audio files; both optional."""
import argparse, subprocess, shutil, pathlib, tempfile
from playwright.sync_api import sync_playwright
a=argparse.ArgumentParser(); a.add_argument('--out',default='out.mp4'); a.add_argument('--voice'); a.add_argument('--music')
a.add_argument('--fps',type=int,default=30); a.add_argument('--scale',type=float,default=1.0)
a.add_argument('--start',type=float,default=0); a.add_argument('--end',type=float); a.add_argument('--sheet',help='write a contact sheet jpg instead of video (frame every 0.5s)')
a=a.parse_args()
page_url=(pathlib.Path(__file__).resolve().parent.parent/'template'/'index.html').as_uri()
tmp=pathlib.Path(tempfile.mkdtemp())
with sync_playwright() as p:
    b=p.chromium.launch(); pg=b.new_page(viewport={'width':1920,'height':1080},device_scale_factor=a.scale)
    pg.goto(page_url); pg.wait_for_timeout(400)
    dur=pg.evaluate('PROJECT_DURATION'); end=min(a.end or dur,dur)
    step=0.5 if a.sheet else 1/a.fps; n=int((end-a.start)/step)
    for i in range(n):
        pg.evaluate(f'seek({a.start+i*step})'); pg.screenshot(path=str(tmp/f'f{i:05d}.png'))
    b.close()
if a.sheet:
    subprocess.run(['ffmpeg','-y','-v','error','-framerate','1','-i',str(tmp/'f%05d.png'),'-vf','scale=384:-1,tile=6x0','-frames:v','1',a.sheet],check=True)
else:
    cmd=['ffmpeg','-y','-v','error','-framerate',str(a.fps),'-i',str(tmp/'f%05d.png')]
    ins=1
    if a.voice: cmd+=['-ss',str(a.start),'-i',a.voice]; vi=ins; ins+=1
    if a.music: cmd+=['-stream_loop','-1','-ss',str(a.start),'-i',a.music]; mi=ins; ins+=1
    if a.voice and a.music: cmd+=['-filter_complex',f'[{mi}:a]volume=0.14[m];[{vi}:a][m]amix=inputs=2:duration=first:normalize=0,loudnorm=I=-16:TP=-1.5[a]','-map','0:v','-map','[a]','-shortest']
    elif a.voice: cmd+=['-filter_complex',f'[{vi}:a]loudnorm=I=-16:TP=-1.5[a]','-map','0:v','-map','[a]','-shortest']
    cmd+=['-c:v','libx264','-pix_fmt','yuv420p','-crf','18','-preset','medium','-c:a','aac','-b:a','192k','-movflags','+faststart',a.out]
    subprocess.run(cmd,check=True); print('wrote',a.out)
shutil.rmtree(tmp)
