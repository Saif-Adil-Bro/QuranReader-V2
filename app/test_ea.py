import urllib.request, re

req = urllib.request.Request("https://everyayah.com/data/", headers={"User-Agent": "Mozilla/5.0"})
try:
    with urllib.request.urlopen(req) as resp:
        html = resp.read().decode("utf-8", errors="ignore")
        folders = re.findall(r'href=["\']?([a-zA-Z0-9_\-]+/)', html)
        for f in sorted(list(set(folders))):
            fl = f.lower()
            if any(k in fl for k in ["khal", "tun", "tan", "mus", "aza", "rad", "shur", "sud", "bas", "hud", "qari"]):
                print("EVERYAYAH FOLDER:", f)
except Exception as e:
    print("ERR:", e)
