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
def at(x,z,cmd):
    return run(f"execute positioned {x} 0 {z} positioned over motion_blocking run {cmd}")
def info(tag):
    for p in ["Pos","Health","Armored","Flying","anger_end_time","NoGravity"]:
        run(f"data get entity @e[tag={tag},limit=1] {p}")
run("time set day"); run("difficulty normal")
for cx in (0, 600):
    run(f"forceload add {cx-48} -48 {cx+47} 47")
time.sleep(10)
run("fill 580 199 -20 620 199 20 minecraft:stone")
print("==== A. ground under spawn 0,0")
run("execute positioned 0 0 0 positioned over motion_blocking if block ~ ~-1 ~ minecraft:water")
print("==== B. platform: group fight then landing")
for i,(x,z) in enumerate([(600,0),(604,0),(596,4)]):
    run(f'summon kurumimnmod:griffin {x} 200 {z} {{Tags:["p","p{i}"]}}')
run('summon minecraft:pig 610 200 10 {Tags:["victim"]}')
time.sleep(3)
run("damage @e[tag=p0,limit=1] 2 minecraft:mob_attack by @e[tag=victim,limit=1]")
for k in range(25):
    time.sleep(2)
    print("t=",2*(k+1))
    run("execute as @e[tag=p] run data get entity @s Flying")
    run("execute as @e[tag=p] run data get entity @s Pos[1]")
    run("execute if entity @e[tag=victim]")
print("==== C. near water at 0,0")
for i,(x,z) in enumerate([(0,0),(3,0)]):
    run(f'execute positioned {x} 0 {z} positioned over motion_blocking run summon kurumimnmod:griffin ~ ~ ~ {{Tags:["w","w{i}"]}}')
run('execute positioned 6 0 6 positioned over motion_blocking run summon minecraft:pig ~ ~ ~ {Tags:["victim2"]}')
time.sleep(2)
run("damage @e[tag=w0,limit=1] 2 minecraft:mob_attack by @e[tag=victim2,limit=1]")
time.sleep(50)
run("execute as @e[tag=w] run data get entity @s Flying")
run("execute as @e[tag=w] run data get entity @s Pos")
run("execute as @e[tag=w] at @s if block ~ ~-1 ~ minecraft:water")
run("execute as @e[type=kurumimnmod:griffin] run data get entity @s Health")
