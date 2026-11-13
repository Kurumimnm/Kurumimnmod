import json, math, sys, ast
from PIL import Image, ImageDraw, ImageFont
out_dir=sys.argv[1]; dst=sys.argv[2]
d=json.load(open(out_dir+'/_slots.json'))
TW,TH=d['tex']; slots=d['slots']
tex=Image.open(out_dir+'/astra_veil.png').convert('RGBA')
S=3 if TW>256 else 4
F='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
ft=ImageFont.truetype(F,22); fs=ImageFont.truetype(F,12); fl=ImageFont.truetype(F,15); fx=ImageFont.truetype(F,9)
W=TW*S+1020; H=1400
img=Image.new('RGB',(W,H),(32,34,44)); dr=ImageDraw.Draw(img)
dr.text((12,14),f'astra_veil.png UV配置図（{TW}×{TH} / Box UV / 1マス=1px）',font=ft,fill=(235,235,245))
ox,oy=0,60
# 市松の背景（透明部分が分かるように）
for y in range(TH):
    for x in range(TW):
        c=(58,60,72) if (x//2+y//2)%2==0 else (48,50,60)
        dr.rectangle([ox+x*S,oy+y*S,ox+x*S+S-1,oy+y*S+S-1],fill=c)
big=tex.resize((TW*S,TH*S),Image.NEAREST)
img.paste(big,(ox,oy),big)
COL={'face':(255,210,90),'core':(255,210,90),'lens':(255,210,90),'antenna':(255,210,90),'crown':(255,210,90),
     'membrane':(110,160,255),'flat':(190,120,255),'ribbon':(120,230,200),'tentacle':(255,120,150),'feather':(150,200,255),'slit':(255,210,90),'coral':(190,120,255)}
GROUP={'face':'顔・冠・目','core':'顔・冠・目','lens':'顔・冠・目','antenna':'顔・冠・目','crown':'顔・冠・目',
       'membrane':'幕（翼3層・下の幕・胴）','flat':'平たい触腕・珊瑚の房','coral':'平たい触腕・珊瑚の房','ribbon':'渦を巻く帯','tentacle':'細い触腕','feather':'羽・背中の扇','slit':'顔・冠・目'}
NAMES={'up':'上','down':'下','east':'東','north':'北','west':'西','south':'南'}
groups={}
for k,s in slots.items():
    u,v=s['uv']; w,h,dd=[int(math.ceil(q)) for q in s['size']]; col=COL[s['mat']]
    rects={'up':(u+dd,v,w,dd),'down':(u+dd+w,v,w,dd),'east':(u,v+dd,dd,h),'north':(u+dd,v+dd,w,h),'west':(u+dd+w,v+dd,dd,h),'south':(u+2*dd+w,v+dd,w,h)}
    for fn,(x,y,ww,hh) in rects.items():
        if ww<=0 or hh<=0: continue
        dr.rectangle([ox+x*S,oy+y*S,ox+(x+ww)*S-1,oy+(y+hh)*S-1],outline=col,width=1)
        if ww>=3 and hh>=3:
            dr.text((ox+x*S+2,oy+y*S+1),NAMES[fn],font=fx,fill=col)
    labels=s['labels']
    name=labels[0] if len(labels)==1 else f"{labels[0]} ほか{len(labels)-1}個"
    sz='×'.join(str(int(q)) if float(q).is_integer() else str(q) for q in s['size'])
    groups.setdefault(GROUP[s['mat']],[]).append((col,f"{name}  UV({u},{v})  {sz}"))
lx=TW*S+30; ly=60; col_start=60; col_w=330; max_ly=0
for g,items in groups.items():
    col=items[0][0]
    dr.rectangle([lx,ly+5,lx+12,ly+17],fill=col); dr.text((lx+20,ly),g,font=fl,fill=col); ly+=24
    for _,t in items:
        dr.text((lx+20,ly),t,font=fs,fill=(220,220,230)); ly+=16
        max_ly=max(max_ly,ly)
        if ly>1300: lx+=col_w; ly=col_start
    ly+=8
notes=['・モデルの前（顔）は北（-Z）側','・同じ大きさ・同じ材質のキューブは同じUVを共有（例：帯・触腕の節）',
       '・透明ピクセル：幕の穴・下のふちの欠け（半透明で描画）','・発光：astra_veil_glowmask.png（同じ配置。目・継ぎ目・ふち・葉脈・星の粒）',
       '・モデルは小さく作り、レンダラーで8倍（ModConstants ASTRA_VEIL_RENDER_SCALE）','・Blockbench: GeckoLib Animated Model で geo.json を読み込むと同じ配置']
legend_end=max(max_ly,ly)
ny=oy+TH*S+20
for n in notes:
    dr.text((12,ny),n,font=fs,fill=(200,200,215)); ny+=18
img=img.crop((0,0,W,max(legend_end,ny)+20))
img.save(dst)
print(img.size)
