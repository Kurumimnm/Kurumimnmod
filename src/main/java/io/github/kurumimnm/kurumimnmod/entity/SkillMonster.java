package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * スキルを使う敵モブの共通部分（ガロウ・コボルト・コボルトロード）。
 * <ul>
 *   <li><b>予兆</b>：スキルの前に頭の上に「！」を出す。{@link #setTelegraphing} でオンにすると
 *       クライアントへ同期され、レンダラー（{@link io.github.kurumimnm.kurumimnmod.client.renderer.TelegraphBones}）が
 *       モデルの {@code alert} ボーンを表示する</li>
 *   <li><b>クールダウン</b>：{@link #skillReady} / {@link #startSkillCooldown}（ワールドの時刻で管理。保存しない）</li>
 * </ul>
 */
public abstract class SkillMonster extends Monster implements GeoEntity {

    /** 予兆中か（頭の上の「！」。クライアントへ同期）。 */
    private static final EntityDataAccessor<Boolean> DATA_TELEGRAPH =
            SynchedEntityData.defineId(SkillMonster.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 次にスキルを使えるワールドの時刻（tick）。 */
    private long skillReadyAt;

    protected SkillMonster(EntityType<? extends SkillMonster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TELEGRAPH, false);
    }

    public boolean isTelegraphing() {
        return this.entityData.get(DATA_TELEGRAPH);
    }

    public void setTelegraphing(boolean value) {
        this.entityData.set(DATA_TELEGRAPH, value);
    }

    /** スキルのクールダウンが終わっているか。 */
    public boolean skillReady() {
        return level().getGameTime() >= skillReadyAt;
    }

    /** クールダウンを始める（ticks 後にまた使える）。 */
    public void startSkillCooldown(int ticks) {
        skillReadyAt = level().getGameTime() + ticks;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
