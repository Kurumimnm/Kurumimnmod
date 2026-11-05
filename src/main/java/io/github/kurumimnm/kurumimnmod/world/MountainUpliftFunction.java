package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * 地形の JSON（density_function）から使う「山脈で地形を盛り上げる量」。
 * <p>
 * {@code kurumimnmod:field_world/offset}（地面の高さの基準）に足し合わせて使う。
 * 地面の高さそのものを持ち上げるので、洞窟・鉱石・地表のブロックなどはバニラの仕組みがそのまま働く
 * （山脈の中にも洞窟ができる）。
 * <pre>{@code
 * { "type": "kurumimnmod:mountain_uplift", "map": "kurumimnmod:first_continent" }
 * }</pre>
 * 値は X・Z だけで決まる（高さ Y には関係しない）。
 */
public record MountainUpliftFunction(Holder<FieldMap> map) implements DensityFunction {

    public static final MapCodec<MountainUpliftFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FieldMap.CODEC.fieldOf("map").forGetter(MountainUpliftFunction::map)
    ).apply(i, MountainUpliftFunction::new));

    @Override
    public DensitySampler compileSampler(DensityFunction.CompileContext context) {
        return new Sampler(map.value());
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        return this;
    }

    @Override
    public Interval range() {
        return Interval.of(0.0F, (float) map.value().maxMountainUplift());
    }

    @Override
    public int domainAxes() {
        return DensityFunction.AXIS_X | DensityFunction.AXIS_Z;
    }

    @Override
    public MapCodec<MountainUpliftFunction> codec() {
        return CODEC;
    }

    private record Sampler(FieldMap map) implements DensitySampler {

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            // 高さ方向には同じ値なので、柱ごとに 1 回だけ計算する（並び順はバニラと同じ z → x → y）
            int index = 0;
            for (int z = 0; z < volume.sizeZ(); ++z) {
                int blockZ = volume.blockZ(z);
                for (int x = 0; x < volume.sizeX(); ++x) {
                    float value = (float) map.mountainUplift(volume.blockX(x), blockZ);
                    for (int y = 0; y < volume.sizeY(); ++y) {
                        outputBuffer.set(index++, value);
                    }
                }
            }
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return (float) map.mountainUplift(blockX, blockZ);
        }
    }
}
