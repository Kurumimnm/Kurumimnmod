package io.github.kurumimnm.kurumimnmod.client.status;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.level.LevelManager;
import io.github.kurumimnm.kurumimnmod.level.PlayerLevel;
import io.github.kurumimnm.kurumimnmod.status.StatType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 【仮置き】HUD とステータス画面に表示する値をまとめて返すクラス。
 * <p>
 * 今回は UI だけを作るため、データは次のように仮で用意している。
 * <ul>
 *   <li>HP … バニラの体力をそのまま使用</li>
 *   <li>MP … 固定値（{@link ModConstants#PLACEHOLDER_MAX_MP}）</li>
 *   <li>レベル・経験値 … 【本実装済み】サーバーから同期された {@link PlayerLevel}（{@link LevelManager}）</li>
 *   <li>ステータス … クライアント側のメモリ上だけに保持（保存・同期なし）</li>
 * </ul>
 * 本実装（サーバー側のデータ保存＋同期）ができたら、このクラスの中身だけを
 * 差し替えれば HUD・画面側は修正不要になるよう、表示側は必ずこのクラス経由で値を取得する。
 */
public final class ClientStatusPlaceholder {

    private ClientStatusPlaceholder() {}

    /** 振り分け済みのステータス（仮：ログアウトしても消えないが、ゲームを再起動すると消える）。 */
    private static final Map<StatType, Integer> STATS = new EnumMap<>(StatType.class);

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    // ---- HP ----

    public static float health() {
        LocalPlayer p = player();
        return p == null ? 0 : p.getHealth();
    }

    public static float maxHealth() {
        LocalPlayer p = player();
        return p == null ? 1 : Math.max(1, p.getMaxHealth());
    }

    // ---- MP（仮：常に満タン） ----

    public static float mana() {
        return ModConstants.PLACEHOLDER_MAX_MP;
    }

    public static float maxMana() {
        return ModConstants.PLACEHOLDER_MAX_MP;
    }

    // ---- レベル・経験値（サーバーから同期されたデータ） ----

    private static PlayerLevel levelData() {
        LocalPlayer p = player();
        return p == null ? PlayerLevel.DEFAULT : LevelManager.get(p);
    }

    public static int level() {
        return levelData().level();
    }

    /** 現在のレベル内で貯まっている経験値。 */
    public static long exp() {
        return levelData().exp();
    }

    /** 次のレベルまでに必要な経験値。 */
    public static long requiredExp() {
        return levelData().requiredExp();
    }

    /** 次のレベルまでの進捗（0.0〜1.0）。 */
    public static float levelProgress() {
        return levelData().progress();
    }

    // ---- ステータス ----

    public static int stat(StatType type) {
        return STATS.getOrDefault(type, 0);
    }

    /** 振り分け済みポイントの合計。 */
    public static int spentPoints() {
        int sum = 0;
        for (int v : STATS.values()) {
            sum += v;
        }
        return sum;
    }

    /** まだ振り分けていないポイント。 */
    public static int unspentPoints() {
        // 初期レベルではポイントなし。1 レベル上がるごとに STAT_POINTS_PER_LEVEL もらえる
        int levelsGained = level() - ModConstants.INITIAL_LEVEL;
        return Math.max(0, levelsGained * ModConstants.STAT_POINTS_PER_LEVEL - spentPoints());
    }

    /** ポイントを 1 振る。成功したら true。 */
    public static boolean allocate(StatType type) {
        if (unspentPoints() <= 0) {
            return false;
        }
        STATS.merge(type, 1, Integer::sum);
        return true;
    }
}
