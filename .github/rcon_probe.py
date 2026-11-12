import socket,struct,sys
def pkt(s,i,t,b):
    d=struct.pack("<ii",i,t)+b.encode()+b"\x00\x00";s.sendall(struct.pack("<i",len(d))+d)
def rd(s):
    n=struct.unpack("<i",s.recv(4))[0];d=b""
    while len(d)<n: d+=s.recv(n-len(d))
    return d[8:-2].decode(errors="replace")
s=socket.create_connection(("127.0.0.1",25575),timeout=600)
pkt(s,1,3,"probe");rd(s)
def run(c):
    pkt(s,2,2,c);r=rd(s);print(">",c);print("  ",r);sys.stdout.flush();return r
mode=sys.argv[1]
O="execute in minecraft:overworld run "
if mode=="setup":
    for c in ["kmod quest list", O+"forceload add 0 0 31 31",
              O+"summon minecraft:pig 5 70 5 {Tags:[\"kurumimnmod.no_drops\",\"p1\"]}",
              O+"summon minecraft:pig 25 70 25 {Tags:[\"p2\"]}"]:
        run(c)
elif mode=="check":
    for c in [O+"data get entity @e[tag=p1,limit=1] Pos", O+"data get entity @e[tag=p2,limit=1] Pos",
              O+"kill @e[tag=p1]"]:
        run(c)
    import time; time.sleep(2)
    run(O+"execute if entity @e[type=minecraft:item]")
    run(O+"kill @e[tag=p2]")
    time.sleep(2)
    run(O+"execute if entity @e[type=minecraft:item]")
    run(O+"data get entity @e[type=minecraft:item,limit=1] Item")
