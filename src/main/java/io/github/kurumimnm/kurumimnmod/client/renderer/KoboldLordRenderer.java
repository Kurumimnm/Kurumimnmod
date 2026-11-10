package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.KoboldLordEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * コボルトロードのレンダラー（GeckoLib）。
 * <ul>
 *   <li>二刀流の前は腰のなた（machete_hip）、二刀流になったら左手のなた（machete_left）を表示する</li>
 *   <li>スキルの予兆中だけ頭の上の「！」を表示する（{@link TelegraphBones}）</li>
 *   <li>head ボーンを見ている方向へ向ける</li>
 * </ul>
 * 着地点の範囲（地面の赤い円）は {@link io.github.kurumimnm.kurumimnmod.client.BossAoeRenderer} が描く。
 */
public class KoboldLordRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<KoboldLordEntity, R> {

    private static final DataTicket<Boolean> DUAL = DataTicket.create("kurumimnmod_kobold_lord_dual", Boolean.class);
    private static final String HEAD_BONE = "head";
    private static final String HIP_MACHETE = "machete_hip";
    private static final String LEFT_MACHETE = "machete_left";

    public KoboldLordRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.KOBOLD_LORD.get());
        // 影も巨体に合わせて大きく（大きさの倍率は属性 scale。モデルの拡大は GeckoLib が行う）
        this.shadowRadius = (float) (ModConstants.KOBOLD_LORD_SHADOW_RADIUS * ModConstants.KOBOLD_LORD_SCALE);
    }

    @Override
    public void extractRenderState(KoboldLordEntity entity, R state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        TelegraphBones.extract(entity, state);
        state.addGeckolibData(DUAL, entity.isDualWielding());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        TelegraphBones.apply(renderPassInfo, snapshots);
        boolean dual = Boolean.TRUE.equals(renderPassInfo.getGeckolibData(DUAL));
        snapshots.ifPresent(dual ? HIP_MACHETE : LEFT_MACHETE, s -> s.skipRender(true).skipChildrenRender(true));
        if (renderPassInfo.renderState() instanceof LivingEntityRenderState living) {
            float yaw = Mth.clamp(living.yRot, -ModConstants.KOBOLD_LORD_HEAD_MAX_YAW, ModConstants.KOBOLD_LORD_HEAD_MAX_YAW);
            float pitch = Mth.clamp(living.xRot, -ModConstants.KOBOLD_LORD_HEAD_MAX_PITCH, ModConstants.KOBOLD_LORD_HEAD_MAX_PITCH);
            snapshots.ifPresent(HEAD_BONE, snapshot -> snapshot
                    .setRotY(snapshot.getRotY() - yaw * Mth.DEG_TO_RAD)
                    .setRotX(snapshot.getRotX() - pitch * Mth.DEG_TO_RAD));
        }
    }
}
