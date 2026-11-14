# アストラ・ヴェイルのモデル生成スクリプト

アストラ・ヴェイル（幅 約 384・高さ 約 256 ブロックの超大型ボス）のモデル・テクスチャ・待機アニメ・リグを、スクリプトで生成しています（ボーン 約 3,100・キューブ 約 6,300 個あるため）。

| ファイル | 内容 |
|---|---|
| generate.py | astra_veil.geo.json・astra_veil.png・astra_veil_glowmask.png・astra_veil.animation.json・astra_veil.rig.json を生成する |
| preview.py | GeckoLib と同じ変換で組み立てて、簡易レンダリングした画像を出す（形・姿勢の確認用） |
| uv_doc.py | UV 配置図を出す |

```
python3 generate.py out
python3 preview.py out/astra_veil.geo.json out/astra_veil.png out/astra_veil_glowmask.png out/preview.png front side back top   # 視点: front / back / side / top / under / quarter
python3 preview.py out/_pose_move.geo.json out/astra_veil.png out/astra_veil_glowmask.png out/move.png side top             # 移動中の姿勢（_pose_idle_a / idle_b / move / turn / climb）
python3 uv_doc.py out out/uv.png
```

生成したファイルの置き場所（`src/main/resources/assets/kurumimnmod/`）：

| 生成物 | 置き場所 |
|---|---|
| astra_veil.geo.json | geckolib/models/entity/ |
| astra_veil.animation.json | geckolib/animations/entity/ |
| astra_veil.png・astra_veil_glowmask.png | textures/entity/ |
| astra_veil.rig.json | astra_veil/rig.json（名前を変えて置く） |

- Blockbench で手直しした場合は、再生成すると上書きされます。手直し後はスクリプトを使わないでください
  （ボーンの向き・位置を変えた場合、リグ（rig.json）は古いままになるので、たなびきの向きがずれます）
- 乱数のシードを固定しているので、同じスクリプトからは毎回同じ結果になります
- 1 ピクセル = 1/2 ブロック（`U = 2`）。変えたら `ModConstants.ASTRA_VEIL_RENDER_SCALE` を 16 / U にしてください
- 原点（エンティティの位置）は胴の中心。傘は y = +52〜+106、胴は +52〜-74、触腕の先は -130 あたり（ブロック）

## 形を変える主な場所

| 変えたいもの | 場所 |
|---|---|
| 傘の大きさ | `HA, HB, HY, HH`（半径 x・z、ふちの高さ、高さ） |
| 顔の大きさ・位置 | `FS`（倍率）、`FY`、`mask_w`（仮面の輪郭）、`CREST`（冠のとげ）、`EYES`（琥珀色の器官） |
| 胴の太さ | `BR`（高さごとの半径） |
| 左右・後ろ・前・上の大型触腕 | `LATS` / `BACKS` / `FRONTS` / `UPS`（向き・仰角・長さ・幅・渦の巻き） |
| 触腕の色 | `paint` の `mat == "tent"` |
| 待機アニメの波 | `tentacle(...)` の `wave`（大きさ）・`wspeed`（速さ）・`wstep`（根元から先端への伝わり方） |
| 移動時のたなびき | `tentacle(...)` の `drag`（後ろへ流れる量）・`lag`（追従の遅れ）・`flutter`（波の大きさ）・`ffreq`（波の速さ） |

## ボーンの名前（攻撃アニメなどで使う。Java 側の一覧は `AstraVeilPart`）

`root → core`（全体の上下）`→ head`（傘）`/ torso`（胴全体）
- 頭部：`face`（顔）、`eye_a0〜5`・`eye_b0〜5`（琥珀色の器官）、`face_gem_0〜3`、`crest_0〜8`、`lobe_*`・`veil_*`（ふちの葉・膜）、`ant_*`（触角）、`up_a0〜2`・`up_b0〜2`（上の触腕）
- 胴体：`body_0〜5`（上から下）、`core_organ_0〜4`・`body_organ_*`（発光器官）、`strand_*`（房）、`oral_*`（口腕）
- 大型触腕：`mantle`（付け根）→ `lat_a0〜5`・`lat_b0〜5`（左右）、`back_0〜7`、`front_0〜3`、`down_0〜4`、`rib_0〜9`（帯）
- 触腕は「根元のボーン（例 `lat_a2`）→ 節（`lat_a2_0`, `lat_a2_1`, …）」の鎖。根元を回すと触腕全体が動く
