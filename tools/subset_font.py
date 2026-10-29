#!/usr/bin/env python3
"""按源码里的字符串字面量裁剪字体，保证界面文案不缺字。

源字体放在 tools/font-source/GameFont-full.ttf，每次从完整字体重新裁剪，
输出到 core/assets/fonts/GameFont.ttf。这样重复运行不会越裁越少。
"""
import os
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = pathlib.Path(os.path.expanduser("~/.aicode/fonts/GameFont-full.ttf"))
if not SRC.exists():
    SRC = pathlib.Path(os.path.expanduser("~/workspace/.fontsource/GameFont-full.ttf"))
OUT = ROOT / "core" / "assets" / "fonts" / "GameFont.ttf"

if not SRC.exists():
    raise SystemExit("缺少完整字体: " + str(SRC))

chars = set()
for p in (ROOT / "core/src").rglob("*.java"):
    text = p.read_text(encoding="utf-8")
    for literal in re.findall(r'"((?:[^"\\]|\\.)*)"', text):
        chars.update(ch for ch in literal if ord(ch) > 127)
for c in range(0x20, 0x7F):
    chars.add(chr(c))

tmp = ROOT / "build" / "font-chars.txt"
tmp.parent.mkdir(parents=True, exist_ok=True)
tmp.write_text("".join(sorted(chars)), encoding="utf-8")
OUT.parent.mkdir(parents=True, exist_ok=True)

subprocess.run([
    "python3", "-m", "fontTools.subset", str(SRC),
    "--text-file=" + str(tmp),
    "--output-file=" + str(OUT),
    "--layout-features=", "--no-hinting", "--desubroutinize",
], check=True)
print("字体已裁剪：%d 字符，%.0f KB" % (len(chars), OUT.stat().st_size / 1024))
