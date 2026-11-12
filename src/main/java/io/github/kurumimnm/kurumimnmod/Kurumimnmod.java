package io.github.kurumimnm.kurumimnmod;

import com.mojang.logging.LogUtils;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Kurumimnmod のメインクラス（エントリーポイント）。
 * <p>
 * MOD ロード時に最初に呼ばれるのがこのクラスのコンストラクタ。
 * ここでは「登録処理の呼び出し」と「Config の登録」だけを行い、
 * 個々のアイテム・ブロック等の定義は registry パッケージ側に置く方針。
 */
@Mod(Kurumimnmod.MOD_ID)
public final class Kurumimnmod {

    /** MOD ID。gradle.properties の mod_id / neoforge.mods.toml と一致させること。 */
    public static final String MOD_ID = "kurumimnmod";

    /** MOD 共通のロガー。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * FML が IEventBus（MOD イベントバス）と ModContainer を自動で渡してくれる。
     */
    public Kurumimnmod(IEventBus modEventBus, ModContainer modContainer) {
        // ---- レジストリ登録（アイテム・ブロック・クリエイティブタブ等） ----
        ModRegistries.register(modEventBus);

        // ---- Config 登録 ----
        ModConfigs.register(modContainer);

        // ---- ライフサイクルイベント ----
        modEventBus.addListener(this::commonSetup);

        // ---- probe: dump ArrowItem API (temporary) ----
        try {
            StringBuilder sb = new StringBuilder("PROBE_ARROWITEM ");
            for (java.lang.reflect.Method m : net.minecraft.world.item.ArrowItem.class.getDeclaredMethods()) {
                if (m.getName().toLowerCase().contains("arrow")) {
                    sb.append(m).append(" || ");
                }
            }
            LOGGER.error(sb.toString());
            StringBuilder sb2 = new StringBuilder("PROBE_ARROW_CTOR ");
            for (java.lang.reflect.Constructor<?> c : net.minecraft.world.entity.projectile.arrow.Arrow.class.getDeclaredConstructors()) {
                sb2.append(c).append(" || ");
            }
            LOGGER.error(sb2.toString());
        } catch (Throwable t) {
            LOGGER.error("PROBE_FAILED", t);
        }
    }

    /** クライアント・サーバー共通のセットアップ処理。 */
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[{}] common setup", MOD_ID);
    }
}
