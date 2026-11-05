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
              # 南北の断面（x=0〜31、z=-1600〜1800）と東西の断面（z=0〜31、x=-1600〜1600）
              "forceload add 0 -1600 31 99","forceload add 0 100 31 1800",
              "forceload add -1600 0 -1 31","forceload add 0 0 1600 31",
              # 峠のあたり（x=-400〜400, z=-1000〜-700）
              "forceload add -400 -1000 400 -700"]:
        run(c)
elif mode=="save":
    run("save-all flush")
