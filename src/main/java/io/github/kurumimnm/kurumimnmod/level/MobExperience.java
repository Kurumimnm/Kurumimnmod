package io.github.kurumimnm.kurumimnmod.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jspecify.annotations.Nullable;

/**
 * Mob を倒したときにもらえる経験値（データマップ）。
 * <p>
 * 経験値はコードに直書きせず、データパックの JSON で Mob ごとに設定する。
 * 設定されていない Mob（バニラの Mob を含む）は経験値 0。
 * <pre>
 * data/kurumimnmod/data_maps/entity_type/experience.json
 * {
 *   "values": {
 *     "kurumimnmod:walnut_mob": { "experience": 100 },
 *     "kurumimnmod:walnut_king": { "experience": 5000, "boss": true },
 *     "minecraft:zombie":       { "experience": 20 }
 *   }
 * }
 * </pre>
 * MOD の Mob を追加したら、datagen（DataMapProvider）でこの JSON を生成する予定。
 * サーバー管理者もデータパックで上書き・追加できる。
 *
 * @param experience キルしたプレイヤーがもらえる基本経験値
 * @param boss       ボス扱いにするか（省略時 false）。
 *                   ボスはアシストも bossAssistExpRatio 倍（既定 100%）で、
 *                   倒されるまでに 1 度でも関わればアシストになる（60 秒の時間制限なし）
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public record MobExperience(int experience, boolean boss) {

    /** 経験値が設定されていない Mob。 */
    public static final MobExperience NONE = new MobExperience(0, false);

    public static final Codec<MobExperience> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("experience").forGetter(MobExperience::experience),
            Codec.BOOL.optionalFieldOf("boss", false).forGetter(MobExperience::boss)
    ).apply(instance, MobExperience::new));

    /** データマップの種類（ID: kurumimnmod:experience）。 */
    public static final DataMapType<EntityType<?>, MobExperience> DATA_MAP = DataMapType.builder(
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "experience"), Registries.ENTITY_TYPE, CODEC).build();

    /** エンティティの種類から経験値の設定を取得する（未設定なら {@link #NONE}）。 */
    public static MobExperience of(EntityType<?> type) {
        @Nullable MobExperience data = type.builtInRegistryHolder().getData(DATA_MAP);
        return data == null ? NONE : data;
    }

    @SubscribeEvent
    static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }
}
