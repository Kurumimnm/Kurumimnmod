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
    # クエスト型：現世の始まりの街・中間拠点・フィールドの区画（確認用に作る）を読み込む
    for c in ['kmod quest list', 'kmod quest preview kurumimnmod:first_trial 1', 'kmod quest preview kurumimnmod:forked_road 2b', 'kmod quest preview kurumimnmod:first_trial boss', 'kmod quest preview kurumimnmod:forked_road 2a', 'kmod quest preview kurumimnmod:first_trial 1', 'execute in minecraft:overworld run forceload add -96 -96 95 95', 'execute in kurumimnmod:quest run forceload add -64 -64 63 63', 'execute in kurumimnmod:quest run forceload add -112 3984 111 4207', 'execute in kurumimnmod:quest run forceload add 3920 3920 4175 4175', 'execute in kurumimnmod:quest run forceload add 3920 4176 4175 4271', 'execute in kurumimnmod:quest run forceload add 4176 3920 4271 4175', 'execute in kurumimnmod:quest run forceload add 4176 4176 4271 4271', 'execute in kurumimnmod:quest run forceload add 8128 4032 8255 4159', 'execute in kurumimnmod:quest run forceload add 12112 3920 12367 4175', 'execute in kurumimnmod:quest run forceload add 12112 4176 12367 4271', 'execute in kurumimnmod:quest run forceload add 12368 3920 12463 4175', 'execute in kurumimnmod:quest run forceload add 12368 4176 12463 4271', 'execute in kurumimnmod:quest run forceload add 16272 3984 16495 4207']:
        run(c)
elif mode=="push":
    pass
elif mode=="save":
    run("execute if entity @e[type=!minecraft:player]")
    run("save-all flush")
