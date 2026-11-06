# 地形 v2 の試作（Python）

ワールド生成 v2 の設計（Notion「04_ワールド生成 v2 設計（案）」）で使った試作です。MOD には入りません。

- `erode.py` : 共通の部品（ノイズ・窪地の埋め立て・流れの向き・河川侵食・崩落）
- `erode2.py` : 「設計した高さ ＋ 侵食」で地形を作る（メディアナ周辺 8192×8192 ブロック、8 ブロック／マス）
- `render.py` : 上から見た地図と、地上からの見た目（簡易 3D）を描く

```
pip install numpy scipy numba pillow
python3 erode2.py 150 0.012   # → eroded.npz
python3 render.py             # → v2_*.png
```

出力のパスは `/home/claude/proto/` 固定なので、使うときは書き換えてください。
