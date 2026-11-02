# Kurumimnmod

Minecraft **26.3** / **NeoForge 26.3** 向けの MOD です。

| 項目 | 値 |
|---|---|
| MOD ID | `kurumimnmod` |
| パッケージ | `io.github.kurumimnm.kurumimnmod` |
| NeoForge | 26.3.0.100 |
| Java | 25 |
| ビルド | Gradle 9.2.1 + ModDevGradle 2.0.148 |
| GeckoLib | 5.5.7（必須前提MOD） |

## 開発環境

1. **JDK 25** をインストール（Microsoft Build of OpenJDK / Temurin 推奨）
2. VSCode に拡張機能 **Extension Pack for Java** と **Gradle for Java** を入れる
3. このリポジトリをクローンして VSCode で開く（初回は Minecraft 本体のダウンロード・展開で時間がかかります）

### よく使うコマンド

```bash
./gradlew runClient       # クライアント起動（runs/client）
./gradlew runServer       # 専用サーバー起動
./gradlew runData         # データ生成（src/generated/resources に出力）
./gradlew build           # jar 作成（build/libs）
./gradlew --refresh-dependencies   # 依存関係の再取得
```

Windows では `gradlew.bat` を使ってください。

## ディレクトリ構成

```
src/main/java/io/github/kurumimnm/kurumimnmod/
├── Kurumimnmod.java          … メインクラス（エントリーポイント）
├── KurumimnmodClient.java    … クライアント専用処理
├── client/
│   └── ModEntityRenderers.java … エンティティレンダラー登録（GeckoLib）
├── datagen/
│   ├── ModDataGenerators.java     … runData のエントリーポイント
│   ├── ModModelProvider.java      … モデル JSON 生成
│   └── ModEnUsLanguageProvider.java … en_us.json 生成
├── config/
│   ├── ModConfigs.java       … ゲーム中に調整する値（kurumimnmod-synced.toml）
│   └── ModConstants.java     … 登録時に決まる固定値（耐久値・攻撃力など）
└── registry/
    ├── ModRegistries.java    … DeferredRegister の一括登録
    ├── ModItems.java
    ├── ModBlocks.java
    ├── ModEntities.java      … エンティティ登録・属性登録（追加手順はJavadoc参照）
    └── ModCreativeTabs.java  … ModItems の全アイテムを自動で並べる MOD タブ
src/main/resources/assets/kurumimnmod/lang/ja_jp.json … 日本語（手書き）
src/generated/resources/ … runData の出力（en_us.json・モデル等。手で編集しない）
src/main/templates/META-INF/neoforge.mods.toml … gradle.properties の値が埋め込まれる
```

## GeckoLib（Blockbench）ファイルの置き場所

Blockbench で「GeckoLib Animated Model」として作成し、書き出したファイルを以下に置きます（例: `walnut_mob`）。

| 種類 | パス |
|---|---|
| モデル | `assets/kurumimnmod/geckolib/models/entity/walnut_mob.geo.json` |
| アニメーション | `assets/kurumimnmod/geckolib/animations/entity/walnut_mob.animation.json` |
| テクスチャ（Box UV） | `assets/kurumimnmod/textures/entity/walnut_mob.png` |

ファイル名をエンティティの登録名と揃えると、`GeoEntityRenderer` が自動でパスを解決します。
`.bbmodel`（Blockbench のプロジェクトファイル）はリポジトリに置いても jar には含まれません。

## データ生成（runData）

英語の言語ファイルとアイテム・ブロックのモデル JSON は `datagen/` のコードから自動生成します。

1. `datagen/` の Provider に追記する（日本語は `ja_jp.json` に手書き）
2. `./gradlew runData` を実行する
3. `src/generated/resources` の変更もコミットする

GitHub Actions で runData の結果とコミット内容を比較しているので、実行し忘れるとビルドが失敗します。

## 数値の調整場所

- **ゲーム中・ワールドごとに変えたい値**（スポーン確率など）→ `config/ModConfigs.java`
- **登録時に確定する値**（耐久値・攻撃力・移動速度など）→ `config/ModConstants.java`

MOD のバージョン・作者・説明文などは `gradle.properties` で管理します。
