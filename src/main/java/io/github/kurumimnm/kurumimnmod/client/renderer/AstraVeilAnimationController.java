package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.renderer.base.BoneSnapshots;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.entity.AstraVeilMotion;
import java.io.Reader;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;

/**
 * アストラ・ヴェイルのアニメーション制御（クライアント側）。
 * <p>
 * 土台の待機アニメ（astra_veil.animation.json。GeckoLib が再生）の上に、動きに合わせた回転をボーンごとに足す。
 * <ul>
 *   <li>たなびき：触腕の節を「引きずる向き」（進む向きの反対）へ曲げる。根元ほど大きく曲がり、長い触腕の先ほど遅れて追従する</li>
 *   <li>移動の波：移動の強さに比例して、根元から先端へ伝わる波を足す（長い触腕は大きくゆっくり、短い触腕は細かく）</li>
 *   <li>渦のほどけ：移動の強さに比例して、先端の渦巻きを伸ばす（後ろへ長く流れる）</li>
 *   <li>胴は頭より遅れて向きを変え、頭は進む向きを先に向く</li>
 * </ul>
 * どの節をどれだけ曲げるかの係数（リグ）は、モデルと一緒に tools/astra_veil/generate.py が作る
 * （assets/kurumimnmod/astra_veil/rig.json。プレビューの姿勢と同じ式）。
 * 値はすべて速さ・向きから連続的に決まるので、静止 ⇔ 移動の切り替えは自然につながる。
 * <p>
 * 【拡張】攻撃アニメを足すときは、GeckoLib 側のアニメ（triggerAnim）で触腕の根元のボーンを動かせば、ここで足す動きと重なる。
 */
public final class AstraVeilAnimationController {

    private AstraVeilAnimationController() {}

    private static final Identifier RIG_LOCATION = Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "astra_veil/rig.json");
    /** 胴全体・頭のボーン（モデルと一致させること）。 */
    private static final String TORSO_BONE = "torso";
    private static final String HEAD_BONE = "head";

    /** リグ：ボーンごとの係数。 */
    private static final class Rig {
        String[] names = new String[0];
        /** 3×3 行列（行優先）：回転の増分（ラジアン）= a × 引きずる向き。 */
        float[][] gain = new float[0][];
        /** 追従の遅れ（0 = 速い 〜 2 = 遅い）。 */
        float[] lag = new float[0];
        /** 移動の波：振幅（ラジアン）・速さ（Hz）・位相（ラジアン）。 */
        float[][] flutter = new float[0][];
        /** 渦のほどけ（ラジアン。移動の強さ 1 のとき）。 */
        float[] uncurl = new float[0];
    }

    private static Rig rig;

    /** リグを読み込む（最初の描画のとき 1 回だけ。読めなかったときは空のリグ = 待機アニメだけになる）。 */
    private static Rig rig() {
        if (rig != null) {
            return rig;
        }
        Rig loaded = new Rig();
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(RIG_LOCATION);
            if (resource.isPresent()) {
                try (Reader reader = resource.get().openAsReader()) {
                    JsonArray bones = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("bones");
                    int n = bones.size();
                    loaded.names = new String[n];
                    loaded.gain = new float[n][9];
                    loaded.lag = new float[n];
                    loaded.flutter = new float[n][3];
                    loaded.uncurl = new float[n];
                    for (int i = 0; i < n; i++) {
                        JsonObject bone = bones.get(i).getAsJsonObject();
                        loaded.names[i] = bone.get("n").getAsString();
                        JsonArray a = bone.getAsJsonArray("a");
                        for (int k = 0; k < 9; k++) {
                            loaded.gain[i][k] = a.get(k).getAsFloat();
                        }
                        loaded.lag[i] = bone.get("l").getAsFloat();
                        JsonArray f = bone.getAsJsonArray("f");
                        for (int k = 0; k < 3; k++) {
                            loaded.flutter[i][k] = f.get(k).getAsFloat();
                        }
                        JsonElement u = bone.get("u");
                        loaded.uncurl[i] = u == null ? 0f : u.getAsFloat();
                    }
                }
            } else {
                Kurumimnmod.LOGGER.warn("Astra Veil rig not found: {}", RIG_LOCATION);
            }
        } catch (Exception e) {
            Kurumimnmod.LOGGER.error("Failed to load Astra Veil rig: {}", RIG_LOCATION, e);
            loaded = new Rig();
        }
        rig = loaded;
        return loaded;
    }

    /** ボーンへ動きを足す（AstraVeilRenderer#adjustModelBonesForRender から呼ぶ）。回転は描画空間のラジアン。 */
    public static void apply(AstraVeilMotion.Pose pose, BoneSnapshots snapshots) {
        Rig r = rig();
        float[] drag = pose.drag();
        float move = pose.move();
        float time = pose.time();
        int last = AstraVeilMotion.LAG_CLASSES - 1;
        for (int i = 0; i < r.names.length; i++) {
            // ---- 引きずる向き：遅れに合わせて、追従の速さが違う 2 つの値を混ぜる ----
            float lag = Mth.clamp(r.lag[i], 0f, last);
            int c0 = Math.min((int) lag, last - 1);
            float t = lag - c0;
            float gx = Mth.lerp(t, drag[c0 * 3], drag[c0 * 3 + 3]);
            float gy = Mth.lerp(t, drag[c0 * 3 + 1], drag[c0 * 3 + 4]);
            float gz = Mth.lerp(t, drag[c0 * 3 + 2], drag[c0 * 3 + 5]);
            float[] a = r.gain[i];
            float dx = a[0] * gx + a[1] * gy + a[2] * gz;
            float dy = a[3] * gx + a[4] * gy + a[5] * gz;
            float dz = a[6] * gx + a[7] * gy + a[8] * gz;
            // ---- 移動の波（根元から先端へ伝わる）と、渦のほどけ ----
            if (move > 0.001f) {
                float[] f = r.flutter[i];
                float phase = Mth.TWO_PI * f[1] * time + f[2];
                dz += move * (f[0] * Mth.sin(phase) + r.uncurl[i]);
                dy += move * f[0] * 0.6f * Mth.sin(phase + 1.3f);
            }
            if (dx == 0f && dy == 0f && dz == 0f) {
                continue;
            }
            Optional<BoneSnapshot> snapshot = snapshots.get(r.names[i]);
            if (snapshot.isPresent()) {
                BoneSnapshot s = snapshot.get();
                s.setRotX(s.getRotX() + dx).setRotY(s.getRotY() + dy).setRotZ(s.getRotZ() + dz);
            }
        }
        // ---- 胴は頭より遅れて向きを変える（触腕の付け根も胴と一緒に遅れる）／頭は進む向きを先に向く ----
        // モデルを Y 軸まわりに +θ 回すと、エンティティの向き（yaw）が θ 小さい姿勢になる
        float bodyYaw = pose.bodyYaw() * Mth.DEG_TO_RAD;
        if (bodyYaw != 0f) {
            snapshots.ifPresent(TORSO_BONE, s -> s.setRotY(s.getRotY() + bodyYaw));
        }
        float headYaw = -pose.headYaw() * Mth.DEG_TO_RAD;
        if (headYaw != 0f) {
            snapshots.ifPresent(HEAD_BONE, s -> s.setRotY(s.getRotY() + headYaw));
        }
    }
}
