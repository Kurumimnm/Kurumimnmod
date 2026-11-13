package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.AstraEntity;
import io.github.kurumimnm.kurumimnmod.entity.AstraVeilEntity;
import io.github.kurumimnm.kurumimnmod.entity.GarouEntity;
import io.github.kurumimnm.kurumimnmod.entity.GriffinEntity;
import io.github.kurumimnm.kurumimnmod.entity.KoboldEntity;
import io.github.kurumimnm.kurumimnmod.entity.KoboldLordEntity;
import io.github.kurumimnm.kurumimnmod.entity.MinionEntity;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.entity.IceIcicleEntity;
import io.github.kurumimnm.kurumimnmod.entity.SlimeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * エンティティ（Mob 等）の登録クラス。
 * <p>
 * 【GeckoLib エンティティの追加手順】
 * <ol>
 *   <li>エンティティクラスを作成し {@code GeoEntity} を実装する（entity パッケージ）
 *       <pre>{@code
 * public class WalnutMob extends PathfinderMob implements GeoEntity {
 *     private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
 *
 *     @Override
 *     public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
 *         controllers.add(new AnimationController<>("main", state ->
 *                 state.setAndContinue(RawAnimation.begin().thenLoop("animation.walnut_mob.idle"))));
 *     }
 *
 *     @Override
 *     public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
 * }
 *       }</pre>
 *   </li>
 *   <li>このクラスにエンティティタイプを登録する
 *       <pre>{@code
 * public static final DeferredHolder<EntityType<?>, EntityType<WalnutMob>> WALNUT_MOB =
 *         ENTITY_TYPES.registerEntityType("walnut_mob", WalnutMob::new, MobCategory.CREATURE,
 *                 builder -> builder.sized(0.6f, 0.6f));
 *       }</pre>
 *   </li>
 *   <li>下の {@link #onAttributeCreation} で体力・移動速度などの属性を登録する
 *       （数値は {@code ModConstants} に置く）</li>
 *   <li>{@code client/ModEntityRenderers} でレンダラーを登録する</li>
 *   <li>Blockbench の書き出しファイルを配置する（"walnut_mob" の場合）
 *       <ul>
 *         <li>モデル: assets/kurumimnmod/geckolib/models/entity/walnut_mob.geo.json</li>
 *         <li>アニメーション: assets/kurumimnmod/geckolib/animations/entity/walnut_mob.animation.json</li>
 *         <li>テクスチャ: assets/kurumimnmod/textures/entity/walnut_mob.png</li>
 *       </ul>
 *   </li>
 * </ol>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class ModEntities {

    private ModEntities() {}

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Kurumimnmod.MOD_ID);

    // =====================================================================
    // カカシ（ステータス実験用の的）
    // =====================================================================

    /** カカシ。召喚用ステッキで出す（スポーンエッグ・自然スポーンなし）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<ScarecrowEntity>> SCARECROW =
            ENTITY_TYPES.registerEntityType("scarecrow", ScarecrowEntity::new, MobCategory.MISC,
                    builder -> builder.sized(ModConstants.SCARECROW_WIDTH, ModConstants.SCARECROW_HEIGHT)
                            .clientTrackingRange(ModConstants.SCARECROW_TRACKING_RANGE)
                            .noLootTable());

    // =====================================================================
    // モブ
    // =====================================================================

    /**
     * グリフォン（中立モブ。地上 / 飛行を切り替える）。今はスポーンエッグ・/summon のみで自然スポーンなし。
     * ドロップなし（noLootTable）。ドロップを追加するときは noLootTable を外して
     * data/kurumimnmod/loot_table/entities/griffin.json を用意する。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<GriffinEntity>> GRIFFIN =
            ENTITY_TYPES.registerEntityType("griffin", GriffinEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(ModConstants.GRIFFIN_WIDTH, ModConstants.GRIFFIN_HEIGHT)
                            .eyeHeight(ModConstants.GRIFFIN_EYE_HEIGHT)
                            .clientTrackingRange(ModConstants.GRIFFIN_TRACKING_RANGE)
                            .noLootTable());

    /** ミニオン（小鬼。敵対・近接）。クエストのフィールド・スポーンエッグ・/summon で出る。ドロップなし。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MinionEntity>> MINION =
            ENTITY_TYPES.registerEntityType("minion", MinionEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(ModConstants.MINION_WIDTH, ModConstants.MINION_HEIGHT)
                            .eyeHeight(ModConstants.MINION_EYE_HEIGHT)
                            .clientTrackingRange(ModConstants.MINION_TRACKING_RANGE)
                            .noLootTable());

    /** スライム（MOD 独自。分裂しない・跳ねて体当たり）。クエストのフィールド・スポーンエッグ・/summon で出る。ドロップなし。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SlimeEntity>> SLIME =
            ENTITY_TYPES.registerEntityType("slime", SlimeEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(ModConstants.SLIME_WIDTH, ModConstants.SLIME_HEIGHT)
                            .eyeHeight(ModConstants.SLIME_EYE_HEIGHT)
                            .clientTrackingRange(ModConstants.SLIME_TRACKING_RANGE)
                            .noLootTable());

    /** ガロウ（4 つ足の狼。敵対。スキル：2 回突進して噛みつく）。ドロップなし。 */
    public static final DeferredHolder<EntityType<?>, EntityType<GarouEntity>> GAROU =
            ENTITY_TYPES.registerEntityType("garou", GarouEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(ModConstants.GAROU_WIDTH, ModConstants.GAROU_HEIGHT)
                            .eyeHeight(ModConstants.GAROU_EYE_HEIGHT)
                            .clientTrackingRange(ModConstants.GAROU_TRACKING_RANGE)
                            .noLootTable());

    /** コボルト（犬頭の小柄な人型。敵対。スキル：飛びつき）。ドロップなし。 */
    public static final DeferredHolder<EntityType<?>, EntityType<KoboldEntity>> KOBOLD =
            ENTITY_TYPES.registerEntityType("kobold", KoboldEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(ModConstants.KOBOLD_WIDTH, ModConstants.KOBOLD_HEIGHT)
                            .eyeHeight(ModConstants.KOBOLD_EYE_HEIGHT)
                            .clientTrackingRange(ModConstants.KOBOLD_TRACKING_RANGE)
                            .noLootTable());

    // =====================================================================
    // ボス（タグ kurumimnmod:bosses に入れる）
    // =====================================================================

    /** コボルトロード（チュートリアルのボス。壁蹴り突撃・HP 3 割で二刀流）。ドロップなし。 */
    public static final DeferredHolder<EntityType<?>, EntityType<KoboldLordEntity>> KOBOLD_LORD =
            ENTITY_TYPES.registerEntityType("kobold_lord", KoboldLordEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(ModConstants.KOBOLD_LORD_WIDTH, ModConstants.KOBOLD_LORD_HEIGHT)
                            .eyeHeight(ModConstants.KOBOLD_LORD_EYE_HEIGHT)
                            .clientTrackingRange(ModConstants.KOBOLD_LORD_TRACKING_RANGE)
                            .noLootTable());

    /**
     * アストラ・ヴェイル（星幽の巡界者。フィールドにランダムエンカウントする超大型ボス）。
     * 今は見た目のみ（スポーンエッグ・/summon で出す）。当たり判定は顔のまわりだけで、見た目はレンダラーで拡大する（ASTRA_VEIL_RENDER_SCALE）。
     * 炎・溶岩で燃えない。ドロップなし（撃破報酬は後で決める）。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<AstraVeilEntity>> ASTRA_VEIL =
            ENTITY_TYPES.registerEntityType("astra_veil", AstraVeilEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(ModConstants.ASTRA_VEIL_WIDTH, ModConstants.ASTRA_VEIL_HEIGHT)
                            .eyeHeight(ModConstants.ASTRA_VEIL_EYE_HEIGHT)
                            .clientTrackingRange(ModConstants.ASTRA_VEIL_TRACKING_RANGE)
                            .fireImmune()
                            .noLootTable());

    /**
     * アストラ（星幽の巡界者。アストラ・ヴェイルとは別モデルの超大型ボス。膜の幅 約 520 ブロック）。
     * 今は見た目のみ（スポーンエッグ・/summon で出す）。当たり判定は頭の付け根と胴の上部だけ（仮）で、見た目はレンダラーで拡大する（ASTRA_RENDER_SCALE）。
     * 炎・溶岩で燃えない。ドロップなし（撃破報酬は後で決める）。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<AstraEntity>> ASTRA =
            ENTITY_TYPES.registerEntityType("astra", AstraEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(ModConstants.ASTRA_WIDTH, ModConstants.ASTRA_HEIGHT)
                            .eyeHeight(ModConstants.ASTRA_EYE_HEIGHT)
                            .clientTrackingRange(ModConstants.ASTRA_TRACKING_RANGE)
                            .fireImmune()
                            .noLootTable());

    // =====================================================================
    // 魔法の弾
    // =====================================================================

    /** 氷柱の杖の氷柱。位置のずれを防ぐため毎 tick 同期する（寿命が短いので負荷は小さい）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<IceIcicleEntity>> ICE_ICICLE =
            ENTITY_TYPES.registerEntityType("ice_icicle", IceIcicleEntity::new, MobCategory.MISC,
                    builder -> builder.sized(ModConstants.ICE_ICICLE_SIZE, ModConstants.ICE_ICICLE_SIZE)
                            .clientTrackingRange(ModConstants.ICE_ICICLE_TRACKING_RANGE)
                            .updateInterval(1));

    /**
     * 生き物系エンティティ（LivingEntity）の属性を登録する。
     * 属性を登録しないとスポーン時にクラッシュするので、Mob を追加したら必ずここに追記する。
     */
    @SubscribeEvent
    static void onAttributeCreation(EntityAttributeCreationEvent event) {
        // 例) event.put(WALNUT_MOB.get(), WalnutMob.createAttributes().build());
        event.put(SCARECROW.get(), ScarecrowEntity.createAttributes().build());
        event.put(GRIFFIN.get(), GriffinEntity.createAttributes().build());
        event.put(MINION.get(), MinionEntity.createAttributes().build());
        event.put(SLIME.get(), SlimeEntity.createAttributes().build());
        event.put(GAROU.get(), GarouEntity.createAttributes().build());
        event.put(KOBOLD.get(), KoboldEntity.createAttributes().build());
        event.put(KOBOLD_LORD.get(), KoboldLordEntity.createAttributes().build());
        event.put(ASTRA_VEIL.get(), AstraVeilEntity.createAttributes().build());
        event.put(ASTRA.get(), AstraEntity.createAttributes().build());
    }
}
