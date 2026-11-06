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
    for c in ["locate structure kurumimnmod:spawn_house",
              "execute positioned 0 64 0 run kmod area","execute positioned 900 64 0 run kmod area","execute positioned 0 64 1000 run kmod area",
              # 街：メディアナ・1（村）・2（都市）・5（城下町）・3（港町）
              "forceload add -80 -80 79 79","forceload add -48 -3048 47 -2953",
              "forceload add -112 -6112 111 -5889","forceload add -96 -9096 95 -8905",
              "forceload add 2504 -7596 2695 -7405",
              "execute positioned 0 64 -900 run kmod area"]:
        run(c)
elif mode=="save":
    run("save-all flush")
