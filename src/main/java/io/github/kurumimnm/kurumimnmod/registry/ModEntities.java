package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.IceIcicleEntity;
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
    }
}
