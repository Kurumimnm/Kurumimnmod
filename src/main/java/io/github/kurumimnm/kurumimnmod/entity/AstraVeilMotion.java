package io.github.kurumimnm.kurumimnmod.entity;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.util.Mth;

/**
 * アストラ・ヴェイルの「見た目の動き」の状態（クライアント側で毎 tick 更新する）。
 * <p>
 * エンティティの位置・向きの変化から、次の値を求めてなめらかに追従させる。
 * 値はすべて連続的に変わるので、静止 ⇔ 移動の切り替えで姿勢が急に変わることはない。
 * <ul>
 *   <li>drag … 触腕を引きずる向き（モデルの座標。前が -Z、後ろが +Z）。進む向きの反対へ流れる。
 *       追従の速さが違う 3 種類を持つ（短い触腕は速く、長い触腕の先は遅れて追従する）</li>
 *   <li>move … 移動の強さ（0 = 静止、1 = 巡航）。移動のときに加わる波・渦のほどけ具合に使う</li>
 *   <li>bodyYaw … 胴が頭より遅れて向きを変えるための角度のずれ</li>
 *   <li>headYaw … 頭が進む向きを先に向く角度</li>
 *   <li>pitch / bank … 上昇・下降・旋回のときの全身の傾き</li>
 * </ul>
 * 描画のときは {@link #pose} で partialTick 補間した値を取り出す。
 * 将来、攻撃などで姿勢を変えるときは、ここに値を足す（例：突進中は drag を強める）。
 */
public final class AstraVeilMotion {

    /** drag の種類の数（0 = 速い、1 = 中、2 = 遅い）。 */
    public static final int LAG_CLASSES = 3;

    /** 描画 1 回ぶんの姿勢（partialTick 補間済み）。レンダラーが RenderState へ入れて使う。 */
    public record Pose(float yaw, float pitch, float bank, float bodyYaw, float headYaw, float move, float time, float[] drag) {}

    private final float[] drag = new float[LAG_CLASSES * 3];
    private final float[] dragO = new float[LAG_CLASSES * 3];
    private float move, moveO;
    private float bodyYaw, bodyYawO;
    private float headYaw, headYawO;
    private float pitch, pitchO;
    private float bank, bankO;
    private float yaw, yawO;

    private boolean initialized;
    private double lastX, lastY, lastZ;
    private float laggedYaw;
    private float smoothedTurn;

    /** 毎 tick（クライアント側）呼ぶ。x・y・z・yaw はエンティティの現在の位置と向き。 */
    public void tick(double x, double y, double z, float entityYaw) {
        System.arraycopy(this.drag, 0, this.dragO, 0, this.drag.length);
        this.moveO = this.move; this.bodyYawO = this.bodyYaw; this.headYawO = this.headYaw;
        this.pitchO = this.pitch; this.bankO = this.bank; this.yawO = this.yaw;

        if (!this.initialized) {
            this.initialized = true;
            this.lastX = x; this.lastY = y; this.lastZ = z;
            this.yaw = this.yawO = entityYaw;
            this.laggedYaw = entityYaw;
        }
        double vx = x - this.lastX, vy = y - this.lastY, vz = z - this.lastZ;
        float turn = Mth.wrapDegrees(entityYaw - this.yaw);
        this.lastX = x; this.lastY = y; this.lastZ = z;
        if (vx * vx + vy * vy + vz * vz > 16.0) {
            // テレポートなどで大きく動いたときは、速さとして扱わない
            vx = vy = vz = 0;
            turn = 0;
            this.laggedYaw = entityYaw;
        }
        this.yaw = entityYaw;
        this.smoothedTurn += (turn - this.smoothedTurn) * 0.15f;

        // ---- 速度をモデルの座標へ直す（モデルの +X・-Z（前）が、ワールドのどの向きか） ----
        float rad = entityYaw * Mth.DEG_TO_RAD;
        float sin = Mth.sin(rad), cos = Mth.cos(rad);
        double full = ModConstants.ASTRA_VEIL_ANIM_FULL_SPEED;
        float mx = (float) ((-vx * cos - vz * sin) / full);
        float my = (float) (vy / full);
        float mz = (float) ((vx * sin - vz * cos) / full);

        // ---- 触腕を引きずる向き：速度の反対。旋回中は外側へ振られる ----
        float tx = -mx - this.smoothedTurn * ModConstants.ASTRA_VEIL_ANIM_TURN_SWING;
        float ty = -my;
        float tz = -mz;
        float len = Mth.sqrt(tx * tx + ty * ty + tz * tz);
        if (len > 1.25f) {
            float k = 1.25f / len;
            tx *= k; ty *= k; tz *= k;
        }
        float[] lag = {ModConstants.ASTRA_VEIL_ANIM_LAG_FAST, ModConstants.ASTRA_VEIL_ANIM_LAG_MID, ModConstants.ASTRA_VEIL_ANIM_LAG_SLOW};
        for (int c = 0; c < LAG_CLASSES; c++) {
            float k = 1.0f / Math.max(1.0f, lag[c]);
            this.drag[c * 3] += (tx - this.drag[c * 3]) * k;
            this.drag[c * 3 + 1] += (ty - this.drag[c * 3 + 1]) * k;
            this.drag[c * 3 + 2] += (tz - this.drag[c * 3 + 2]) * k;
        }

        // ---- 移動の強さ（静止 ⇔ 移動のなめらかな切り替え） ----
        float speed = Mth.clamp(Mth.sqrt(mx * mx + my * my + mz * mz), 0f, 1f);
        this.move += (speed - this.move) / Math.max(1.0f, ModConstants.ASTRA_VEIL_ANIM_BLEND_TICKS);

        // ---- 胴は頭より遅れて向きを変える ----
        this.laggedYaw += Mth.wrapDegrees(entityYaw - this.laggedYaw) / Math.max(1.0f, ModConstants.ASTRA_VEIL_ANIM_BODY_LAG_TICKS);
        float maxLag = ModConstants.ASTRA_VEIL_ANIM_BODY_LAG_MAX_DEGREES;
        this.bodyYaw = Mth.clamp(Mth.wrapDegrees(entityYaw - this.laggedYaw), -maxLag, maxLag);

        // ---- 頭は進む向きを先に向く ----
        float maxLead = ModConstants.ASTRA_VEIL_ANIM_HEAD_LEAD_MAX_DEGREES;
        float lead = Mth.clamp(this.smoothedTurn * ModConstants.ASTRA_VEIL_ANIM_HEAD_LEAD, -maxLead, maxLead);
        this.headYaw += (lead - this.headYaw) * 0.08f;

        // ---- 全身の傾き：上昇・下降で前後に、旋回で内側に ----
        float pitchTarget = Mth.clamp(my * 1.6f, -1f, 1f) * ModConstants.ASTRA_VEIL_ANIM_MAX_PITCH_DEGREES;
        this.pitch += (pitchTarget - this.pitch) * 0.04f;
        float bankTarget = Mth.clamp(this.smoothedTurn / Math.max(0.01f, ModConstants.ASTRA_VEIL_MAX_TURN_RATE), -1f, 1f)
                * ModConstants.ASTRA_VEIL_ANIM_MAX_BANK_DEGREES;
        this.bank += (bankTarget - this.bank) * 0.04f;
    }

    /** 移動の強さ（0 = 静止、1 = 巡航）。 */
    public float moveAmount() {
        return this.move;
    }

    /** 描画用に補間した姿勢。time は秒（エンティティの生存時間）。 */
    public Pose pose(float partialTick, float timeSeconds) {
        float[] d = new float[this.drag.length];
        for (int i = 0; i < d.length; i++) {
            d[i] = Mth.lerp(partialTick, this.dragO[i], this.drag[i]);
        }
        return new Pose(
                Mth.rotLerp(partialTick, this.yawO, this.yaw),
                Mth.lerp(partialTick, this.pitchO, this.pitch),
                Mth.lerp(partialTick, this.bankO, this.bank),
                Mth.lerp(partialTick, this.bodyYawO, this.bodyYaw),
                Mth.lerp(partialTick, this.headYawO, this.headYaw),
                Mth.lerp(partialTick, this.moveO, this.move),
                timeSeconds, d);
    }
}
