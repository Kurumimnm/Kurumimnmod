package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.AstraVeilEntity;
import io.github.kurumimnm.kurumimnmod.entity.AstraVeilMotion;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * アストラ・ヴェイルの専用レンダラー（GeckoLib）。
 * <ul>
 *   <li>巨大モデル：モデルを {@link ModConstants#ASTRA_VEIL_RENDER_SCALE} 倍に拡大して描く（当たり判定の大きさとは別）</li>
 *   <li>画面外判定：当たり判定ではなく、触腕の先まで含めた見た目全体の範囲で判定する（{@link AstraVeilEntity#cullingBox}）。
 *       描画する距離はエンティティ側で広げてある（{@link AstraVeilEntity#shouldRenderAtSqrDistance}）</li>
 *   <li>半透明：触腕・膜の透け感と、テクスチャの透明な切り抜き。
 *       描く順はモデルのボーンの順（傘・顔 → 胴 → 触腕 → 星屑）で、不透明な部分を先に描く</li>
 *   <li>発光：astra_veil_glowmask.png（発光器官・光の筋・星の粒）を明るさに関係なく光らせる</li>
 *   <li>向き・傾き：エンティティの向きに、上昇・下降・旋回の傾きを足す（{@link AstraVeilMotion}）</li>
 *   <li>動きに合わせたボーンの回転：{@link AstraVeilAnimationController}</li>
 *   <li>影は出さない（空に浮かんでいるため）</li>
 * </ul>
 * R は「EntityRenderState かつ GeoRenderState」（他の GeckoLib レンダラーと同じ書き方）。
 */
public class AstraVeilRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<AstraVeilEntity, R> {

    /** RenderState に書き込む「この描画の姿勢」。 */
    private static final DataTicket<AstraVeilMotion.Pose> POSE =
            DataTicket.create("kurumimnmod_astra_veil_pose", AstraVeilMotion.Pose.class);

    public AstraVeilRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.ASTRA_VEIL.get());
        withScale(ModConstants.ASTRA_VEIL_RENDER_SCALE);
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0f;
    }

    @Override
    public void extractRenderState(AstraVeilEntity entity, R state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.addGeckolibData(POSE, entity.motion().pose(partialTick, (entity.tickCount + partialTick) / 20.0f));
    }

    /** 全身の向き：エンティティの向き（なめらかに補間）＋ 上昇・下降の傾き ＋ 旋回の傾き。 */
    @Override
    protected void applyRotations(RenderPassInfo<R> renderPassInfo, PoseStack poseStack, float nativeScale) {
        AstraVeilMotion.Pose pose = renderPassInfo.getGeckolibData(POSE);
        if (pose == null) {
            super.applyRotations(renderPassInfo, poseStack, nativeScale);
            return;
        }
        // モデルの前は -Z。Y 軸まわりに (180 - yaw) 度回すと、エンティティの向いている方向が前になる。
        // X 軸まわりの + は機首上げ、Z 軸まわりは旋回の内側へ傾ける向き
        poseStack.rotate(new Quaternionf().rotationYXZ(
                (180f - pose.yaw()) * Mth.DEG_TO_RAD, pose.pitch() * Mth.DEG_TO_RAD, -pose.bank() * Mth.DEG_TO_RAD));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        AstraVeilMotion.Pose pose = renderPassInfo.getGeckolibData(POSE);
        if (pose != null) {
            AstraVeilAnimationController.apply(pose, snapshots);
        }
    }

    /** 半透明で描く（背面は描かない。キューブは閉じているので見た目は変わらず、重なりの崩れが減る）。 */
    @Override
    public @Nullable RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityTranslucentCull(texture);
    }

    /** 画面外判定に使う範囲。当たり判定ではなく、触腕の先まで含めた見た目全体で判定する。 */
    @Override
    protected AABB getBoundingBoxForCulling(AstraVeilEntity entity, float partialTick) {
        return AstraVeilEntity.cullingBox(entity);
    }
}
