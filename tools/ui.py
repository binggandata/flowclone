#!/usr/bin/env python3
"""读取 uiautomator xml：列出文本节点 / 按文本点击（坐标取 bounds 中心）。"""
import sys, re, subprocess, os, glob
ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")

def load(path=None):
    if path is None:
        subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
        out = subprocess.run([ADB, "exec-out", "cat", "/sdcard/ui.xml"], capture_output=True).stdout
        import xml.etree.ElementTree as ET
        return ET.fromstring(out)
    import xml.etree.ElementTree as ET
    return ET.parse(path).getroot()

def center(b):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)
    x1,y1,x2,y2 = map(int, m.groups())
    return (x1+x2)//2, (y1+y2)//2

def nodes(root):
    for n in root.iter("node"):
        yield n

def show(root):
    for n in nodes(root):
        t, d = n.get("text",""), n.get("content-desc","")
        if t or d:
            cx, cy = center(n.get("bounds","[0,0][0,0]"))
            print(f"{'CLICK' if n.get('clickable')=='true' else '     '} ({cx:4d},{cy:4d}) text={t!r} desc={d!r} bounds={n.get('bounds')}")

def find(root, needle):
    best = None; best_score = -1
    for n in nodes(root):
        for v in (n.get("text",""), n.get("content-desc","")):
            if needle and needle in v:
                score = (10 if n.get("clickable")=="true" else 0) + (5 if v.strip()==needle else 0) + (1 if v.startswith(needle) else 0)
                if score > best_score:
                    best, best_score = n, score
    return best

def tap(x, y):
    subprocess.run([ADB, "shell", "input", "tap", str(x), str(y)], capture_output=True)

if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "show"
    root = load(sys.argv[2] if len(sys.argv) > 2 and sys.argv[2].endswith(".xml") else None)
    if cmd == "show":
        show(root)
    elif cmd == "tap":
        n = find(root, sys.argv[2])
        if n is None: print("NOT_FOUND"); sys.exit(1)
        print("hit:", repr(n.get("text")), repr(n.get("content-desc")), n.get("clickable"), n.get("bounds"))
        x,y = center(n.get("bounds"))
        tap(x,y); print(f"TAPPED ({x},{y})")
