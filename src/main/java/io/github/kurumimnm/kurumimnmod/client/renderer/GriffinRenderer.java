package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.GriffinEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * グリフォンのレンダラー（GeckoLib）。
 * <ul>
 *   <li>鎧なしの個体は armor_* ボーン（古い鉄の鎧）を描かない</li>
 *   <li>head ボーンを見ている方向へ向ける（アニメーションの回転に足す）</li>
 * </ul>
 * モデル・アニメーション・テクスチャは登録名 "griffin" から自動で決まる
 * （geckolib/models/entity/griffin.geo.json など）。
 * <p>
 * R は「EntityRenderState かつ GeoRenderState」（ScarecrowRenderer と同じ書き方）。
 */
public class GriffinRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<GriffinEntity, R> {

    /** RenderState に書き込む「鎧を着ているか」。 */
    private static final DataTicket<Boolean> ARMORED = DataTicket.create("kurumimnmod_griffin_armored", Boolean.class);

    /** 鎧のボーン（モデルの armor_* と一致させること）。 */
    private static final List<String> ARMOR_BONES = List.of("armor_body", "armor_neck", "armor_head");
    private static final String HEAD_BONE = "head";

    public GriffinRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.GRIFFIN.get());
        this.shadowRadius = ModConstants.GRIFFIN_SHADOW_RADIUS;
    }

    @Override
    public void extractRenderState(GriffinEntity entity, R state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.addGeckolibData(ARMORED, entity.isArmored());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        // ---- 鎧の表示切り替え ----
        if (!Boolean.TRUE.equals(renderPassInfo.getGeckolibData(ARMORED))) {
            for (String bone : ARMOR_BONES) {
                snapshots.ifPresent(bone, snapshot -> snapshot.skipRender(true).skipChildrenRender(true));
            }
        }
        // ---- 頭を見ている方向へ向ける ----
        // GeckoLib のボーン回転はバニラの向きと符号が逆（X・Y）なので、マイナスして足す
        if (renderPassInfo.renderState() instanceof LivingEntityRenderState living) {
            float yaw = Mth.clamp(living.yRot, -ModConstants.GRIFFIN_HEAD_MAX_YAW, ModConstants.GRIFFIN_HEAD_MAX_YAW);
            float pitch = Mth.clamp(living.xRot, -ModConstants.GRIFFIN_HEAD_MAX_PITCH, ModConstants.GRIFFIN_HEAD_MAX_PITCH);
            snapshots.ifPresent(HEAD_BONE, snapshot -> snapshot
                    .setRotY(snapshot.getRotY() - yaw * Mth.DEG_TO_RAD)
                    .setRotX(snapshot.getRotX() - pitch * Mth.DEG_TO_RAD));
        }
    }
}
