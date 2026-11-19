"""Resize the Flutter-rendered logo for Android and iOS.

Run from mobile/: python3 tool/resize_app_icons.py
Requires Pillow (python3 -m pip install Pillow).
"""
import json
from pathlib import Path

from PIL import Image

root = Path(__file__).resolve().parents[1]
with Image.open(root / 'assets/app_icon/app_icon.png') as rendered:
    icon = rendered.convert('RGB')
    icon.save(root / 'assets/app_icon/app_icon.png')
    ios = root / 'ios/Runner/Assets.xcassets/AppIcon.appiconset'
    entries = json.loads((ios / 'Contents.json').read_text())['images']
    for entry in entries:
        size = round(float(entry['size'].split('x')[0]) * float(entry['scale'].rstrip('x')))
        icon.resize((size, size), Image.Resampling.LANCZOS).save(ios / entry['filename'])
    for density, size in {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}.items():
        destination = root / f'android/app/src/main/res/mipmap-{density}/ic_launcher.png'
        icon.resize((size, size), Image.Resampling.LANCZOS).save(destination)
print('Updated the Android and iOS launcher icons.')
