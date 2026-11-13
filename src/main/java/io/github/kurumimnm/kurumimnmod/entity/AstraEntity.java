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
 * アストラ（星幽の巡界者）。フィールドをめぐる超大型のボス。アストラ・ヴェイルとは別のモデル。
 * <p>
 * 【見た目】設定画に合わせた形（キューブ 約 2250 個、テクスチャ 2048×768、1 ピクセル = 1/2 ブロック）。
 *   <ul>
 *     <li>腕／羽 4 本：上の 2 枚は外上へ、下の 2 枚は外下・前へ伸びる巨大な膜（1 枚が 100 ブロック四方を覆う大きさ）。
 *         傘の骨のような筋・楕円の穴・真珠色のふちを、扇全体でつながる 1 枚の絵として描いている。下の 2 本の先は鎌のような爪</li>
 *     <li>頭：前へ約 45 ブロック突き出すくさび形の立体（上から見ると三角形。後ろの幅 約 30 ブロック）。体と同じ紫〜藤色の肌、
 *         左右 2 つずつ計 4 つの光る目とつり上がった眉、牙の並ぶ口、後ろへ流れる角、背のとげ。少しうつむいて地上を見下ろす。
 *         下あご（ボーン jaw）は後ろを軸に下へ開く（口を開くアニメを後から付けられる）</li>
 *     <li>胴：顔の後ろから下へ垂れる穴だらけの格子の柱、襟の葉、カーテン、触腕</li>
 *     <li>尾：後ろへ長くたなびく膜と帯（横から見ると前後に長い）。まわりに漂う欠片と星の粒</li>
 *   </ul>
 *   モデル・テクスチャ・アニメは tools/astra/generate.py で生成。
 * 【大きさ】モデルは小さく作り、レンダラーで {@link ModConstants#ASTRA_RENDER_SCALE} 倍にする
 *   （8 倍で 全幅 約 500・高さ 約 360・前後 約 430 ブロック）。
 * 【当たり判定】頭の付け根と胴の上部を囲む箱（仮。{@link ModConstants#ASTRA_WIDTH} × {@link ModConstants#ASTRA_HEIGHT}）。
 *   エンティティの位置 = 胴の中心の真下。頭・膜・触腕は箱から大きくはみ出す。
 * 【今の状態：見た目のみ】
 *   <ul>
 *     <li>重力・押し合いなし。その場に浮かんで待機アニメ（16 秒ループ）を続ける</li>
 *     <li>ステータス・スキル・攻撃アニメ・遭遇（地上のフィールドにランダム出現）・空の演出は後で追加する</li>
 *     <li>スポーンエッグ・/summon のみ。置いた場所から {@link ModConstants#ASTRA_SPAWN_HOVER_HEIGHT} ブロック上に浮かぶ</li>
 *   </ul>
 */
public class AstraEntity extends Monster implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.astra.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AstraEntity(EntityType<? extends AstraEntity> type, Level level) {
        super(type, level);
        // 空に浮かぶ巨体：重力なし・ブロックをすり抜ける
        setNoGravity(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.ASTRA_MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, ModConstants.ASTRA_KNOCKBACK_RESISTANCE)
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
        setPos(getX(), getY() + ModConstants.ASTRA_SPAWN_HOVER_HEIGHT, getZ());
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    // =====================================================================
    // 描画の範囲（巨体なので、遠く・画面の端でも消えないようにする）
    // =====================================================================

    /** 描画する距離。バニラは当たり判定の大きさから決めるので、見た目の大きさに合わせて広げる。 */
    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double range = ModConstants.ASTRA_RENDER_DISTANCE;
        return distanceSqr < range * range;
    }

    /**
     * 見た目全体を囲む範囲。画面外判定（視錐台カリング）に使う（{@link io.github.kurumimnm.kurumimnmod.client.renderer.AstraRenderer}）。
     * 当たり判定（顔）だけで判定すると、顔が画面外に出たとたんに巨体全体が消えてしまうため。
     */
    public static AABB cullingBox(Entity entity) {
        double half = ModConstants.ASTRA_CULL_HALF_WIDTH;
        return new AABB(entity.getX() - half, entity.getY() - ModConstants.ASTRA_CULL_BELOW, entity.getZ() - half,
                entity.getX() + half, entity.getY() + ModConstants.ASTRA_CULL_ABOVE, entity.getZ() + half);
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
        // 待機：膜がゆっくり波打ち、帯と触腕がたなびき、下あごがわずかに開閉する（16 秒ループ）
        controllers.add(new AnimationController<>("main", test -> test.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
