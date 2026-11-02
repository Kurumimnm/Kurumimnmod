# Kurumimnmod

Minecraft **26.3** / **NeoForge 26.3** 向けの MOD です。

| 項目 | 値 |
|---|---|
| MOD ID | `kurumimnmod` |
| パッケージ | `io.github.kurumimnm.kurumimnmod` |
| NeoForge | 26.3.0.100 |
| Java | 25 |
| ビルド | Gradle 9.2.1 + ModDevGradle 2.0.148 |

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
├── config/
│   ├── ModConfigs.java       … ゲーム中に調整する値（kurumimnmod-synced.toml）
│   └── ModConstants.java     … 登録時に決まる固定値（耐久値・攻撃力など）
└── registry/
    ├── ModRegistries.java    … DeferredRegister の一括登録
    ├── ModItems.java
    ├── ModBlocks.java
    └── ModCreativeTabs.java  … ModItems の全アイテムを自動で並べる MOD タブ
src/main/resources/assets/kurumimnmod/lang/
├── en_us.json
└── ja_jp.json
src/main/templates/META-INF/neoforge.mods.toml … gradle.properties の値が埋め込まれる
```

## 数値の調整場所

- **ゲーム中・ワールドごとに変えたい値**（スポーン確率など）→ `config/ModConfigs.java`
- **登録時に確定する値**（耐久値・攻撃力・移動速度など）→ `config/ModConstants.java`

MOD のバージョン・作者・説明文などは `gradle.properties` で管理します。
