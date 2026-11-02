package io.github.kurumimnm.kurumimnmod.datagen;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * データ生成（runData）のエントリーポイント。
 * <p>
 * {@code ./gradlew runData} を実行すると、ここに登録した Provider が
 * src/generated/resources に JSON を出力する。
 * <ul>
 *   <li>{@link ModModelProvider} … アイテム・ブロックのモデル、ブロックステート、アイテム定義</li>
 *   <li>{@link ModEnUsLanguageProvider} … 英語の言語ファイル（en_us.json）</li>
 * </ul>
 * 日本語（ja_jp.json）は src/main/resources に手書きで管理する。
 * <p>
 * ※ レシピ・ルートテーブル・タグは、必要になったタイミングで Provider を追加する。
 *    （26.3 ではレシピがデータパックレジストリ方式の GatherDataRegistryEntriesEvent に移行している）
 */
// ModelProvider はクライアント側のクラスなので、クライアント環境でのみ読み込む
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ModDataGenerators {

    private ModDataGenerators() {}

    @SubscribeEvent
    static void onGatherClientData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
        event.createProvider(ModEnUsLanguageProvider::new);
    }
}
