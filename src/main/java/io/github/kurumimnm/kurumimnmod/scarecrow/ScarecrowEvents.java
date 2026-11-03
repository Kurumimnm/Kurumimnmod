package io.github.kurumimnm.kurumimnmod.scarecrow;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * カカシのダメージ計測（サーバー側）。
 * <p>
 * 【カカシが受けるダメージの計算順】
 * <ol>
 *   <li>バニラ：武器・エンチャント・防具ポイント（設定の「アーマー」）など</li>
 *   <li>StatEvents：攻撃したプレイヤーの攻撃力（魔法なら知力）・技量のクリティカル</li>
 *   <li>ここ：カカシの防御力（プレイヤーの防御力ステータスと同じ式の固定値軽減）</li>
 *   <li>ここ：カカシの魔法耐性（魔法ダメージだけ % 軽減）</li>
 * </ol>
 * 最後の値を「最終ダメージ」として頭上に表示し、HP 無限なら HP は減らさない。
 * 有限 HP で削り切る一撃は赤で表示し、倒れずに全回復する。
 * <p>
 * 【反撃でプレイヤーが受けたダメージ】プレイヤー側の軽減（防具・防御力）が終わった値をアクションバーに表示する。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class ScarecrowEvents {

    private ScarecrowEvents() {}

    // =====================================================================
    // クリティカルの記録
    // =====================================================================

    /** StatEvents（技量）の判定が確定したあとに、カカシへのクリティカルを記録する。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onCriticalHit(CriticalHitEvent event) {
        if (event.isCriticalHit() && event.getTarget() instanceof ScarecrowEntity scarecrow
                && !scarecrow.level().isClientSide()) {
            scarecrow.markCritical();
        }
    }

    // =====================================================================
    // カカシが受けるダメージ
    // =====================================================================

    /** ほかの軽減処理がすべて終わってから最終値を決める（優先度 LOWEST）。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onScarecrowDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ScarecrowEntity scarecrow) || scarecrow.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        // /kill・奈落は通常どおり（撤去できなくなるのを防ぐ）
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        ScarecrowSettings settings = scarecrow.getSettings();
        float damage = event.getNewDamage();
        boolean magic = isMagic(source);

        // ---- 防御力（攻撃者のいるダメージだけ。プレイヤーの防御力と同じ式） ----
        if (source.getEntity() != null && damage > 0) {
            float reduction = (float) (Math.max(0, settings.defense() - 1) * ModConfigs.defenseReductionPerPoint());
            if (reduction > 0) {
                float minDamage = (float) Math.min(ModConfigs.defenseMinDamage(), damage);
                damage = Math.max(minDamage, damage - reduction);
            }
        }
        // ---- 魔法耐性 ----
        if (magic && settings.magicResistance() > 0) {
            damage *= 1f - settings.magicResistance() / 100f;
        }
        damage = Math.max(0f, damage);

        // ---- 表示の種類 ----
        DamageKind kind;
        boolean lethal = !settings.infiniteHealth() && damage >= scarecrow.getHealth();
        if (lethal) {
            kind = DamageKind.BREAK;
        } else if (scarecrow.wasCriticalThisTick() && source.getDirectEntity() instanceof Player) {
            kind = DamageKind.CRITICAL;
        } else if (magic) {
            kind = DamageKind.MAGIC;
        } else if (source.getEntity() == null) {
            kind = DamageKind.OTHER;
        } else {
            kind = DamageKind.NORMAL;
        }
        scarecrow.recordHit(damage, kind);

        // ---- HP への反映 ----
        if (settings.infiniteHealth()) {
            event.setNewDamage(0f);
        } else if (lethal) {
            // 倒れずに全回復（「削り切った」ことは赤い数字で分かる）
            event.setNewDamage(0f);
            scarecrow.restoreHealth();
        } else {
            event.setNewDamage(damage);
        }
    }

    /** 魔法攻撃か（MOD の魔法タグ・バニラの魔法ダメージ）。 */
    private static boolean isMagic(DamageSource source) {
        return source.is(ModTags.DamageTypes.MAGIC_ATTACK) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    // =====================================================================
    // 反撃でプレイヤーが受けたダメージ
    // =====================================================================

    /** プレイヤーの防具・防御力で軽減されたあとの値を、アクションバーに表示する。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onPlayerDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() instanceof ScarecrowEntity) {
            player.sendOverlayMessage(Component.translatable("message.kurumimnmod.scarecrow.damage_taken",
                            format(event.getInflictedDamage()), format(event.getOriginalDamage()))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /** 小数第 1 位まで表示する（整数なら小数なし）。 */
    public static String format(float value) {
        float rounded = Math.round(value * 10f) / 10f;
        return rounded == (long) rounded ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }
}
