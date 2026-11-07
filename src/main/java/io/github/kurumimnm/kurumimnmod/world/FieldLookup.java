package io.github.kurumimnm.kurumimnmod.world;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import org.jspecify.annotations.Nullable;

/**
 * 「今いる場所はどのフィールドの何番エリアか」を調べるための入口。
 * <p>
 * 今後、モブのレベル決め（危険度）・エリア名の表示・ゲート判定などはここを使う。
 * このMODのワールド（ワールドの種類「Kurumimnmod」）以外では空を返す。
 */
public final class FieldLookup {

    private FieldLookup() {}

    /** そのディメンションが使っている地図。フィールドマップのワールドでなければ空。 */
    public static Optional<FieldMap> map(ServerLevel level) {
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        if (source instanceof FieldMapBiomeSource fieldSource) {
            return Optional.of(fieldSource.map().value());
        }
        return Optional.empty();
    }

    // ---- サーバー・クライアントどちらからでも引ける入口（当たり判定など、両側で動く処理用） ----

    /** クライアント側の地図の引き方（クライアントの初期化で差し込む。サーバー専用環境では null のまま）。 */
    private static volatile @Nullable Function<Level, FieldMap> clientResolver;

    public static void setClientResolver(Function<Level, FieldMap> resolver) {
        clientResolver = resolver;
    }

    /** そのワールドの地図（サーバー・クライアント共通）。フィールドマップのワールドでなければ null。 */
    public static @Nullable FieldMap mapOrNull(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return map(serverLevel).orElse(null);
        }
        Function<Level, FieldMap> resolver = clientResolver;
        return resolver == null ? null : resolver.apply(level);
    }

    /** そのディメンションが使っている地図の名前（例：kurumimnmod:astralia）。 */
    public static Optional<net.minecraft.resources.Identifier> mapId(ServerLevel level) {
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        if (source instanceof FieldMapBiomeSource fieldSource) {
            return fieldSource.map().unwrapKey().map(key -> key.identifier());
        }
        return Optional.empty();
    }

    /** ブロック座標 (x, z) のフィールド・エリア・危険度。 */
    public static Optional<FieldMap.Location> locate(ServerLevel level, double x, double z) {
        return map(level).map(m -> m.locate(x, z));
    }
}
