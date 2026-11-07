import socket,struct,time,sys
def pkt(s,i,t,b):
    d=struct.pack("<ii",i,t)+b.encode()+b"\x00\x00";s.sendall(struct.pack("<i",len(d))+d)
def rd(s):
    n=struct.unpack("<i",s.recv(4))[0];d=b""
    while len(d)<n: d+=s.recv(n-len(d))
    return d[8:-2].decode(errors="replace")
s=socket.create_connection(("127.0.0.1",25575),timeout=600)
pkt(s,1,3,"probe");rd(s)
def run(c):
    pkt(s,2,2,c);r=rd(s);print(">",c);print(r);sys.stdout.flush();return r
run("time set day"); run("difficulty normal")
# A: small pool (15x15) at x=600 / B: large pool (41x41, land beyond search range 12) at x=800
run("forceload add 560 -48 640 47")
run("forceload add 750 -48 850 47")
time.sleep(8)
Y=200
for cx,plat,pool in ((600,20,7),(800,30,20)):
    run(f"fill {cx-plat} {Y-2} {-plat} {cx+plat} {Y-1} {plat} minecraft:stone")
    run(f"fill {cx-pool} {Y-1} {-pool} {cx+pool} {Y-1} {pool} minecraft:water")
time.sleep(2)
for tag,x,z in (("a0",600,0),("a1",603,3),("b0",800,0),("b1",804,-3)):
    run(f'summon kurumimnmod:griffin {x}.5 {Y-1} {z}.5 {{Tags:["g","{tag}"]}}')
for k in range(12):
    time.sleep(10)
    print("==== t=",10*(k+1))
    for tag in ("a0","a1","b0","b1"):
        run(f"data get entity @e[tag={tag},limit=1] Flying")
        run(f"data get entity @e[tag={tag},limit=1] Pos")
        run(f"execute as @e[tag={tag}] at @s if block ~ ~-0.5 ~ minecraft:stone")
        run(f"execute as @e[tag={tag}] at @s if entity @s[nbt={{OnGround:1b}}]")
