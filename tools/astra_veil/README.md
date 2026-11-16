# アストラ・ヴェイルのモデル生成スクリプト

アストラ・ヴェイル（空を水平に泳ぐ超大型ボス。幅 約 370・高さ 約 240・奥行 約 320 ブロック、移動時は奥行 約 390 ブロック）の
モデル・テクスチャ・待機アニメ・リグを、スクリプトで生成しています（ボーン 約 1,900・キューブ 約 3,400 個あるため）。

| ファイル | 内容 |
|---|---|
| generate.py | astra_veil.geo.json・astra_veil.png・astra_veil_glowmask.png・astra_veil.animation.json・astra_veil.rig.json を生成する |
| preview.py | GeckoLib と同じ変換で組み立てて、簡易レンダリングした画像を出す（形・姿勢の確認用） |
| uv_doc.py | UV 配置図を出す（docs/uv/astra_veil_uv.png） |

```
python3 generate.py out
python3 preview.py out/astra_veil.geo.json out/astra_veil.png out/astra_veil_glowmask.png out/preview.png front side top   # 視点: front / back / side / top / under / player / quarter
python3 preview.py out/_pose_move.geo.json out/astra_veil.png out/astra_veil_glowmask.png out/move.png side player         # 移動中の姿勢（_pose_idle_a / idle_b / move / turn / climb）
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
- 原点（エンティティの位置）は胸の下。傘は y = +66〜+106、胴の芯は y ≒ +20、触腕の先は y ≒ -90 あたり（ブロック）

## 形を変える主な場所

| 変えたいもの | 場所 |
|---|---|
| 傘の大きさ | `HA, HB, HY, HH`（半径 左右・前後、ふちの高さ、高さ）、`ZH`（傘の中心の前後位置） |
| 顔の大きさ・位置 | `FS`（倍率）、`FY`、`mask_w`（仮面の輪郭）、`CREST`（冠のとげ）、`EYES`（琥珀色の器官） |
| 頭の芯（顔の裏〜首） | `HEAD_CORE`（前後の位置, 半径） |
| 首・胴の長さ・太さ・曲がり | `SEG`（節ごとの 長さ・下向きの曲げ・横の曲げ・半径）。`CHEST` = 触腕の付け根の節 |
| 大型触腕 8 本 | `LARGE`（付け根の角度・前後のずれ・向き・仰角・長さ（胴に対する倍率）・幅・渦）、`TL`（基準の長さ = 胴 ÷ 1.2） |
| 尾の飾り帯 | `streamer_*` のループ（本数・長さ・垂れ具合） |
| 触腕の色 | `paint` の `mat == "tent"`／胴の色は `mat in ("dbody", "dcore")` |
| 待機アニメの波 | `tentacle(...)` の `wave`（大きさ）・`wspeed`（速さ）・`wstep`（根元から先端への伝わり方）、胴は `SEG` のループの `add_anim` |
| 移動時のたなびき | `tentacle(...)` の `drag`（後ろへ流れる量）・`lag`（追従の遅れ）・`flutter`（波の大きさ）・`ffreq`（波の速さ）、胴は `add_rig(..., axis=(0, 0, 1), w=...)` |

## ボーンの名前（攻撃アニメなどで使う。Java 側の一覧は `AstraVeilPart`）

`root → core`（全体の上下）`→ head`（傘・顔）`/ torso`（首〜胴）
- 頭部：`face`（顔）、`eye_a0〜5`・`eye_b0〜5`（琥珀色の器官）、`face_gem_0〜3`、`crest_0〜8`、`brow_*`、`whisker_*`、`jaw_organ_*`、
  `lobe_*`・`scale*_*`・`veil_*`（ふちの葉・膜）、`star_*`・`crown_gem`、`ant_*`（触角）、`rim_*`（ふちの細い触腕）
- 首・胴：`body_0〜14`（関節。0〜1 = 首、2〜3 = 胸、4〜14 = 胴〜尾）→ 見た目は `body_N_s`（呼吸でふくらむ殻）。
  `spine_*`（背の棘）、`fin_a*`・`fin_b*`（脇のひれ）、`belly_organ_*`・`flank_organ_*`（発光器官）、`oral_*`（口腕）、`btl_*`（胴の下の細い触腕）、
  `tail_fin_0〜4`（尾びれ）、`streamer_0〜3`（尾の飾り帯）
- 大型触腕：`tent_a0〜3`（右）・`tent_b0〜3`（左）。0 が前、3 が後ろ
- 触腕は「根元のボーン（例 `tent_a2`）→ 節（`tent_a2_0`, `tent_a2_1`, …）」の鎖。根元を回すと触腕全体が動く
