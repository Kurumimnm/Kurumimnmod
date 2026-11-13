package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * アストラ・ヴェイル（星幽の巡界者）。フィールドをめぐる超大型のボス。
 * <p>
 * 【見た目】設定画（星幽の巡界者のデザインシート）に合わせた形（キューブ 900 個以上）。
 *   <ul>
 *     <li>全体：水平に大きく広がる布のような膜。地上から見上げると、膜の裏面（ひれの筋・穴）が空を覆う。
 *         横から見ると前後に長く、帯と触腕が後ろへたなびく</li>
 *     <li>翼：肩から扇形に放射する「ひれ」を並べた 2 層の膜（上 12 本・下 8 本ずつ）。波打ち、前の先は巻き上がり、後ろは垂れる。
 *         頭の左右からも膜が立ち上がって外へ巻き下りる</li>
 *     <li>顔：細長い V 字の銀の仮面（2 本の柱が下で合わさる）、光る稜線、触角、真珠の粒の房、花びらのような襟</li>
 *     <li>胴：穴だらけの骨のような格子が縦に垂れるカーテン。下で大きな葉が外へ巻く</li>
 *     <li>後ろへたなびく帯 10 本、翼のふちから渦を巻く帯（先に玉）、まわりに漂う欠片 28 個</li>
 *   </ul>
 *   モデル・テクスチャ・アニメは tools/astra_veil/generate.py で生成。
 * 【大きさ】モデルは小さく作り、レンダラーで {@link ModConstants#ASTRA_VEIL_RENDER_SCALE} 倍にする
 *   （8 倍で 翼幅 約 125・帯の先まで 約 155・高さ 約 75・前後 約 95 ブロック）。
 * 【当たり判定】顔のまわりだけ（{@link ModConstants#ASTRA_VEIL_WIDTH} × {@link ModConstants#ASTRA_VEIL_HEIGHT}）。
 *   エンティティの位置 = 顔の下端。幕・触腕はエンティティの位置より下・横に大きくはみ出す。
 * 【今の状態：見た目のみ】
 *   <ul>
 *     <li>重力・当たり判定による押し合いなし。その場に浮かんで待機アニメ（8 秒ループ）を続ける</li>
 *     <li>AI・攻撃・ボスバー・遭遇（フィールドの屋外にランダム出現、3 分で帰る）・空の演出は、攻撃の内容が決まってから作る</li>
 *     <li>スポーンエッグ・/summon のみ。置いた場所から {@link ModConstants#ASTRA_VEIL_SPAWN_HOVER_HEIGHT} ブロック上に浮かぶ</li>
 *   </ul>
 */
public class AstraVeilEntity extends Monster implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.astra_veil.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AstraVeilEntity(EntityType<? extends AstraVeilEntity> type, Level level) {
        super(type, level);
        // 空に浮かぶ巨体：重力なし・ブロックをすり抜ける
        setNoGravity(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.ASTRA_VEIL_MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, ModConstants.ASTRA_VEIL_KNOCKBACK_RESISTANCE)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    // =====================================================================
    // 動き（今は浮かんでいるだけ）
    // =====================================================================

    @Override
    protected void registerGoals() {
        // 見た目のみの段階なので AI なし（攻撃の内容が決まったら追加する）
    }

    @Override
    public void tick() {
        super.tick();
        // 押されたり流されたりしないように、毎 tick 速度を 0 にする
        setDeltaMovement(Vec3.ZERO);
    }

    /** スポーンエッグ・/summon で出したときは、触腕の先が地面に届くくらいの高さへ浮かべる。 */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        setPos(getX(), getY() + ModConstants.ASTRA_VEIL_SPAWN_HOVER_HEIGHT, getZ());
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    // =====================================================================
    // 描画の範囲（巨体なので、遠く・画面の端でも消えないようにする）
    // =====================================================================

    /** 描画する距離。バニラは当たり判定の大きさから決めるので、見た目の大きさに合わせて広げる。 */
    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double range = ModConstants.ASTRA_VEIL_RENDER_DISTANCE;
        return distanceSqr < range * range;
    }

    /**
     * 見た目全体を囲む範囲。画面外判定（視錐台カリング）に使う（{@link io.github.kurumimnm.kurumimnmod.client.renderer.AstraVeilRenderer}）。
     * 当たり判定（顔のまわり）だけで判定すると、顔が画面外に出たとたんに巨体全体が消えてしまうため。
     */
    public static AABB cullingBox(Entity entity) {
        double half = ModConstants.ASTRA_VEIL_CULL_HALF_WIDTH;
        return new AABB(entity.getX() - half, entity.getY() - ModConstants.ASTRA_VEIL_CULL_BELOW, entity.getZ() - half,
                entity.getX() + half, entity.getY() + ModConstants.ASTRA_VEIL_CULL_ABOVE, entity.getZ() + half);
    }

    // =====================================================================
    // バニラの挙動の無効化（消えない・押されない など）
    // =====================================================================

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // 他のエンティティに押されても動かない
    }

    @Override
    protected void doPush(Entity entity) {
        // 他のエンティティを押さない
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        // 撃破報酬は後で決める
        return false;
    }

    // =====================================================================
    // GeckoLib
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 待機：幕がゆっくり波打ち、帯と触腕がたなびく（8 秒ループ）
        controllers.add(new AnimationController<>("main", test -> test.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
