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
mode=sys.argv[1]
if mode=="load":
    for c in ["locate structure minecraft:pillager_outpost","locate structure minecraft:mineshaft","locate structure #minecraft:village",
              "locate structure minecraft:stronghold","locate structure minecraft:ruined_portal",
              "execute positioned 0 64 0 run kmod area","execute positioned 900 64 0 run kmod area","execute positioned 0 64 1000 run kmod area",
              # 街：メディアナ・1（村）・2（都市）・5（城下町）・3（港町）
              "forceload add -256 -256 -1 -1","forceload add 0 -256 255 -1","forceload add -256 0 -1 255","forceload add 0 0 255 255",
              "forceload add -128 -1100 127 -845","forceload add -96 -3096 95 -2905",

              "execute positioned 0 64 -900 run kmod area",
              # 山脈の斜面（安山岩・鉱石・湧き水が出ていないか）
              "forceload add -48 880 47 975",
              # ボーダーの壁：メディアナの街の境（半径48）へ向けてゾンビを飛ばす
              "time set night","execute positioned 40 64 0 run kmod area","execute positioned 60 64 0 run kmod area"]:
        run(c)
elif mode=="push":
    # 街（半径48）の内側から外へ、ゾンビと矢を飛ばす。壁が効いていれば x=48 の手前で止まる／矢は消える
    run("execute positioned 40 0 0 positioned over motion_blocking run summon minecraft:zombie ~ ~ ~ {Motion:[2.5d,0.4d,0.0d],PersistenceRequired:1b,Tags:[\"probe\",\"p1\"]}")
    run("execute positioned 40 0 4 positioned over motion_blocking run summon minecraft:arrow ~ ~3 ~ {Motion:[3.0d,0.0d,0.0d],NoGravity:1b,Tags:[\"probe\",\"p2\"]}")
    # 比べる用：壁のない場所（街の中）で同じように飛ばす
    run("execute positioned -20 0 -10 positioned over motion_blocking run summon minecraft:zombie ~ ~ ~ {Motion:[2.5d,0.4d,0.0d],PersistenceRequired:1b,Tags:[\"probe\",\"p3\"]}")
    for k in range(4):
        time.sleep(1.5)
        run("execute as @e[tag=probe] run data get entity @s Pos")
elif mode=="save":
    run("data get entity @e[tag=probe,type=zombie,limit=1,sort=nearest] Pos")
    run("execute as @e[tag=probe] run data get entity @s Pos")
    run("execute at @e[tag=probe,type=zombie] run kmod area")
    run("execute if entity @e[type=!minecraft:player,tag=!probe]")
    run("execute if entity @e[type=!minecraft:item,type=!minecraft:player,tag=!probe]")
    run("save-all flush")
