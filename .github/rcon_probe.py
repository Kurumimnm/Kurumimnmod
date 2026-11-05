import socket,struct
def pkt(s,i,t,b):
    d=struct.pack("<ii",i,t)+b.encode()+b"\x00\x00";s.sendall(struct.pack("<i",len(d))+d)
def rd(s):
    n=struct.unpack("<i",s.recv(4))[0];d=b""
    while len(d)<n: d+=s.recv(n-len(d))
    return d[8:-2].decode(errors="replace")
s=socket.create_connection(("127.0.0.1",25575),timeout=60)
pkt(s,1,3,"probe");print("auth",rd(s))
cmds=["kmod towns","execute positioned 0 64 0 run kmod area","execute positioned 0 64 -4500 run kmod area",
"execute positioned 2000 64 -7000 run kmod area","execute positioned -2600 64 -9000 run kmod area",
"execute positioned 3000 64 -16000 run kmod area","execute positioned 4500 64 -12000 run kmod area",
"locate biome kurumimnmod:whispering_forest","locate biome kurumimnmod:sandstorm_desert","locate biome minecraft:plains",
"execute positioned 0 64 0 run locate biome kurumimnmod:cradle_basin","forceload add -4200 -9100 -4100 -9000",
"execute positioned -4150 100 -9050 run kmod area"]
for i,c in enumerate(cmds):
    pkt(s,10+i,2,c);print(">",c);print(rd(s))
