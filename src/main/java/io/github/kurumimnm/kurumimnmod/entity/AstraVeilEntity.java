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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * アストラ・ヴェイル：空を漂う規格外の超巨大生命体（ボス）。1 体のエンティティとして存在する。
 * <p>
 * 【大きさ】静止時：幅 約 400・高さ 約 280・奥行 約 300 ブロック（細い帯の先まで含む）。
 *   移動時は触腕が後ろへ流れ、渦がほどけて奥行 約 450 ブロックになる。
 *   モデルは 1 ピクセル = 1/2 ブロックで作り、レンダラーで {@link ModConstants#ASTRA_VEIL_RENDER_SCALE} 倍にする。
 * 【構造】（モデル・テクスチャ・待機アニメ・リグは tools/astra_veil/generate.py で生成。部位とボーン名は {@link AstraVeilPart}）
 *   <ul>
 *     <li>頭部：横に大きく広がる傘。前面中央に顔（仮面・くちばし・冠のとげ・琥珀色の発光器官・青い宝玉）</li>
 *     <li>胴体：傘の中央から下へ伸びる首と胴。下へ細る房、内部で光る器官</li>
 *     <li>大型触腕：左右 12・上 6・後ろ 8・前 4・下 5 本と、帯状の長い触腕 10 本</li>
 *     <li>小型触腕：傘のふち・胴のまわりの細い触腕、口腕、触角、あごひげ</li>
 *     <li>発光・装飾：発光器官（不規則に明滅）、光の筋、星雲の模様、まわりに漂う星屑</li>
 *   </ul>
 * 【役割の分担】
 *   <ul>
 *     <li>このクラス：エンティティの基本（属性・保存・バニラの挙動の無効化・描画の範囲）</li>
 *     <li>{@link AstraVeilFlightController}：空中移動（サーバー側。向き・速さを少しずつ変えて漂う）</li>
 *     <li>{@link AstraVeilMotion}：動きに合わせた見た目の状態（クライアント側。触腕のたなびき・傾き・静止 ⇔ 移動の切り替え）</li>
 *     <li>client/renderer/AstraVeilRenderer：描画（拡大・半透明・発光・画面外判定）</li>
 *     <li>client/renderer/AstraVeilAnimationController：ボーンへ動きを当てはめる</li>
 *   </ul>
 * 【今の状態】モデル・空中移動・静止／移動アニメまで。攻撃・戦闘 AI・ダメージ処理・ボスバー・ドロップは未実装
 *   （registerGoals は空。攻撃を作るときは Goal を足し、移動は {@link AstraVeilFlightController#hold}・flyTo で制御する）。
 * 【当たり判定】胴の上半分〜傘の中心だけ（仮）。エンティティの位置 = 胴の中心。
 * 【出現】スポーンエッグ・/summon のみ。置いた場所から {@link ModConstants#ASTRA_VEIL_SPAWN_HOVER_HEIGHT} ブロック上に浮かぶ。
 */
public class AstraVeilEntity extends Monster implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.astra_veil.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** 空中移動（サーバー側）。 */
    private final AstraVeilFlightController flight = new AstraVeilFlightController(this);
    /** 動きに合わせた見た目の状態（クライアント側）。 */
    private final AstraVeilMotion motion = new AstraVeilMotion();

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
    // 動き
    // =====================================================================

    @Override
    protected void registerGoals() {
        // 攻撃・戦闘 AI は未実装（移動は AstraVeilFlightController が行う）
    }

    @Override
    public void tick() {
        super.tick();
        // バニラの物理（押される・流される・摩擦）は使わない。位置は AstraVeilFlightController だけが動かす
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide()) {
            // 見た目の状態を更新（位置・向きの変化から、触腕のたなびき・傾きを求める）
            this.motion.tick(getX(), getY(), getZ(), getYRot());
        } else if (isAlive()) {
            this.flight.tick();
        }
    }

    /** {@link AstraVeilFlightController} が決めた位置・向きを反映する（体・頭の向きも同じにそろえる）。 */
    void applyFlight(double x, double y, double z, float yaw) {
        setPos(x, y, z);
        setYRot(yaw);
        setXRot(0f);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    /** 空中移動の制御（将来の攻撃・演出から、止める・向かわせるのに使う）。 */
    public AstraVeilFlightController flight() {
        return this.flight;
    }

    /** 動きに合わせた見た目の状態（レンダラーが使う）。 */
    public AstraVeilMotion motion() {
        return this.motion;
    }

    /** 移動中か（false = 空中で静止して漂っている）。クライアント側では見た目の状態から、サーバー側では速さから判定する。 */
    public boolean isDrifting() {
        if (level().isClientSide()) {
            return this.motion.moveAmount() > ModConstants.ASTRA_VEIL_ANIM_MOVING_THRESHOLD;
        }
        return this.flight.speed() > ModConstants.ASTRA_VEIL_ANIM_FULL_SPEED * ModConstants.ASTRA_VEIL_ANIM_MOVING_THRESHOLD;
    }

    /** スポーンエッグ・/summon で出したときは、触腕の先が地面の少し上に来る高さへ浮かべる。 */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        setPos(getX(), getY() + ModConstants.ASTRA_VEIL_SPAWN_HOVER_HEIGHT, getZ());
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    // =====================================================================
    // 保存・読み込み（うろつく範囲の中心）
    // =====================================================================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.flight.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.flight.load(input);
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
     * 当たり判定（胴・傘の中心）だけで判定すると、そこが画面外に出たとたんに巨体全体が消えてしまうため。
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
        // 土台の動き（48 秒ループ）：全身の上下・胴の呼吸・触腕 1 本ごとの波・発光器官の明滅・星屑。静止時も移動時も常に流す。
        // 移動に合わせた動き（触腕が後ろへ流れる・波が大きくなる・渦がほどける）は、レンダラーが速さに応じて上から足す
        // （AstraVeilAnimationController）。アニメーションを切り替えないので、静止 ⇔ 移動で姿勢が飛ばない。
        // 攻撃アニメを作るときは、ここへ 2 つ目のコントローラー（triggerAnim 用）を足す。
        controllers.add(new AnimationController<>("main", test -> test.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
