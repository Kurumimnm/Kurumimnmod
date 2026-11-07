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
 * 地形の JSON（density_function）から使う「地面の高さ（Y）」。{@link FieldMap#terrainHeight} を返す。
 * <p>
 * 地形の密度は「地面の高さ − Y」で作る（kurumimnmod:field_world/final_density）ので、
 * この値より下が地面、上が空になる。バニラの地形・洞窟・川は使っていない。
 * <pre>{@code
 * { "type": "kurumimnmod:terrain_height", "map": "kurumimnmod:astralia" }
 * }</pre>
 * 値は X・Z だけで決まる（高さ Y には関係しない）。
 */
public record TerrainHeightFunction(Holder<FieldMap> map) implements DensityFunction {

    public static final MapCodec<TerrainHeightFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FieldMap.CODEC.fieldOf("map").forGetter(TerrainHeightFunction::map)
    ).apply(i, TerrainHeightFunction::new));

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
        return Interval.of(-64.0F, (float) map.value().terrain().maxHeight());
    }

    @Override
    public int domainAxes() {
        return DensityFunction.AXIS_X | DensityFunction.AXIS_Z;
    }

    @Override
    public MapCodec<TerrainHeightFunction> codec() {
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
                    float value = (float) map.terrainHeight(volume.blockX(x), blockZ);
                    for (int y = 0; y < volume.sizeY(); ++y) {
                        outputBuffer.set(index++, value);
                    }
                }
            }
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return (float) map.terrainHeight(blockX, blockZ);
        }
    }
}
