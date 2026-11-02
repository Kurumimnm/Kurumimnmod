package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
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

    /**
     * 生き物系エンティティ（LivingEntity）の属性を登録する。
     * 属性を登録しないとスポーン時にクラッシュするので、Mob を追加したら必ずここに追記する。
     */
    @SubscribeEvent
    static void onAttributeCreation(EntityAttributeCreationEvent event) {
        // 例) event.put(WALNUT_MOB.get(), WalnutMob.createAttributes().build());
    }
}
