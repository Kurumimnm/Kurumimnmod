# アストラ・ヴェイルのモデル生成スクリプト

アストラ・ヴェイルのモデル・テクスチャ・待機アニメを、手作業ではなくスクリプトで生成しています（キューブが約 1650 個あるため）。

| ファイル | 内容 |
|---|---|
| generate.py | geo.json・テクスチャ（本体・発光マスク）・待機アニメを生成する |
| preview.py | GeckoLib と同じ変換で組み立てて、簡易レンダリングした画像を出す（形の確認用） |
| uv_doc.py | UV 配置図を出す |

```
python3 generate.py out          # out/ に astra_veil.geo.json・.png・_glowmask.png・.animation.json
python3 preview.py out/astra_veil.geo.json out/astra_veil.png out/astra_veil_glowmask.png out/preview.png player   # 視点: player（地上から見上げ）/ front / side / below / under / top
python3 uv_doc.py out out/uv.png
```

- 生成したファイルを assets/kurumimnmod/... にコピーして使います
- Blockbench で手直しした場合は、再生成すると上書きされます。手直し後はスクリプトを使わないでください
- 乱数のシードを固定しているので、同じスクリプトからは毎回同じ結果になります

- `generate.py` の `K` がモデルの細かさです（4 で 1 ピクセル = 1/8 ブロック）。変えたら `ModConstants.ASTRA_VEIL_RENDER_SCALE` を 8 / K にしてください
