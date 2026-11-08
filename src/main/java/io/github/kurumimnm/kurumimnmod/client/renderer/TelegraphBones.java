package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import io.github.kurumimnm.kurumimnmod.entity.SkillMonster;

/**
 * スキルの予兆（頭の上の「！」）の表示。
 * <p>
 * 「！」はモデルの {@code alert} ボーン（geo.json の中のブロック）として作ってあり、
 * 予兆中でなければ描かない。文字ではなくモデルの一部なので、どの向きからも見え、遠くでも形が崩れない。
 * 使い方（各レンダラー）：
 * <pre>{@code
 * extractRenderState: TelegraphBones.extract(entity, state);
 * adjustModelBonesForRender: TelegraphBones.apply(renderPassInfo, snapshots);
 * }</pre>
 */
public final class TelegraphBones {

    private TelegraphBones() {}

    /** モデルの「！」のボーン名（geo.json と一致させる）。 */
    public static final String ALERT_BONE = "alert";

    private static final DataTicket<Boolean> TELEGRAPH = DataTicket.create("kurumimnmod_telegraph", Boolean.class);

    public static void extract(SkillMonster entity, GeoRenderState state) {
        state.addGeckolibData(TELEGRAPH, entity.isTelegraphing());
    }

    public static <R extends GeoRenderState> void apply(RenderPassInfo<R> info, BoneSnapshots snapshots) {
        if (!Boolean.TRUE.equals(info.getGeckolibData(TELEGRAPH))) {
            snapshots.ifPresent(ALERT_BONE, s -> s.skipRender(true).skipChildrenRender(true));
        }
    }
}
