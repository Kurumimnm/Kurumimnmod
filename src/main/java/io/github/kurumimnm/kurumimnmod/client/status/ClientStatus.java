package io.github.kurumimnm.kurumimnmod.client.status;

import io.github.kurumimnm.kurumimnmod.level.LevelManager;
import io.github.kurumimnm.kurumimnmod.level.PlayerLevel;
import io.github.kurumimnm.kurumimnmod.status.AllocType;
import io.github.kurumimnm.kurumimnmod.status.StatManager;
import io.github.kurumimnm.kurumimnmod.status.StatType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * HUD とステータス画面に表示する値をまとめて返すクラス（クライアント側）。
 * <p>
 * 値はすべてサーバーから同期されたデータ（アタッチメント・属性・SYNCED Config）から計算する。
 * <ul>
 *   <li>HP … バニラの体力（最大 HP はステータスが属性に反映済み）</li>
 *   <li>MP … {@link StatManager#mana} / {@link StatManager#maxMana}</li>
 *   <li>レベル・経験値 … {@link LevelManager}</li>
 *   <li>ステータス … {@link StatManager}</li>
 * </ul>
 * 表示側は必ずこのクラス経由で値を取得する（プレイヤー未ログイン時の null 対策をここに集約）。
 */
public final class ClientStatus {

    private ClientStatus() {}

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

    // ---- MP ----

    public static float mana() {
        LocalPlayer p = player();
        return p == null ? 0 : StatManager.mana(p);
    }

    public static float maxMana() {
        LocalPlayer p = player();
        return p == null ? 0 : StatManager.maxMana(p);
    }

    // ---- レベル・経験値 ----

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

    /** ステータスの最終的な値。 */
    public static double stat(StatType type) {
        LocalPlayer p = player();
        return p == null ? 0 : StatManager.value(p, type);
    }

    /** 振り分け済みのポイント数。 */
    public static int allocated(AllocType type) {
        LocalPlayer p = player();
        return p == null ? 0 : StatManager.get(p).points(type);
    }

    /** まだ振り分けていないポイント。 */
    public static int unspentPoints() {
        LocalPlayer p = player();
        return p == null ? 0 : StatManager.unspentPoints(p);
    }

    /** クリティカル率（0.0〜1.0。ツールチップ表示用）。 */
    public static double critChance() {
        LocalPlayer p = player();
        return p == null ? 0 : StatManager.critChance(p);
    }

    /** クリティカル倍率（ツールチップ表示用）。 */
    public static float critMultiplier() {
        LocalPlayer p = player();
        return p == null ? 1 : StatManager.critMultiplier(p);
    }

    /** 攻撃力による与ダメージ倍率（ツールチップ表示用）。 */
    public static float attackMultiplier() {
        LocalPlayer p = player();
        return p == null ? 1 : StatManager.attackMultiplier(p);
    }

    /** 防御力による軽減量（ツールチップ表示用）。 */
    public static float defenseReduction() {
        LocalPlayer p = player();
        return p == null ? 0 : StatManager.defenseReduction(p);
    }
}
