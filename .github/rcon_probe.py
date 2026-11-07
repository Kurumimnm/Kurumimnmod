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
    # アストラリア：始まりの村・紅河大橋・霧沼大橋（峡谷）・裂溝大橋・南の海岸を読み込む
    for c in ['kmod towns', 'execute positioned 0 100 5600 run kmod area', 'execute positioned 0 100 4400 run kmod area', 'execute positioned 0 100 3000 run kmod area', 'execute positioned -2800 100 2080 run kmod area', 'execute positioned -2800 100 2300 run kmod area', 'execute positioned -3350 100 -3120 run kmod area', 'execute positioned -3000 100 6750 run kmod area', 'execute positioned 0 100 0 run kmod area', 'execute positioned -300 100 -400 run kmod area', 'execute positioned 6400 100 400 run kmod area', 'execute positioned -2000 100 -7400 run kmod area', 'forceload add -128 5472 127 5727', 'forceload add -128 2880 127 3119', 'forceload add -3104 1952 -2849 2207', 'forceload add -2848 1952 -2593 2207', 'forceload add -2592 1952 -2465 2207', 'forceload add -3504 -3312 -3249 -3057', 'forceload add -3504 -3056 -3249 -2929', 'forceload add -3248 -3312 -3169 -3057', 'forceload add -3248 -3056 -3169 -2929', 'forceload add -3120 6640 -2865 6879']:
        run(c)
elif mode=="push":
    pass
elif mode=="save":
    run("data get entity @e[tag=probe,type=zombie,limit=1,sort=nearest] Pos")
    run("execute as @e[tag=probe] run data get entity @s Pos")
    run("execute at @e[tag=probe,type=zombie] run kmod area")
    run("execute if entity @e[type=!minecraft:player,tag=!probe]")
    run("execute if entity @e[type=!minecraft:item,type=!minecraft:player,tag=!probe]")
    run("save-all flush")
