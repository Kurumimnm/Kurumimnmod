import socket, struct, time, sys, re, math
def pkt(s, i, t, b):
    d = struct.pack("<ii", i, t) + b.encode() + b"\x00\x00"; s.sendall(struct.pack("<i", len(d)) + d)
def rd(s):
    n = struct.unpack("<i", s.recv(4))[0]; d = b""
    while len(d) < n: d += s.recv(n - len(d))
    return d[8:-2].decode(errors="replace")
s = socket.create_connection(("127.0.0.1", 25575), timeout=600)
pkt(s, 1, 3, "probe"); rd(s)
def run(c, show=True):
    pkt(s, 2, 2, c); r = rd(s)
    if show: print(">", c); print(r); sys.stdout.flush()
    return r
for cx in range(-512, 512, 256):
    for cz in range(-512, 512, 256):
        run(f"forceload add {cx} {cz} {cx + 255} {cz + 255}", show=False)
run("forceload query 0 0")
time.sleep(20)
run("summon kurumimnmod:astra_veil 0.5 -60 0.5")
SEL = "@e[type=kurumimnmod:astra_veil,limit=1]"
num = re.compile(r"-?\d+\.?\d*(?:[eE]-?\d+)?")
rows = []
t0 = time.time()
STEP = 5.0
for i in range(96):
    p = run(f"data get entity {SEL} Pos", show=False); r = run(f"data get entity {SEL} Rotation", show=False)
    pv = [float(x) for x in num.findall(p.split("data:")[-1])][:3]; rv = [float(x) for x in num.findall(r.split("data:")[-1])][:2]
    if len(pv) < 3 or len(rv) < 1:
        print("no entity:", p, r); break
    rows.append((time.time() - t0, *pv, rv[0]))
    time.sleep(STEP)
print("  t(s)      x       y       z     yaw   speed(b/s)  climb(b/s)  turn(deg/s)")
mx_speed = mx_turn = 0; stopped = 0; path = 0
for a, b in zip(rows, rows[1:]):
    dt = b[0] - a[0]
    sp = math.hypot(b[1] - a[1], b[3] - a[3]) / dt; cl = (b[2] - a[2]) / dt
    tr = ((b[4] - a[4] + 180) % 360 - 180) / dt
    mx_speed = max(mx_speed, sp); mx_turn = max(mx_turn, abs(tr)); stopped += sp < 0.3; path += sp * dt
    print(f"{b[0]:6.0f} {b[1]:7.1f} {b[2]:7.1f} {b[3]:7.1f} {b[4]:7.1f} {sp:10.2f} {cl:10.2f} {tr:10.2f}")
print(f"SUMMARY samples={len(rows)} max_speed={mx_speed:.2f} b/s max_turn={mx_turn:.2f} deg/s hovering_samples={stopped} path={path:.0f} blocks "
      f"y_range={min(r[2] for r in rows):.1f}..{max(r[2] for r in rows):.1f} max_dist_from_spawn={max(math.hypot(r[1], r[3]) for r in rows):.0f}")
run(f"execute if entity {SEL}")
