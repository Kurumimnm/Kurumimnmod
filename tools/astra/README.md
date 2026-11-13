# アストラのモデル生成スクリプト

アストラ（星幽の巡界者・全幅 約 500 ブロック）のモデル・テクスチャ・待機アニメを、スクリプトで生成しています（キューブが約 2300 個あるため）。
アストラ・ヴェイル（`tools/astra_veil`）とは別のモデルです。

| ファイル | 内容 |
|---|---|
| generate.py | astra.geo.json・astra.png・astra_glowmask.png・astra.animation.json を生成する |
| preview.py | GeckoLib と同じ変換で組み立てて、簡易レンダリングした画像を出す（形の確認用） |
| uv_doc.py | UV 配置図を出す |

```
python3 generate.py out
python3 preview.py out/astra.geo.json out/astra.png out/astra_glowmask.png out/preview.png front   # 視点: front / player（地上から見上げ）/ side / top / under / below
python3 uv_doc.py out out/uv.png
```

- 生成したファイルを `assets/kurumimnmod/geckolib/...`・`textures/entity/` にコピーして使います
- Blockbench で手直しした場合は、再生成すると上書きされます。手直し後はスクリプトを使わないでください
- 乱数のシードを固定しているので、同じスクリプトからは毎回同じ結果になります
- 1 ピクセル = 1/2 ブロック（`ModConstants.ASTRA_RENDER_SCALE` = 8）。原点（エンティティの位置）はあごの先
- 形を変える主な場所：`build_wing(...)` の引数（膜の付け根・傾き・扇の角度・長さ）、`up_shape` / `low_shape` / `tail_shape`（膜の反り）、`face_fields` / `paint_face`（顔の絵）
- 攻撃アニメ用のボーン：`head`、`jaw_l` / `jaw_r`（あご）、`wing_up_l` / `wing_up_r` / `wing_low_l` / `wing_low_r` / `tail_c`（膜の付け根）、`talon_*`（爪）
