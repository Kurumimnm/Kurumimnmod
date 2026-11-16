# 専用サーバー（rcon）でアストラ・ヴェイルを出し、クライアント（Xvfb 上）のプレイヤーをいろいろな位置へ動かして画面を撮る
import socket, struct, time, sys, subprocess, os
def pkt(s, i, t, b):
    d = struct.pack("<ii", i, t) + b.encode() + b"\x00\x00"; s.sendall(struct.pack("<i", len(d)) + d)
def rd(s):
    n = struct.unpack("<i", s.recv(4))[0]; d = b""
    while len(d) < n: d += s.recv(n - len(d))
    return d[8:-2].decode(errors="replace")
s = socket.create_connection(("127.0.0.1", 25575), timeout=600)
pkt(s, 1, 3, "probe"); rd(s)
def run(c):
    pkt(s, 2, 2, c); r = rd(s); print(">", c, "|", r.strip()[:300]); sys.stdout.flush(); return r
os.makedirs("shots", exist_ok=True)
def shot(name, wait=12):
    time.sleep(wait)
    subprocess.run(["import", "-window", "root", f"shots/{name}.png"], check=False)
    print("shot", name); sys.stdout.flush()
for i in range(90):
    r = run("list")
    if "1 of" in r or ": " in r and r.strip().split(": ")[-1]:
        if "0 of" not in r:
            break
    time.sleep(5)
time.sleep(25)
shot("00_joined", 1)
run("time set 18000"); run("weather clear")
run("gamemode spectator @a")
run("summon kurumimnmod:astra_veil 0.5 -60 0.5")
SEL = "@e[type=kurumimnmod:astra_veil,limit=1]"
time.sleep(3)
run(f"data get entity {SEL} Pos")
def view(name, local, wait=14):
    run(f"execute as {SEL} at @s rotated as @s run tp @a {local} facing entity {SEL} feet")
    shot(name, wait)
view("01_front_far", "^ ^20 ^330")
view("02_front", "^ ^0 ^230")
view("03_side", "^230 ^0 ^")
view("04_back", "^ ^10 ^-260")
view("05_ground_below", "^40 ^-110 ^80")
view("06_top", "^ ^260 ^1")
view("07_quarter", "^170 ^-30 ^170")
view("08_very_far", "^ ^60 ^700", 20)
# 胴の中心が画面外でも触腕・傘が消えないか（中心から大きく横を向く）
run(f"execute as {SEL} at @s rotated as @s run tp @a ^150 ^-60 ^150 facing ^-60 ^-60 ^-200")
shot("09_edge_cull", 12)
time.sleep(60)
run(f"data get entity {SEL} Pos")
view("10_moving_side", "^240 ^0 ^", 10)
view("11_moving_below", "^30 ^-100 ^40", 12)
time.sleep(40)
view("12_moving_quarter", "^-180 ^-20 ^180", 12)
run(f"data get entity {SEL} Pos")
