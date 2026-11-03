package io.github.kurumimnm.kurumimnmod.scarecrow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * カカシ 1 体ごとの設定（設定用ステッキの設定画面で編集する値）。
 * <p>
 * 【被弾側】
 * <ul>
 *   <li>{@code maxHealth} … 最大 HP（1〜1024）</li>
 *   <li>{@code infiniteHealth} … true なら HP が減らない（ダメージは計測・表示される）</li>
 *   <li>{@code defense} … 防御力。プレイヤーの防御力ステータスと同じ計算式で固定値軽減</li>
 *   <li>{@code armor} … バニラの防具ポイント（0〜30）</li>
 *   <li>{@code magicResistance} … 魔法ダメージの軽減率（%）</li>
 *   <li>{@code knockbackResistance} … ノックバック耐性（%）</li>
 *   <li>{@code noKnockback} … true なら一切吹き飛ばない（ノックバック耐性の値は無視）</li>
 * </ul>
 * 【反撃側】
 * <ul>
 *   <li>{@code counterEnabled} … 近くのプレイヤーを攻撃するか</li>
 *   <li>{@code counterDamage} … 攻撃力（プレイヤーの防具・防御力で軽減される前の値）</li>
 *   <li>{@code counterInterval} … 攻撃間隔（秒）</li>
 *   <li>{@code counterRange} … 射程（ブロック）</li>
 * </ul>
 * カカシ本体の NBT（{@link #CODEC}）に保存され、設定画面とのやり取りは {@link #STREAM_CODEC} で行う。
 * 値の範囲は {@link #sanitized()} で必ず丸める（改造クライアント対策としてサーバー側でも呼ぶ）。
 */
public record ScarecrowSettings(
        float maxHealth,
        boolean infiniteHealth,
        int defense,
        float armor,
        float magicResistance,
        float knockbackResistance,
        boolean noKnockback,
        boolean counterEnabled,
        float counterDamage,
        float counterInterval,
        float counterRange) {

    /** Config が読めないとき用の初期値（Config の既定値と同じ）。 */
    public static final ScarecrowSettings FALLBACK =
            new ScarecrowSettings(100f, true, 1, 0f, 0f, 0f, false, false, 4f, 2f, 3f);

    /** セーブデータ用。項目が足りない古いデータでも読めるよう、すべて既定値付きにする。 */
    public static final Codec<ScarecrowSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("max_health", FALLBACK.maxHealth).forGetter(ScarecrowSettings::maxHealth),
            Codec.BOOL.optionalFieldOf("infinite_health", FALLBACK.infiniteHealth).forGetter(ScarecrowSettings::infiniteHealth),
            Codec.INT.optionalFieldOf("defense", FALLBACK.defense).forGetter(ScarecrowSettings::defense),
            Codec.FLOAT.optionalFieldOf("armor", FALLBACK.armor).forGetter(ScarecrowSettings::armor),
            Codec.FLOAT.optionalFieldOf("magic_resistance", FALLBACK.magicResistance).forGetter(ScarecrowSettings::magicResistance),
            Codec.FLOAT.optionalFieldOf("knockback_resistance", FALLBACK.knockbackResistance).forGetter(ScarecrowSettings::knockbackResistance),
            Codec.BOOL.optionalFieldOf("no_knockback", FALLBACK.noKnockback).forGetter(ScarecrowSettings::noKnockback),
            Codec.BOOL.optionalFieldOf("counter_enabled", FALLBACK.counterEnabled).forGetter(ScarecrowSettings::counterEnabled),
            Codec.FLOAT.optionalFieldOf("counter_damage", FALLBACK.counterDamage).forGetter(ScarecrowSettings::counterDamage),
            Codec.FLOAT.optionalFieldOf("counter_interval", FALLBACK.counterInterval).forGetter(ScarecrowSettings::counterInterval),
            Codec.FLOAT.optionalFieldOf("counter_range", FALLBACK.counterRange).forGetter(ScarecrowSettings::counterRange)
    ).apply(i, ScarecrowSettings::new));

    /** 通信用（項目が多いので手書き。書く順と読む順を必ず揃えること）。 */
    public static final StreamCodec<ByteBuf, ScarecrowSettings> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> {
                buf.writeFloat(s.maxHealth);
                buf.writeBoolean(s.infiniteHealth);
                buf.writeInt(s.defense);
                buf.writeFloat(s.armor);
                buf.writeFloat(s.magicResistance);
                buf.writeFloat(s.knockbackResistance);
                buf.writeBoolean(s.noKnockback);
                buf.writeBoolean(s.counterEnabled);
                buf.writeFloat(s.counterDamage);
                buf.writeFloat(s.counterInterval);
                buf.writeFloat(s.counterRange);
            },
            buf -> new ScarecrowSettings(
                    buf.readFloat(), buf.readBoolean(), buf.readInt(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat()));

    /** 召喚直後の初期値（Config の [scarecrow]）。 */
    public static ScarecrowSettings fromConfig() {
        return new ScarecrowSettings(
                ModConfigs.get(ModConfigs.SCARECROW_MAX_HEALTH).floatValue(),
                ModConfigs.get(ModConfigs.SCARECROW_INFINITE_HEALTH),
                ModConfigs.get(ModConfigs.SCARECROW_DEFENSE),
                ModConfigs.get(ModConfigs.SCARECROW_ARMOR).floatValue(),
                ModConfigs.get(ModConfigs.SCARECROW_MAGIC_RESISTANCE).floatValue(),
                ModConfigs.get(ModConfigs.SCARECROW_KNOCKBACK_RESISTANCE).floatValue(),
                ModConfigs.get(ModConfigs.SCARECROW_NO_KNOCKBACK),
                ModConfigs.get(ModConfigs.SCARECROW_COUNTER_ENABLED),
                ModConfigs.get(ModConfigs.SCARECROW_COUNTER_DAMAGE).floatValue(),
                ModConfigs.get(ModConfigs.SCARECROW_COUNTER_INTERVAL).floatValue(),
                ModConfigs.get(ModConfigs.SCARECROW_COUNTER_RANGE).floatValue()).sanitized();
    }

    /** 各値を入力可能な範囲に丸めたコピーを返す（NaN は下限扱い）。 */
    public ScarecrowSettings sanitized() {
        return new ScarecrowSettings(
                clamp(maxHealth, ModConstants.SCARECROW_MIN_HEALTH, ModConstants.SCARECROW_MAX_HEALTH_LIMIT),
                infiniteHealth,
                Mth.clamp(defense, 0, ModConstants.SCARECROW_DEFENSE_LIMIT),
                clamp(armor, 0, ModConstants.SCARECROW_ARMOR_LIMIT),
                clamp(magicResistance, 0, 100),
                clamp(knockbackResistance, 0, 100),
                noKnockback,
                counterEnabled,
                clamp(counterDamage, 0, ModConstants.SCARECROW_COUNTER_DAMAGE_LIMIT),
                clamp(counterInterval, ModConstants.SCARECROW_COUNTER_INTERVAL_MIN, ModConstants.SCARECROW_COUNTER_INTERVAL_MAX),
                clamp(counterRange, ModConstants.SCARECROW_COUNTER_RANGE_MIN, ModConstants.SCARECROW_COUNTER_RANGE_MAX));
    }

    /** 反撃の間隔（tick）。 */
    public int counterIntervalTicks() {
        return Math.max(1, Math.round(counterInterval * 20f));
    }

    private static float clamp(float value, double min, double max) {
        if (Float.isNaN(value)) {
            return (float) min;
        }
        return (float) Math.clamp(value, min, max);
    }
}
